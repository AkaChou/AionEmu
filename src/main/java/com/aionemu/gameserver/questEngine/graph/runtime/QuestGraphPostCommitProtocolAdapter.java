package com.aionemu.gameserver.questEngine.graph.runtime;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import com.aionemu.gameserver.model.ChatType;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.network.aion.serverpackets.SM_MESSAGE;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.CloseDialogAction;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.NoRepeatDeadlinePolicy;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.PlayerMessageChannel;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.RepeatDeadlinePolicy;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.SendPlayerMessageAction;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.SendRepeatDeadlineMessageAction;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.ShowQuestListAction;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ActionInvocation;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ActionResult;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.RepeatDeadlineResolution;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphDialogProtocolAdapter.DialogCommandTargetKind;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 将全部剩余提交后协议动作接到显式 typed 端点。
 * Connects the remaining post-commit protocol actions to explicit typed endpoints.
 *
 * <p>关闭窗口、任务列表和亮黄居中消息沿用 QuestHandler/现有 Handler 的已证实包形状。
 * repeat-deadline 消息的具体客户端文案和协议由调用方注入，避免在没有真端证据时猜测。
 * Close/list/player-message use the packet shapes proven by QuestHandler and existing handlers.
 * The concrete repeat-deadline message protocol is injected by the caller so this layer never guesses it.</p>
 */
public final class QuestGraphPostCommitProtocolAdapter {

	private final int playerId;
	private final Function<DialogWindowCommand, ActionResult> dialogEndpoint;
	private final Function<DialogWindowCommand, ActionResult> dialogRetry;
	private final Function<PlayerMessageCommand, ActionResult> playerMessageEndpoint;
	private final Function<PlayerMessageCommand, ActionResult> playerMessageRetry;
	private final Function<RepeatDeadlineMessageCommand, ActionResult> repeatDeadlineEndpoint;
	private final Function<RepeatDeadlineMessageCommand, ActionResult> repeatDeadlineRetry;
	private final Set<String> acceptedKeys = new HashSet<>();

	/**
	 * 创建在线玩家 adapter；repeat-deadline 端点必须由有证据的正式协议实现提供。
	 * Creates an online-player adapter; the repeat-deadline endpoint must be supplied by a proven production protocol implementation.
	 */
	public QuestGraphPostCommitProtocolAdapter(Player player,
		Function<DialogWindowCommand, ActionResult> dialogRetry,
		Function<PlayerMessageCommand, ActionResult> playerMessageRetry,
		Function<RepeatDeadlineMessageCommand, ActionResult> repeatDeadlineEndpoint,
		Function<RepeatDeadlineMessageCommand, ActionResult> repeatDeadlineRetry) {
		this(requirePlayer(player).getObjectId(), command -> sendDialog(player, command), dialogRetry,
			command -> sendPlayerMessage(player, command), playerMessageRetry, repeatDeadlineEndpoint, repeatDeadlineRetry);
	}

	/** Creates a focused-test adapter with injectable direct and retry endpoints. */
	QuestGraphPostCommitProtocolAdapter(int playerId,
		Function<DialogWindowCommand, ActionResult> dialogEndpoint,
		Function<DialogWindowCommand, ActionResult> dialogRetry,
		Function<PlayerMessageCommand, ActionResult> playerMessageEndpoint,
		Function<PlayerMessageCommand, ActionResult> playerMessageRetry,
		Function<RepeatDeadlineMessageCommand, ActionResult> repeatDeadlineEndpoint,
		Function<RepeatDeadlineMessageCommand, ActionResult> repeatDeadlineRetry) {
		if (playerId <= 0) {
			throw new IllegalArgumentException("Post-commit protocol adapter player id is invalid");
		}
		this.playerId = playerId;
		this.dialogEndpoint = Objects.requireNonNull(dialogEndpoint, "dialog endpoint");
		this.dialogRetry = Objects.requireNonNull(dialogRetry, "dialog retry");
		this.playerMessageEndpoint = Objects.requireNonNull(playerMessageEndpoint, "player-message endpoint");
		this.playerMessageRetry = Objects.requireNonNull(playerMessageRetry, "player-message retry");
		this.repeatDeadlineEndpoint = Objects.requireNonNull(repeatDeadlineEndpoint, "repeat-deadline endpoint");
		this.repeatDeadlineRetry = Objects.requireNonNull(repeatDeadlineRetry, "repeat-deadline retry");
	}

	/** 执行四类协议动作；未知动作、错误 owner、非法冻结结果均失败关闭。 */
	public synchronized ActionResult execute(ActionInvocation invocation) {
		if (invocation == null || invocation.event().playerId() != playerId) {
			return ActionResult.FAILED;
		}
		if (invocation.action() instanceof CloseDialogAction) {
			return executeDialog(invocation, DialogWindowKind.CLOSE);
		}
		if (invocation.action() instanceof ShowQuestListAction) {
			return executeDialog(invocation, DialogWindowKind.QUEST_LIST);
		}
		if (invocation.action() instanceof SendPlayerMessageAction action) {
			if (action.channel() != PlayerMessageChannel.BRIGHT_YELLOW_CENTER) {
				return ActionResult.FAILED;
			}
			return executePlayerMessage(new PlayerMessageCommand(invocation.questId(), playerId, action.text(), action.channel(),
				invocation.idempotencyKey()));
		}
		if (invocation.action() instanceof SendRepeatDeadlineMessageAction action) {
			RepeatDeadlineResolution resolution = invocation.repeatDeadlineResolution();
			if (!validRepeatResolution(action.repeatDeadlinePolicy(), resolution)) {
				return ActionResult.FAILED;
			}
			return executeRepeatDeadline(new RepeatDeadlineMessageCommand(invocation.questId(), playerId,
				action.repeatDeadlinePolicy(), resolution, invocation.idempotencyKey()));
		}
		return ActionResult.FAILED;
	}

	/** 清理会话级幂等键。 / Clears session-local idempotency keys. */
	public synchronized void clear() {
		acceptedKeys.clear();
	}

	/** 返回已接受的协议键数量，仅用于审计和确定性测试。 */
	public synchronized int size() {
		return acceptedKeys.size();
	}

	private ActionResult executeDialog(ActionInvocation invocation, DialogWindowKind kind) {
		int objectId = objectId(invocation);
		DialogCommandTargetKind targetKind = targetKind(invocation, objectId);
		if (targetKind == null) {
			return ActionResult.FAILED;
		}
		return executeDialog(new DialogWindowCommand(invocation.questId(), playerId, objectId, kind, targetKind,
			invocation.idempotencyKey()));
	}

	private ActionResult executeDialog(DialogWindowCommand command) {
		return executeProtocol(command.idempotencyKey(), command, dialogEndpoint, dialogRetry);
	}

	private ActionResult executePlayerMessage(PlayerMessageCommand command) {
		return executeProtocol(command.idempotencyKey(), command, playerMessageEndpoint, playerMessageRetry);
	}

	private ActionResult executeRepeatDeadline(RepeatDeadlineMessageCommand command) {
		return executeProtocol(command.idempotencyKey(), command, repeatDeadlineEndpoint, repeatDeadlineRetry);
	}

	private <T> ActionResult executeProtocol(String key, T command, Function<T, ActionResult> endpoint,
		Function<T, ActionResult> retry) {
		if (acceptedKeys.contains(key)) {
			return ActionResult.ALREADY_APPLIED;
		}
		ActionResult direct;
		try {
			direct = Objects.requireNonNull(endpoint.apply(command), "post-commit endpoint result");
		} catch (RuntimeException e) {
			direct = ActionResult.FAILED;
		}
		if (accepted(direct)) {
			acceptedKeys.add(key);
			return direct;
		}
		try {
			ActionResult deferred = Objects.requireNonNull(retry.apply(command), "post-commit retry result");
			if (accepted(deferred)) {
				acceptedKeys.add(key);
				return deferred;
			}
		} catch (RuntimeException ignored) {
			// The caller must observe FAILED when no endpoint accepts ownership.
		}
		return ActionResult.FAILED;
	}

	private static boolean accepted(ActionResult result) {
		return result == ActionResult.APPLIED || result == ActionResult.ALREADY_APPLIED;
	}

	private static int objectId(ActionInvocation invocation) {
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

	private static DialogCommandTargetKind targetKind(ActionInvocation invocation, int objectId) {
		if (invocation.event() instanceof QuestGraphEvent.DialogEvent dialog) {
			if (dialog.npcId() == 0 && objectId == 0) {
				return DialogCommandTargetKind.NO_TARGET;
			}
			return dialog.npcId() > 0 && objectId > 0 ? DialogCommandTargetKind.NPC : null;
		}
		if (invocation.event() instanceof QuestGraphEvent.ItemUseEvent || invocation.event() instanceof QuestGraphEvent.ItemDialogEvent) {
			return objectId > 0 ? DialogCommandTargetKind.ITEM : null;
		}
		return objectId == 0 ? DialogCommandTargetKind.NO_TARGET : null;
	}

	private static ActionResult sendDialog(Player player, DialogWindowCommand command) {
		if (player.getObjectId() != command.playerId() || player.getClientConnection() == null) {
			return ActionResult.FAILED;
		}
		int dialogId = command.kind() == DialogWindowKind.CLOSE ? 0 : 10;
		PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(command.objectId(), dialogId));
		return ActionResult.APPLIED;
	}

	private static ActionResult sendPlayerMessage(Player player, PlayerMessageCommand command) {
		if (player.getObjectId() != command.playerId() || player.getClientConnection() == null) {
			return ActionResult.FAILED;
		}
		PacketSendUtility.broadcastPacket(player, new SM_MESSAGE(player, command.text(), ChatType.BRIGHT_YELLOW_CENTER), true);
		return ActionResult.APPLIED;
	}

	private static boolean validRepeatResolution(RepeatDeadlinePolicy policy, RepeatDeadlineResolution resolution) {
		if (policy == null || policy == NoRepeatDeadlinePolicy.INSTANCE || resolution == null) {
			return false;
		}
		return switch (resolution.disposition()) {
			case DEADLINE -> resolution.deadlineAt() != null;
			case PRIVILEGED_BYPASS -> policy.privilegeMode() == com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.RepeatPrivilegeMode.BYPASS_FOR_PRIVILEGED;
			case NOT_APPLICABLE -> false;
		};
	}

	private static Player requirePlayer(Player player) {
		return Objects.requireNonNull(player, "player");
	}

	/** 窗口协议的固定页面。 / Fixed dialog-window protocol pages. */
	public enum DialogWindowKind {
		CLOSE,
		QUEST_LIST
	}

	/** 关闭窗口/任务列表的 typed 命令。 / Typed close/list window command. */
	public record DialogWindowCommand(int questId, int playerId, int objectId, DialogWindowKind kind,
		DialogCommandTargetKind targetKind, String idempotencyKey) {
		public DialogWindowCommand {
			if (questId <= 0 || playerId <= 0 || objectId < 0 || kind == null || targetKind == null
				|| targetKind == DialogCommandTargetKind.NO_TARGET && objectId != 0
				|| targetKind != DialogCommandTargetKind.NO_TARGET && objectId == 0
				|| idempotencyKey == null || idempotencyKey.isBlank()) {
				throw new IllegalArgumentException("Dialog-window command is invalid");
			}
		}
	}

	/** 亮黄居中玩家消息命令。 / Bright-yellow-center player-message command. */
	public record PlayerMessageCommand(int questId, int playerId, String text, PlayerMessageChannel channel,
		String idempotencyKey) {
		public PlayerMessageCommand {
			if (questId <= 0 || playerId <= 0 || text == null || text.isBlank()
				|| channel != PlayerMessageChannel.BRIGHT_YELLOW_CENTER || idempotencyKey == null || idempotencyKey.isBlank()) {
				throw new IllegalArgumentException("Player-message command is invalid");
			}
		}
	}

	/** repeat deadline 消息命令；具体文案/协议由端点依据冻结 policy 和 resolution 处理。 */
	public record RepeatDeadlineMessageCommand(int questId, int playerId, RepeatDeadlinePolicy policy,
		RepeatDeadlineResolution resolution, String idempotencyKey) {
		public RepeatDeadlineMessageCommand {
			if (questId <= 0 || playerId <= 0 || policy == null || policy == NoRepeatDeadlinePolicy.INSTANCE
				|| resolution == null || idempotencyKey == null || idempotencyKey.isBlank()
				|| !validRepeatResolution(policy, resolution)) {
				throw new IllegalArgumentException("Repeat-deadline message command is invalid");
			}
		}
	}
}
