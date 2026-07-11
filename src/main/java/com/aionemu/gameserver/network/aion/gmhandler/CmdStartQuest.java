package com.aionemu.gameserver.network.aion.gmhandler;

import com.aionemu.gameserver.configs.administration.PanelConfig;
import com.aionemu.gameserver.lifecycle.GameStaticDataServices;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.QuestTemplate;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.services.QuestService;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * GM 指令：为目标玩家启动指定任务。
 * GM command handler that starts a quest for the target player.
 *
 * @author Alcapwnd
 */
public class CmdStartQuest extends AbstractGMHandler {

	/**
	 * 创建处理器并立即启动任务。
	 * Creates the handler and immediately starts the quest.
	 *
	 * @param admin 执行指令的管理员 / the admin executing the command
	 * @param params 任务 ID 字符串 / quest id as string
	 */
	public CmdStartQuest(Player admin, String params) {
		super(admin, params);
		run();
	}

	/**
	 * 校验权限后为目标玩家启动指定任务。
	 * After access check, starts the given quest for the target player.
	 */
	private void run() {
		Player t = target != null ? target : admin;

		if (admin.getClientConnection().getAccount().getAccessLevel() <= PanelConfig.STARTQUEST_PANEL_LEVEL) {
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
			PacketSendUtility.sendMessage(admin, "Quest with ID: " + questID + "was not founded");
			return;
		}

		QuestEnv env = new QuestEnv(null, t, questID, 0);
		QuestService.startQuest(env);
	}
}
