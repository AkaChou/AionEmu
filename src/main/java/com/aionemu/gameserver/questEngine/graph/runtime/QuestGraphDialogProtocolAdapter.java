package com.aionemu.gameserver.questEngine.graph.runtime;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import com.aionemu.gameserver.model.EmotionType;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.network.aion.serverpackets.SM_EMOTION;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.DialogBindingMode;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.DialogTargetKind;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.EmotionTarget;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.SendDialogAction;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.SendEmotionAction;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ActionInvocation;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ActionResult;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 将对话提交后协议接到 BOUND/UNBOUND 窗口与服务端权威表情投影。
 * Connects post-dialog protocol to BOUND/UNBOUND windows and server-authoritative emote projection.
 */
public final class QuestGraphDialogProtocolAdapter {

	private final int playerId;
	private final Function<DialogCommand, ActionResult> endpoint;
	private final Function<DialogCommand, ActionResult> retry;
	private final Function<EmotionCommand, ActionResult> emotionEndpoint;
	private final Function<EmotionCommand, ActionResult> emotionRetry;
	private final Set<String> acceptedKeys = new HashSet<>();

	/**
	 * 创建在线玩家 adapter；retry 端口必须持久化或可观测地接管失败投影。
	 * Creates an online-player adapter; retry ports must durably or observably accept failed projections.
	 */
	public QuestGraphDialogProtocolAdapter(Player player, Function<DialogCommand, ActionResult> retry,
			Function<EmotionCommand, ActionResult> emotionRetry) {
		this(requirePlayer(player).getObjectId(), command -> send(player, command), retry,
			command -> sendEmotion(player, command), emotionRetry);
	}

	/** 创建可注入端点的聚焦测试 adapter。 / Creates a focused-test adapter with an injectable endpoint. */
	QuestGraphDialogProtocolAdapter(int playerId, Function<DialogCommand, ActionResult> endpoint) {
		this(playerId, endpoint, command -> ActionResult.FAILED, command -> ActionResult.FAILED, command -> ActionResult.FAILED);
	}

	/** 创建可分别注入窗口与表情端点的聚焦测试 adapter。 / Creates focused-test dialog and emote endpoints. */
	QuestGraphDialogProtocolAdapter(int playerId, Function<DialogCommand, ActionResult> endpoint,
			Function<EmotionCommand, ActionResult> emotionEndpoint) {
		this(playerId, endpoint, command -> ActionResult.FAILED, emotionEndpoint, command -> ActionResult.FAILED);
	}

	/** 创建分别带直接发送和 retry 端口的聚焦测试 adapter。 / Creates focused-test direct and retry ports. */
	QuestGraphDialogProtocolAdapter(int playerId, Function<DialogCommand, ActionResult> endpoint,
			Function<DialogCommand, ActionResult> retry, Function<EmotionCommand, ActionResult> emotionEndpoint,
			Function<EmotionCommand, ActionResult> emotionRetry) {
		if (playerId <= 0) {
			throw new IllegalArgumentException("Dialog protocol adapter player id is invalid");
		}
		this.playerId = playerId;
		this.endpoint = Objects.requireNonNull(endpoint, "endpoint");
		this.retry = Objects.requireNonNull(retry, "retry");
		this.emotionEndpoint = Objects.requireNonNull(emotionEndpoint, "emotionEndpoint");
		this.emotionRetry = Objects.requireNonNull(emotionRetry, "emotionRetry");
	}

	/**
	 * 投影对话窗口；错误 owner/动作或非法绑定显式失败。
	 * Projects the dialog window and explicitly fails wrong owner/action or illegal binding.
	 */
	public synchronized ActionResult execute(ActionInvocation invocation) {
		if (invocation == null || invocation.event().playerId() != playerId) {
			return ActionResult.FAILED;
		}
		if (invocation.action() instanceof SendEmotionAction action) {
			return executeEmotion(invocation, action);
		}
		if (!(invocation.action() instanceof SendDialogAction action)) {
			return ActionResult.FAILED;
		}
		int objectId = resolveObjectId(invocation);
		DialogCommand command;
		try {
			command = new DialogCommand(invocation.questId(), playerId, objectId, action.dialogId(), action.binding(), targetKind(invocation),
				invocation.idempotencyKey());
		} catch (RuntimeException e) {
			return ActionResult.FAILED;
		}
		return executeProtocol(command.idempotencyKey(), command, endpoint, retry);
	}

	/** 把 DIALOG objectId 快照绑定到玩家或 NPC 表情目标。 / Binds the DIALOG object-id snapshot to a player or NPC emote actor. */
	private ActionResult executeEmotion(ActionInvocation invocation, SendEmotionAction action) {
		if (!(invocation.event() instanceof QuestGraphEvent.DialogEvent dialog) || dialog.npcObjectId() <= 0) {
			return ActionResult.FAILED;
		}
		EmotionCommand command;
		try {
			command = new EmotionCommand(invocation.questId(), playerId, dialog.npcId(), dialog.npcObjectId(), action.target(),
				action.emotion(), action.broadcast(), invocation.idempotencyKey());
		} catch (RuntimeException e) {
			return ActionResult.FAILED;
		}
		return executeProtocol(command.idempotencyKey(), command, emotionEndpoint, emotionRetry);
	}

	/** 清理玩家会话结束后的临时幂等集合。 / Clears temporary idempotency keys after the player session ends. */
	public synchronized void clear() {
		acceptedKeys.clear();
	}

	/** 返回已接受键数量，仅用于确定性测试与审计。 / Returns the accepted-key count only for deterministic tests and audit. */
	public synchronized int size() {
		return acceptedKeys.size();
	}

	/** 直接投影失败时，把同一个 typed command 交给显式 retry/outbox 端口。 / Hands the same typed command to retry/outbox after direct failure. */
	private <T> ActionResult executeProtocol(String idempotencyKey, T command, Function<T, ActionResult> direct,
			Function<T, ActionResult> deferred) {
		if (acceptedKeys.contains(idempotencyKey)) {
			return ActionResult.ALREADY_APPLIED;
		}
		ActionResult directResult;
		try {
			directResult = Objects.requireNonNull(direct.apply(command), "dialog protocol endpoint result");
		} catch (RuntimeException e) {
			directResult = ActionResult.FAILED;
		}
		if (accepted(directResult)) {
			acceptedKeys.add(idempotencyKey);
			return directResult;
		}
		try {
			ActionResult retryResult = Objects.requireNonNull(deferred.apply(command), "dialog protocol retry result");
			if (accepted(retryResult)) {
				acceptedKeys.add(idempotencyKey);
				return retryResult;
			}
		} catch (RuntimeException ignored) {
			// The executor must observe FAILED when retry/outbox did not accept ownership.
		}
		return ActionResult.FAILED;
	}

	private static boolean accepted(ActionResult result) {
		return result == ActionResult.APPLIED || result == ActionResult.ALREADY_APPLIED;
	}

	/** 从 NPC/物品对话事件解析服务端权威 objectId；其他事件为 0。 / Resolves the authoritative NPC/item object id; 0 for other events. */
	private static int resolveObjectId(ActionInvocation invocation) {
		if (invocation.event() instanceof QuestGraphEvent.DialogEvent dialog) {
			return dialog.npcObjectId();
		}
		if (invocation.event() instanceof QuestGraphEvent.ItemUseEvent itemUse) {
			return itemUse.itemObjectId();
		}
		if (invocation.event() instanceof QuestGraphEvent.ItemDialogEvent itemDialog) {
			return itemDialog.itemObjectId();
		}
		return 0;
	}

	/** 仅把显式零 NPC 身份映射为 NO_TARGET；缺失的普通 NPC/物品 objectId 保持其绑定类型并失败关闭。 */
	private static DialogCommandTargetKind targetKind(ActionInvocation invocation) {
		if (invocation.event() instanceof QuestGraphEvent.DialogEvent dialog) {
			if (dialog.npcId() == 0 && dialog.npcObjectId() == 0) {
				return invocation.transitionEvent() != null
					&& invocation.transitionEvent().dialogTargetKind() == DialogTargetKind.NO_TARGET
					? DialogCommandTargetKind.NO_TARGET : null;
			}
			return DialogCommandTargetKind.NPC;
		}
		if (invocation.event() instanceof QuestGraphEvent.ItemUseEvent || invocation.event() instanceof QuestGraphEvent.ItemDialogEvent) {
			return DialogCommandTargetKind.ITEM;
		}
		return null;
	}

	private static ActionResult send(Player player, DialogCommand command) {
		if (player.getObjectId() != command.playerId() || player.getClientConnection() == null) {
			return ActionResult.FAILED;
		}
		if (command.binding() == DialogBindingMode.UNBOUND) {
			PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(command.objectId(), command.dialogId()));
		} else {
			PacketSendUtility.sendPacket(player,
				new SM_DIALOG_WINDOW(command.objectId(), command.dialogId(), command.questId()));
		}
		return ActionResult.APPLIED;
	}

	/** 解析服务端权威 actor，并复用旧 QuestHandler.sendEmotion 的协议字段。 / Resolves the authoritative actor and preserves legacy emote fields. */
	private static ActionResult sendEmotion(Player player, EmotionCommand command) {
		if (player.getObjectId() != command.playerId() || player.getClientConnection() == null) {
			return ActionResult.FAILED;
		}
		Creature actor;
		int targetObjectId;
		if (command.target() == EmotionTarget.PLAYER) {
			actor = player;
			targetObjectId = command.npcObjectId();
		} else {
			if (!(player.getKnownList().getObject(command.npcObjectId()) instanceof Npc npc)
					|| npc.getNpcId() != command.npcId()) {
				return ActionResult.FAILED;
			}
			actor = npc;
			targetObjectId = player.getObjectId();
		}
		PacketSendUtility.broadcastPacket(player,
			new SM_EMOTION(actor, EmotionType.EMOTE, command.emotion().id(), targetObjectId), command.broadcast());
		return ActionResult.APPLIED;
	}

	private static Player requirePlayer(Player player) {
		return Objects.requireNonNull(player, "player");
	}

	/** 封闭窗口协议可绑定的服务端目标种类。 / Closes server-authoritative target kinds accepted by the dialog protocol. */
	public enum DialogCommandTargetKind {
		NPC,
		ITEM,
		NO_TARGET
	}

	/** 表示带强类型目标的 BOUND/UNBOUND 对话协议投影命令。 / Represents a BOUND/UNBOUND dialog command with a typed target. */
	public record DialogCommand(int questId, int playerId, int objectId, int dialogId, DialogBindingMode binding,
			DialogCommandTargetKind targetKind, String idempotencyKey) {
		/** 校验对话协议命令。 / Validates the dialog protocol command. */
		public DialogCommand {
			if (questId <= 0 || playerId <= 0 || objectId < 0 || dialogId <= 0 || binding == null || targetKind == null
					|| targetKind == DialogCommandTargetKind.NO_TARGET && objectId != 0
					|| targetKind != DialogCommandTargetKind.NO_TARGET && objectId == 0
					|| idempotencyKey == null || idempotencyKey.isBlank()) {
				throw new IllegalArgumentException("Dialog protocol command is invalid");
			}
		}
	}

	/** 表示已绑定 DIALOG 身份快照的表情协议命令。 / Represents an emote command bound to a DIALOG identity snapshot. */
	public record EmotionCommand(int questId, int playerId, int npcId, int npcObjectId, EmotionTarget target,
			com.aionemu.gameserver.model.EmotionId emotion, boolean broadcast, String idempotencyKey) {
		/** 校验 owner、NPC 双标识和封闭目标。 / Validates owner, both NPC identifiers, and the closed target. */
		public EmotionCommand {
			if (questId <= 0 || playerId <= 0 || npcId <= 0 || npcObjectId <= 0 || target == null || emotion == null
					|| emotion == com.aionemu.gameserver.model.EmotionId.NONE || idempotencyKey == null || idempotencyKey.isBlank()) {
				throw new IllegalArgumentException("Emotion protocol command is invalid");
			}
		}
	}
}
