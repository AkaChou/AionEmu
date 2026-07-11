package com.aionemu.gameserver.network.aion.gmhandler;

import com.aionemu.gameserver.configs.administration.PanelConfig;
import com.aionemu.gameserver.lifecycle.GameStaticDataServices;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.QuestStateList;
import com.aionemu.gameserver.model.templates.QuestTemplate;
import com.aionemu.gameserver.network.aion.serverpackets.SM_QUEST_COMPLETED_LIST;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.QuestService;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * GM 指令：强制完成目标玩家的指定任务。
 * GM command handler that forcibly finishes a quest for the target player.
 *
 * @author Alcapwnd
 */
public class CmdEndQuest extends AbstractGMHandler {

	/**
	 * 创建处理器并立即强制完成任务。
	 * Creates the handler and immediately runs the end-quest logic.
	 *
	 * @param admin 执行指令的管理员 / the admin executing the command
	 * @param params 任务 ID 字符串 / quest id as string
	 */
	public CmdEndQuest(Player admin, String params) {
		super(admin, params);
		run();
	}

	/**
	 * 校验权限后将任务置为奖励态并调用完成流程。
	 * After access check, sets the quest to REWARD and finishes it.
	 */
	private void run() {
		Player t = target != null ? target : admin;

		if (admin.getClientConnection().getAccount().getAccessLevel() <= PanelConfig.ENDQUEST_PANEL_LEVEL) {
			PacketSendUtility.sendMessage(admin, "You haven't access this panel commands");
			return;
		}

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
			PacketSendUtility.sendMessage(admin, "Quest not founded for target " + t.getName());
			return;
		}
		if (list.getQuestState(questID).getStatus() == QuestStatus.COMPLETE) {
			PacketSendUtility.sendMessage(admin, "Quest allready finished");
			return;
		}
		list.getQuestState(questID).setStatus(QuestStatus.REWARD);
		t.getController().updateNearbyQuests();
		QuestEnv env = new QuestEnv(null, t, questID, 0);
		QuestService.finishQuest(env);
		PacketSendUtility.sendPacket(t, new SM_QUEST_COMPLETED_LIST(t.getQuestStateList().getAllFinishedQuests()));
		t.getController().updateNearbyQuests();
	}
}
