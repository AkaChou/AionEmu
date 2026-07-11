package com.aionemu.gameserver.network.aion.gmhandler;

import com.aionemu.gameserver.configs.administration.PanelConfig;
import com.aionemu.gameserver.lifecycle.GameStaticDataServices;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.QuestStateList;
import com.aionemu.gameserver.model.templates.QuestTemplate;
import com.aionemu.gameserver.model.templates.quest.QuestCategory;
import com.aionemu.gameserver.network.aion.serverpackets.SM_QUEST_COMPLETED_LIST;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.Util;
import com.aionemu.gameserver.world.World;

/**
 * GM 指令：删除目标玩家的指定任务进度。
 * GM command handler that deletes a quest state from the target player.
 *
 * @author Alcapwnd
 */
public class CmdDeleteQuest extends AbstractGMHandler {

	/**
	 * 创建处理器并立即执行删除任务。
	 * Creates the handler and immediately runs the delete-quest logic.
	 *
	 * @param admin 执行指令的管理员 / the admin executing the command
	 * @param params 任务 ID 字符串 / quest id as string
	 */
	public CmdDeleteQuest(Player admin, String params) {
		super(admin, params);
		run();
	}

	/**
	 * 校验权限后删除目标玩家指定任务状态并刷新任务列表。
	 * After access check, removes the quest state and refreshes the quest list.
	 */
	private void run() {
		Player t = admin;

		if (admin.getClientConnection().getAccount().getAccessLevel() <= PanelConfig.DELQUEST_PANEL_LEVEL) {
			PacketSendUtility.sendMessage(admin, "You haven't access this panel commands");
			return;
		}

		if (admin.getTarget() != null && admin.getTarget() instanceof Player)
			t = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findPlayer(Util.convertName(admin.getTarget().getName()));

		Integer questID = Integer.parseInt(params);
		if (questID <= 0) {
			return;
		}

		@SuppressWarnings("static-access")
		QuestTemplate qt = GameStaticDataServices.dataManager().QUEST_DATA.getQuestById(questID);
		if (qt == null) {
			PacketSendUtility.sendMessage(admin, "Quest with ID: " + questID + " was not found");
			return;
		}

		QuestStateList list = t.getQuestStateList();
		if (list == null || list.getQuestState(questID) == null) {
			PacketSendUtility.sendMessage(admin, "Quest not deleted for target " + t.getName());
			return;
		}

		QuestState qs = list.getQuestState(questID);
		qs.setQuestVar(0);
		qs.setCompleteCount(0);
		if (qt.getCategory() == QuestCategory.MISSION) {
			qs.setStatus(QuestStatus.START);
		} else {
			qs.setStatus(null);
		}
		if (qs.getPersistentState() != PersistentState.NEW) {
			qs.setPersistentState(PersistentState.DELETED);
		}
		PacketSendUtility.sendPacket(t, new SM_QUEST_COMPLETED_LIST(t.getQuestStateList().getAllFinishedQuests()));
		t.getController().updateNearbyQuests();
	}
}
