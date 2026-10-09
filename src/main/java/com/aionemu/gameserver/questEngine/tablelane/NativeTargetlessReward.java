package com.aionemu.gameserver.questEngine.tablelane;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.definition.QuestDialogAction;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.DialogService;

/**
 * 无目标领奖结算段（原版 {@code QuestDialog} 无主键协议）：任务窗/实时奖励槽的确认包不带任何 NPC
 * 上下文（{@code CM_DIALOG_SELECT} 的 targetless 分支，或客户端携带的上一个交互对象已不可解析），
 * 服务端只凭 questId + 动作路由到拥有该行的 native 族并结算——{@link QuestDialogAction#isRewardWindowAction}
 * 的契约注释即此口径（「不能把该对象当作完成 NPC 绑定」）。
 * <p>
 * 合同来源（三源一致）：
 * <ol>
 *   <li>客户端权威：Aion 5.8 客户端任务书「在任务窗点击[领取奖励]% 或 和[奥尔佩]%对话」；第一个实时
 *       奖励槽发送 {@code SELECTED_QUEST_AUTO_REWARD1}(110)（普通槽为 {@code SELECTED_QUEST_REWARD1}(8)）；</li>
 *   <li>Playbook 案例 8.3 {@code TARGETLESS_REALTIME_REWARD_ACTION_SPACE}（commit {@code 4a23cf0a}，
 *       13830-13834 退役前的 XML 已实机验收）：无目标 8/110 → 发职业物品 + 经验、回收工作物品、完成，
 *       收尾 = <b>关窗</b>；</li>
 *   <li>typed 车道 targetless 等价物：{@code PlayerQuestDialogPort.closeDialog} 在无目标时以 objectId=0
 *       关窗（{@code SM_DIALOG_WINDOW(0,0)}）。</li>
 * </ol>
 * 退役迁移曾丢失该面（XML 合同随 13830-13834/23830-23834 删除，native 只按 npcId 路由），本段由七族
 * handler 与 {@link DataDrivenNativeRuntime} 在 {@code npcId == 0} 时共用。
 * <p>
 * The targetless reward settlement (the retail ownerless {@code QuestDialog} protocol): reward-window
 * confirmations sent from the quest journal carry no NPC context, so the server routes them by quest id
 * and action alone to the native family owning the row. The close-dialog tail matches the typed lane's
 * targetless {@code CloseDialog} and the accepted XML contract of Playbook case 8.3.
 */
public final class NativeTargetlessReward {

	private NativeTargetlessReward() {
	}

	/**
	 * 按 questId 结算无目标领奖：状态门 {@code REWARD} + 奖励窗确认段动作（与六族同口径，经
	 * {@link QuestDialogAction#isRewardWindowAction}）→ 共用结算体 → 关窗收尾。
	 * 任何一门不满足即返回 false（零副作用），调用方继续既有分发。
	 * <p>
	 * Settles the targetless claim gated on {@code REWARD} and the shared reward-window confirm band;
	 * the tail closes the window. Any failed gate returns false with zero side effects.
	 * @param player     玩家 / the player
	 * @param questId    行 owner（调用方已按 {@code routes(questId)} 判定）/ the owning row
	 * @param dialogId   对话动作 id / the dialog action id
	 * @param rewardFlow 领奖口（各族 handler 的既有字段）/ the settlement port
	 * @return 是否已结算并发出收尾 / whether the claim settled and the tail was sent
	 */
	public static boolean claim(Player player, int questId, int dialogId, NativeReportRewardFlow rewardFlow) {
		return claim(player, questId, dialogId, rewardFlow, null);
	}

	/**
	 * 同上，另接受**族级后置**（在结算体成功之后、关窗之前执行）——供带完成流条件回收段的族使用
	 * （CombineTask：扣产物 + 忘配方；其余族与 DD 传 null）。
	 * <p>
	 * Same as above with a family post-claim hook (run after the settlement, before the window closes);
	 * only lanes whose completion carries family-level recycling need it.
	 * @param familyPostClaim 族级后置（可为 null）/ the family-level post-claim action, nullable
	 * @return 是否已结算并发出收尾 / whether the claim settled and the tail was sent
	 */
	public static boolean claim(Player player, int questId, int dialogId, NativeReportRewardFlow rewardFlow,
			Runnable familyPostClaim) {
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
		if (familyPostClaim != null) {
			familyPostClaim.run();
		}
		// 收尾 = 关窗（原版 0x5d8；与 typed 车道无目标 CloseDialog 及案例 8.3 合同一致）。
		// Tail = close the window (retail 0x5d8; the typed targetless CloseDialog shape).
		DialogService.closeDialog(player, 0);
		return true;
	}
}
