package com.aionemu.gameserver.ai.quests;

import java.util.List;
import java.util.Map;

import com.aionemu.gameserver.ai.ActionItemNpcAI2;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.lifecycle.GameEngineServices;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.questEngine.definition.QuestDialogAction;
import com.aionemu.gameserver.questEngine.definition.QuestDialogPage;
import com.aionemu.gameserver.services.item.ItemService;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 任务相关 NPC AI：Quest Start Item Npc Ai2，继承 ActionItemNpcAI2。
 * 对应野外场景中交互后开出任务启动道具的物体（如贝尔特伦信函 700009、克拉尔书 700004、旧箱子 700513 等）。
 * 遵循原版原生分层：发道具与物体消失为 NPC AI 行为，任务系统本身保持原版表驱动解耦。
 */
@AIName("quest_start_use_item,scroll_q41,scroll_q49,scroll_q2498,npc_ai_box_q1559,npc_ai_fobj_q11036a,npc_ai_fobj_q11123a,npc_ai_fobj_q11143a")
public class QuestStartItemNpcAi2 extends ActionItemNpcAI2 {

	public record StartItemReward(int questId, int itemId) {
	}

	private static final Map<Integer, StartItemReward> REWARDS_BY_NPC = Map.of(
		700004, new StartItemReward(1197, 182200558),
		700009, new StartItemReward(1198, 182200559),
		700302, new StartItemReward(2498, 182204232),
		700513, new StartItemReward(1559, 182201823),
		700610, new StartItemReward(11036, 182206731),
		700616, new StartItemReward(11123, 182206798),
		700909, new StartItemReward(11143, 182206866)
	);

	@Override
	protected void handleDialogStart(Player player) {
		super.handleDialogStart(player);
	}

	@Override
	protected void handleUseItemFinish(Player player) {
		List<Integer> relatedQuests = GameEngineServices.questEngine().getQuestNpc(getOwner().getNpcId()).getOnQuestStart();
		for (int dialogId : dialogIdsFor(!relatedQuests.isEmpty())) {
			if (AI2Actions.selectDialog(this, player, 0, dialogId).isSuccess()) {
				return;
			}
		}
		if (tryGiveQuestStartItem(player)) {
			return;
		}
		if (isDialogNpc()) {
			PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), QuestDialogPage.SELECT1.id()));
		}
	}

	protected boolean tryGiveQuestStartItem(Player player) {
		StartItemReward reward = rewardForNpc(getOwner().getNpcId());
		if (reward == null) {
			return false;
		}
		if (canReceiveReward(player, reward.questId(), reward.itemId())) {
			if (ItemService.addItem(player, reward.itemId(), 1) == 0) {
				AI2Actions.deleteOwner(this);
				return true;
			}
		}
		return false;
	}

	public static StartItemReward rewardForNpc(int npcId) {
		return REWARDS_BY_NPC.get(npcId);
	}

	public static boolean canReceiveReward(Player player, int questId, int itemId) {
		if (player == null) {
			return false;
		}
		if (player.isCompleteQuest(questId)) {
			return false;
		}
		if (player.getInventory() != null && player.getInventory().getItemCountByItemId(itemId) > 0) {
			return false;
		}
		return true;
	}

	static List<Integer> dialogIdsFor(boolean hasQuestStart) {
		return hasQuestStart
			? List.of(QuestDialogAction.USE_OBJECT.id(), QuestDialogAction.QUEST_SELECT.id())
			: List.of(QuestDialogAction.USE_OBJECT.id());
	}

	private boolean isDialogNpc() {
		return getObjectTemplate().isDialogNpc();
	}
}
