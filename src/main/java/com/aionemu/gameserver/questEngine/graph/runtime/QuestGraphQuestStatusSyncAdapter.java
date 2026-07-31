package com.aionemu.gameserver.questEngine.graph.runtime;

import java.util.Objects;
import java.util.function.Function;

import com.aionemu.gameserver.lifecycle.GameEngineServices;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_QUEST_ACTION;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.QuestStatus;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.SyncQuestStatusAction;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ActionInvocation;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ActionResult;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.QuestStatusSyncSnapshot;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.utils.PacketSendUtility;

/** Projects a persisted direct Handler update snapshot without reading the live QuestState. Lifecycle services own their own projection. */
public final class QuestGraphQuestStatusSyncAdapter {

	private final int playerId;
	private final Function<QuestStatusSyncCommand, ActionResult> endpoint;

	/** Creates an online-player adapter aligned only with QuestHandler.sendUpdatePacket. */
	public QuestGraphQuestStatusSyncAdapter(Player player) {
		this(requirePlayer(player).getObjectId(), command -> send(player, command));
	}

	/** Creates a focused-test adapter with an injectable projection endpoint. */
	QuestGraphQuestStatusSyncAdapter(int playerId, Function<QuestStatusSyncCommand, ActionResult> endpoint) {
		if (playerId <= 0) {
			throw new IllegalArgumentException("Quest-status sync adapter player id is invalid");
		}
		this.playerId = playerId;
		this.endpoint = Objects.requireNonNull(endpoint, "endpoint");
	}

	/** Executes exactly the frozen occurrence carried by the action invocation. */
	public ActionResult execute(ActionInvocation invocation) {
		QuestStatusSyncCommand command;
		try {
			if (invocation == null || invocation.event().playerId() != playerId
					|| !(invocation.action() instanceof SyncQuestStatusAction action)) {
				return ActionResult.FAILED;
			}
			QuestStatusSyncSnapshot snapshot = invocation.questStatusSyncSnapshot();
			if (snapshot == null || snapshot.actionIndex() != invocation.actionIndex()
					|| action.snapshotAfterActionCount() >= 0
						&& action.snapshotAfterActionCount() != snapshot.snapshotAfterActionCount()) {
				return ActionResult.FAILED;
			}
			command = new QuestStatusSyncCommand(invocation.questId(), playerId, snapshot.status(), snapshot.packedQuestVars(),
				invocation.idempotencyKey());
		} catch (RuntimeException e) {
			return ActionResult.FAILED;
		}
		try {
			return Objects.requireNonNull(endpoint.apply(command), "quest-status sync endpoint result");
		} catch (RuntimeException e) {
			return ActionResult.FAILED;
		}
	}

	private static ActionResult send(Player player, QuestStatusSyncCommand command) {
		if (player.getObjectId() != command.playerId()) {
			return ActionResult.FAILED;
		}
		PacketSendUtility.sendPacket(player, packet(command));
		if (command.status() == QuestStatus.REWARD || command.status() == QuestStatus.COMPLETE) {
			QuestEnv env = new QuestEnv(null, player, command.questId(), 0);
			GameEngineServices.questEngine().onLvlUp(env);
			player.getController().updateZone();
			player.getController().updateNearbyQuests();
		}
		return ActionResult.APPLIED;
	}

	/** Builds the exact packet shape used by QuestHandler.sendUpdatePacket. */
	static SM_QUEST_ACTION packet(QuestStatusSyncCommand command) {
		Objects.requireNonNull(command, "command");
		return new SM_QUEST_ACTION(command.questId(), modelStatus(command.status()), command.packedQuestVars());
	}

	private static com.aionemu.gameserver.questEngine.model.QuestStatus modelStatus(QuestStatus status) {
		return com.aionemu.gameserver.questEngine.model.QuestStatus.valueOf(status.name());
	}

	private static Player requirePlayer(Player player) {
		return Objects.requireNonNull(player, "player");
	}

	/** Immutable protocol command sourced only from the persisted snapshot. */
	public record QuestStatusSyncCommand(int questId, int playerId, QuestStatus status, int packedQuestVars, String idempotencyKey) {
		public QuestStatusSyncCommand {
			if (questId <= 0 || playerId <= 0 || status == null || idempotencyKey == null || idempotencyKey.isBlank()) {
				throw new IllegalArgumentException("Quest-status sync command is invalid");
			}
		}
	}
}
