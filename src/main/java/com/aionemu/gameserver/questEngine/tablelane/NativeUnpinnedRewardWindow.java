package com.aionemu.gameserver.questEngine.tablelane;

import java.util.Objects;
import java.util.function.Supplier;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.questEngine.definition.QuestDialogAction;
import com.aionemu.gameserver.questEngine.definition.QuestDialogPage;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 未绑定交付对象的奖励窗确认动作恢复段（真端 {@code QuestDialog} 无主键协议；与 typed 车道
 * {@code QuestRuntimeDispatcher#dispatchRewardWindowAction} 的恢复同裁定）。
 * <p>
 * 奖励窗由全局 UI 打开，客户端可能携带上一个交互对象，因此服务端**不能要求交互对象等于完成路由的
 * 交付 NPC**。本段在 native 八族的严格绑定派发全部未命中后按 questId + action 结算：
 * <ol>
 *   <li>门 = 动作 ∈ {@link QuestDialogAction#isRewardWindowAction} + 状态 {@code REWARD}
 *       （结算体自身继续验真端元数据/档位/按钮声明，任何一门不过零副作用）；</li>
 *   <li>结算经共用领奖口 {@link NativeReportRewardFlow}（与各族交付面同一份发放与状态写入）；</li>
 *   <li>收尾 = 选择对话页（页 10，questId=0；与各族交付面领奖收尾同形）。</li>
 * </ol>
 * 缺口来源（2026-10-08 实机 19640，接取对象 799022 上点奖励窗 → 动作 id 被下游 AI 回显为页 8 →
 * 客户端 load fail）：传送门类 AI（{@code PortalDialogAI2}/{@code Specialize01PortalAI2}）的开门页轴
 * 只读 QuestEngine 的 NPC 对话注册表，对「该 NPC 相关且处于 REWARD」的任务一律开奖励窗页 5——
 * 「接取对象 ≠ 交付对象」的行（19640 = 接取 Elim_DF4_01 / 交付 Barus）会在接取对象上被开出奖励窗，
 * 而各族领奖腿只注册在交付对象上，动作就此落空。本段把这类动作按 questId 收进结算，
 * 不新增奖励窗开启入口（开启仍由原页轴决定）。
 * <p>
 * Recovery settlement for reward-window confirmation actions that carry an interaction object which is
 * not the completion route's delivery NPC (the retail ownerless {@code QuestDialog} protocol, same
 * adjudication as the typed lane's {@code dispatchRewardWindowAction}). Runs after every native family
 * has missed on strict NPC binding; gate = reward-window action band + {@code REWARD} state, and the
 * settlement itself keeps all retail metadata / tier / button-declaration gates. It never opens a
 * reward window — it only settles the button the client already shows.
 */
public final class NativeUnpinnedRewardWindow {

	/** 引擎调用面的领奖口来源（生产 = {@link NativeReportRewardFlow#instance()}）；包内测试接缝可替换。 /
	 * The settlement port supplier used by the engine entry point (production singleton); replaceable
	 * through the package-private test seam. */
	private static volatile Supplier<NativeReportRewardFlow> flowSupplier =
		NativeReportRewardFlow::instance;

	private NativeUnpinnedRewardWindow() {
	}

	/**
	 * 引擎调用面：按 questId 结算未绑定交付对象的奖励窗确认动作（领奖口 = 生产单例）。
	 * Engine entry point: settles an unpinned reward-window confirmation by quest id (production port).
	 * @param player   玩家 / the player
	 * @param questId  行 owner（调用方已按 native owner 判定）/ the owning row
	 * @param dialogId 对话动作 id / the dialog action id
	 * @param objectId 对话对象句柄（收尾页下发用）/ the dialog object handle for the tail page
	 * @return 是否已结算并发出收尾 / whether the claim settled and the tail was sent
	 */
	public static boolean claim(Player player, int questId, int dialogId, int objectId) {
		return claim(player, questId, dialogId, objectId, flowSupplier.get());
	}

	/**
	 * 替换引擎调用面的领奖口来源（包内测试接缝；调用方负责在 finally 还原）。
	 * Replaces the settlement port supplier of the engine entry point (package-private test seam;
	 * restore in the caller).
	 * @param supplier 领奖口来源 / the settlement port supplier
	 */
	static void setFlowSupplierForTest(Supplier<NativeReportRewardFlow> supplier) {
		flowSupplier = Objects.requireNonNull(supplier, "supplier");
	}

	/** 还原生产领奖口来源。 / Restores the production settlement port supplier. */
	static void resetFlowSupplierForTest() {
		flowSupplier = NativeReportRewardFlow::instance;
	}

	/**
	 * 按 questId 结算未绑定交付对象的奖励窗确认动作；收尾下发选择对话页（页 10，questId=0）。
	 * Settles a reward-window confirmation keyed by quest id when no native family claims it by npc
	 * binding; the tail sends the selection dialog page (page 10, no quest context).
	 * @param player     玩家 / the player
	 * @param questId    行 owner（调用方已按 native owner 判定）/ the owning row
	 * @param dialogId   对话动作 id / the dialog action id
	 * @param objectId   对话对象句柄（收尾页下发用）/ the dialog object handle for the tail page
	 * @param rewardFlow 领奖口（生产 = {@link NativeReportRewardFlow#instance()}）/ the settlement port
	 * @return 是否已结算并发出收尾 / whether the claim settled and the tail was sent
	 */
	public static boolean claim(Player player, int questId, int dialogId, int objectId,
			NativeReportRewardFlow rewardFlow) {
		if (player == null || player.getQuestStateList() == null || questId <= 0 || rewardFlow == null) {
			return false;
		}
		if (!QuestDialogAction.isRewardWindowAction(dialogId)) {
			return false;
		}
		QuestState state = player.getQuestStateList().getQuestState(questId);
		if (state == null || state.getStatus() != QuestStatus.REWARD) {
			return false;
		}
		int rewardIndex = dialogId >= QuestDialogAction.SELECTED_QUEST_REWARD1.id()
				&& dialogId <= QuestDialogAction.SELECTED_QUEST_REWARD15.id()
			? dialogId - QuestDialogAction.SELECTED_QUEST_REWARD1.id() : 0;
		if (!rewardFlow.claim(new QuestEnv(null, player, questId, dialogId), rewardIndex).completed()) {
			return false;
		}
		// 收尾 = 回选择对话页（与各族交付面领奖收尾同形：页 10、questId=0）。
		// Tail = the selection dialog page, matching every family's delivery-face claim tail.
		PacketSendUtility.sendPacket(player,
			new SM_DIALOG_WINDOW(objectId, QuestDialogPage.SELECT_QUEST.id()));
		return true;
	}
}
