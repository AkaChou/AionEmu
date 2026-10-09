package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.model.DialogPage;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.services.DialogService;
import com.aionemu.gameserver.services.StigmaService;
import com.aionemu.gameserver.utils.PacketSendUtility;

import java.util.Objects;
import java.util.function.IntPredicate;
import java.util.function.ObjIntConsumer;

/** 真实 {@link QuestDialogPort}：提交后关闭玩家的任务对话窗口。 / Real {@link QuestDialogPort}: closes the player's quest dialog window after commit. */
public final class PlayerQuestDialogPort implements QuestDialogPort {
	private final QuestPlayerPort players;
	private final ObjIntConsumer<Player> stigmaSlotRefresh;
	private final IntPredicate declaresStigmaSlotExtension;

	public PlayerQuestDialogPort(QuestPlayerPort players) {
		this(players, StigmaService::reannounceStigmaSlots,
			PlayerQuestDialogPort::declaresStigmaSlotExtensionBestEffort);
	}

	/**
	 * 任务引擎未由 Spring 提供时（单元测试、尚未启动的进程）按「未声明」处理：
	 * 槽位再通告是派生状态的 best-effort 推送，绝不能因引擎缺席而影响对话交付。
	 * Treated as "not declared" when no quest engine is Spring-provided (unit tests, processes that have
	 * not booted yet): the slot re-announce is a best-effort derived-state push and must never let a
	 * missing engine break dialog delivery.
	 * @param questId 任务 ID / quest id
	 * @return 声明扩展时为 true / true when the extension is declared
	 */
	private static boolean declaresStigmaSlotExtensionBestEffort(int questId) {
		try {
			return StigmaService.declaresStigmaSlotExtension(questId);
		} catch (IllegalStateException engineNotProvided) {
			return false;
		}
	}

	PlayerQuestDialogPort(QuestPlayerPort players, ObjIntConsumer<Player> stigmaSlotRefresh,
			IntPredicate declaresStigmaSlotExtension) {
		this.players = Objects.requireNonNull(players, "players");
		this.stigmaSlotRefresh = Objects.requireNonNull(stigmaSlotRefresh, "stigmaSlotRefresh");
		this.declaresStigmaSlotExtension = Objects.requireNonNull(declaresStigmaSlotExtension,
			"declaresStigmaSlotExtension");
	}

	/**
	 * 对话页下发前的烙印槽位再通告：本页会把客户端带进烙印窗口（页 1），或该任务数据声明「扩展烙印槽」时，
	 * 把玩家的槽位数再发一次（页 1 由服务侧附带原版「任务开启凹槽」通知）。
	 * 客户端缓存的槽位数会被服务端关窗重置，且重置晚于同一批包生效，
	 * 所以教学链里之后的每一次交互都必须重新确立这条协议事实，窗口内的凹槽才渲染得出来。
	 * Slot re-announce before a dialog page goes out: when the page leads the client into the stigma window
	 * (page 1), or the quest data declares the stigma-slot extension, the player's slot count is sent
	 * again (page 1 also carries the retail "slot opened by quest" notice). Server-side dialog closes reset
	 * the client-cached count after the batch that carried them, so every later interaction of the tutorial
	 * chain has to re-establish that protocol fact before the window can render its sockets.
	 * @param player 玩家 / player
	 * @param dialogId 目标对话页 ID / target dialog page id
	 * @param questId 任务 ID / quest id
	 */
	private void announceStigmaSlots(Player player, int dialogId, int questId) {
		if (dialogId == DialogPage.STIGMA.id() || declaresStigmaSlotExtension.test(questId)) {
			stigmaSlotRefresh.accept(player, dialogId);
		}
	}

	/**
	 * 解析目标对话对象 ID：目标缺失（客户端某些按钮不带对象 ID）且目标页是烙印窗口时，
	 * 用玩家正在进行的任务对话授权作为对话对象——授权由任务列表点击时登记（同一 NPC + 同一任务），
	 * 是服务端自己的权威记录，不是猜测（原版开烙印窗前同样先登记对话对象）。
	 * Resolves the dialog target object id: when the target is missing (the client sends no object id for
	 * some buttons) and the page is the stigma window, the player's ongoing quest-dialog authorization is
	 * used — it was registered on the quest-list click (same NPC, same quest), i.e. the server's own
	 * authoritative record rather than a guess (retail registers the dialog peer before opening the stigma
	 * window too).
	 * @param player 玩家 / player
	 * @param snapshot 提交快照 / commit snapshot
	 * @param dialogId 目标对话页 ID / target dialog page id
	 * @return 目标对象 ID（解析不到为 0）/ the target object id, or 0
	 */
	private static int resolveObjectId(Player player, QuestSnapshot snapshot, int dialogId) {
		if (!snapshot.targetlessDialog()) {
			return snapshot.interactionObjectId();
		}
		return dialogId == DialogPage.STIGMA.id()
			? player.getNpcQuestDialogObjectIdForQuest(snapshot.questId()) : 0;
	}

	@Override
	public boolean closeDialog(QuestSnapshot snapshot, QuestMutationPlan plan) {
		Objects.requireNonNull(snapshot, "snapshot");
		Objects.requireNonNull(plan, "plan");
		Player player = players.find(snapshot.playerId());
		if (player == null) {
			// 提交已成功但玩家已登出：无可发送对象，best-effort 关闭。 / Commit succeeded but player logged out: nothing to send to, best-effort close.
			return false;
		}
		player.clearNpcQuestDialogSelection();
		// 收尾同客户端关窗链：结束 NPC 对话态（DIALOG_FINISH），否则行进中的 NPC 停在半路。
		// Same tail as the client close: end the NPC talk state so a walking NPC resumes.
		DialogService.closeDialog(player, snapshot.targetlessDialog() ? 0 : snapshot.interactionObjectId());
		return true;
	}

	@Override
	public boolean showDialog(QuestSnapshot snapshot, QuestMutationPlan plan, int dialogId) {
		Objects.requireNonNull(snapshot, "snapshot");
		Objects.requireNonNull(plan, "plan");
		if (dialogId < 0) {
			throw new IllegalArgumentException("dialogId must be non-negative");
		}
		Player player = players.find(snapshot.playerId());
		if (player == null) {
			// 提交已成功但玩家已登出：无可发送对象，best-effort 跳过。 / Commit succeeded but player logged out: nothing to send to, best-effort skip.
			return false;
		}
		int objectId = resolveObjectId(player, snapshot, dialogId);
		if (objectId == 0 && !snapshot.targetlessDialog()) {
			// 缺少权威交互 objectId 时必须 fail closed，禁止用 NPC templateId 或玩家 target 猜测。
			// Missing an authoritative interaction objectId must fail closed; never guess
			// from the NPC templateId or the player's current target.
			throw new IllegalStateException("showDialog requires an authoritative interaction objectId "
				+ "from the execution context for quest " + snapshot.questId());
		}
		announceStigmaSlots(player, dialogId, snapshot.questId());
		PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(objectId, dialogId, snapshot.questId()));
		return true;
	}

	@Override
	public boolean showSelectionDialog(QuestSnapshot snapshot, QuestMutationPlan plan, int dialogId) {
		Objects.requireNonNull(snapshot, "snapshot");
		Objects.requireNonNull(plan, "plan");
		if (dialogId <= 0) {
			throw new IllegalArgumentException("dialogId must be positive");
		}
		Player player = players.find(snapshot.playerId());
		if (player == null) {
			return false;
		}
		int objectId = resolveObjectId(player, snapshot, dialogId);
		player.clearNpcQuestDialogSelection();
		if (objectId == 0 && !snapshot.targetlessDialog()) {
			throw new IllegalStateException("showSelectionDialog requires an authoritative interaction objectId "
				+ "from the execution context for quest " + snapshot.questId());
		}
		announceStigmaSlots(player, dialogId, snapshot.questId());
		PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(objectId, dialogId));
		return true;
	}

	@Override
	public boolean showDialogWindow(QuestSnapshot snapshot, QuestMutationPlan plan, int dialogId) {
		Objects.requireNonNull(snapshot, "snapshot");
		Objects.requireNonNull(plan, "plan");
		if (dialogId < 0) {
			throw new IllegalArgumentException("dialogId must be non-negative");
		}
		Player player = players.find(snapshot.playerId());
		if (player == null) {
			return false;
		}
		int objectId = resolveObjectId(player, snapshot, dialogId);
		if (objectId == 0 && !snapshot.targetlessDialog()) {
			throw new IllegalStateException("showDialogWindow requires an authoritative interaction objectId "
				+ "from the execution context for quest " + snapshot.questId());
		}
		announceStigmaSlots(player, dialogId, snapshot.questId());
		PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(objectId, dialogId));
		return true;
	}
}
