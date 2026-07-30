package com.aionemu.gameserver.questEngine.graph.runtime;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.SendSystemMessageAction;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.SystemMessageKind;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ActionInvocation;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ActionResult;
import com.aionemu.gameserver.utils.PacketSendUtility;

/** Projects the closed set of quest system messages through an explicit delivery boundary. */
public final class QuestGraphSystemMessageAdapter {

	private final int playerId;
	private final Function<SystemMessageCommand, ActionResult> endpoint;
	private final Function<SystemMessageCommand, ActionResult> retry;
	private final Set<String> acceptedKeys = new HashSet<>();

	/** Creates an online-player adapter with an explicit retry/outbox port. */
	public QuestGraphSystemMessageAdapter(Player player, Function<SystemMessageCommand, ActionResult> retry) {
		this(requirePlayer(player).getObjectId(), command -> send(player, command), retry);
	}

	/** Creates a focused-test adapter without a successful retry port. */
	QuestGraphSystemMessageAdapter(int playerId, Function<SystemMessageCommand, ActionResult> endpoint) {
		this(playerId, endpoint, command -> ActionResult.FAILED);
	}

	/** Creates a focused-test adapter with injectable direct and retry ports. */
	QuestGraphSystemMessageAdapter(int playerId, Function<SystemMessageCommand, ActionResult> endpoint,
			Function<SystemMessageCommand, ActionResult> retry) {
		if (playerId <= 0) {
			throw new IllegalArgumentException("System-message adapter player id is invalid");
		}
		this.playerId = playerId;
		this.endpoint = Objects.requireNonNull(endpoint, "endpoint");
		this.retry = Objects.requireNonNull(retry, "retry");
	}

	/** Projects one typed message and fails closed for invalid ownership or delivery rejection. */
	public synchronized ActionResult execute(ActionInvocation invocation) {
		if (invocation == null || !(invocation.action() instanceof SendSystemMessageAction action)) {
			return ActionResult.FAILED;
		}
		return execute(invocation.questId(), invocation.event().playerId(), action.kind(), invocation.idempotencyKey());
	}

	/** Projects one typed message and fails closed for invalid ownership or delivery rejection. */
	public synchronized ActionResult execute(int questId, int eventPlayerId, SystemMessageKind kind, String idempotencyKey) {
		if (eventPlayerId != playerId) {
			return ActionResult.FAILED;
		}
		SystemMessageCommand command;
		try {
			command = new SystemMessageCommand(questId, playerId, kind, idempotencyKey);
		} catch (RuntimeException e) {
			return ActionResult.FAILED;
		}
		if (acceptedKeys.contains(command.idempotencyKey())) {
			return ActionResult.ALREADY_APPLIED;
		}
		ActionResult directResult;
		try {
			directResult = Objects.requireNonNull(endpoint.apply(command), "system-message endpoint result");
		} catch (RuntimeException e) {
			directResult = ActionResult.FAILED;
		}
		if (accepted(directResult)) {
			acceptedKeys.add(command.idempotencyKey());
			return directResult;
		}
		try {
			ActionResult retryResult = Objects.requireNonNull(retry.apply(command), "system-message retry result");
			if (accepted(retryResult)) {
				acceptedKeys.add(command.idempotencyKey());
				return retryResult;
			}
		} catch (RuntimeException ignored) {
			// The executor must observe FAILED unless the retry/outbox explicitly accepts ownership.
		}
		return ActionResult.FAILED;
	}

	/** Clears temporary idempotency claims when the player session ends. */
	public synchronized void clear() {
		acceptedKeys.clear();
	}

	/** Returns accepted-key count for deterministic tests and audits. */
	public synchronized int size() {
		return acceptedKeys.size();
	}

	private static boolean accepted(ActionResult result) {
		return result == ActionResult.APPLIED || result == ActionResult.ALREADY_APPLIED;
	}

	private static ActionResult send(Player player, SystemMessageCommand command) {
		if (player.getObjectId() != command.playerId() || player.getClientConnection() == null) {
			return ActionResult.FAILED;
		}
		PacketSendUtility.sendPacket(player, packet(command));
		return ActionResult.APPLIED;
	}

	/** Builds only the three packet shapes proven by the migrated handlers. */
	static SM_SYSTEM_MESSAGE packet(SystemMessageCommand command) {
		SystemMessagePacketShape shape = packetShape(command);
		return shape.hasExplicitSender() ? new SM_SYSTEM_MESSAGE(shape.npcShout(), shape.code(), shape.senderId(), shape.textColorId())
			: new SM_SYSTEM_MESSAGE(shape.code());
	}

	/** Returns the closed packet projection so tests can verify the protocol without private-field access. */
	static SystemMessagePacketShape packetShape(SystemMessageCommand command) {
		Objects.requireNonNull(command, "command");
		return switch (command.kind()) {
			case INSTANCE_DUNGEON_NEED_SOLO -> new SystemMessagePacketShape(1403080, 0, 26, false, false);
			case WAREHOUSE_FULL_INVENTORY -> new SystemMessagePacketShape(1390149, 0, 26, false, false);
			case COMMON_SAY_08 -> new SystemMessagePacketShape(1111307, command.playerId(), 2, false, true);
		};
	}

	private static Player requirePlayer(Player player) {
		return Objects.requireNonNull(player, "player");
	}

	/** Immutable delivery command bound to one quest owner and stable idempotency key. */
	public record SystemMessageCommand(int questId, int playerId, SystemMessageKind kind, String idempotencyKey) {
		public SystemMessageCommand {
			if (questId <= 0 || playerId <= 0 || kind == null || idempotencyKey == null || idempotencyKey.isBlank()) {
				throw new IllegalArgumentException("System-message command is invalid");
			}
		}
	}

	/** Exact constructor-level packet shape for the closed system-message set. */
	record SystemMessagePacketShape(int code, int senderId, int textColorId, boolean npcShout, boolean hasExplicitSender) {
		SystemMessagePacketShape {
			if (code <= 0 || senderId < 0 || textColorId < 0 || (!hasExplicitSender && (senderId != 0 || textColorId != 26 || npcShout))) {
				throw new IllegalArgumentException("System-message packet shape is invalid");
			}
		}
	}
}
