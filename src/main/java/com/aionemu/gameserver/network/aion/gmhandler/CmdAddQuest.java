package com.aionemu.gameserver.network.aion.gmhandler;

import com.aionemu.gameserver.lifecycle.GameEngineServices;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.QuestService;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * GM 指令：为玩家添加（启动）任务。
 * GM command handler that starts a quest for a player.
 *
 * @author Kill3r
 */
public class CmdAddQuest extends AbstractGMHandler {

	/**
	 * 创建处理器并立即执行添加任务逻辑。
	 * Creates the handler and immediately runs the add-quest logic.
	 *
	 * @param admin 执行指令的管理员 / the admin executing the command
	 * @param params 任务 ID 字符串 / quest id as string
	 */
	public CmdAddQuest(Player admin, String params) {
		super(admin, params);
		run();
	}

	/**
	 * 解析任务 ID 并尝试启动任务；失败时检查并提示前置条件。
	 * Parses the quest id and attempts to start the quest; reports failed preconditions.
	 */
	public void run() {
		try {
			int id = Integer.parseInt(params);
			QuestEnv env = new QuestEnv(admin, target, id, 0);

			if (QuestService.startQuest(env)) {
				PacketSendUtility.sendMessage(admin, "Quest started.");
			} else {
				var metadata = GameEngineServices.questEngine().questCatalog().findMetadata(id).orElse(null);
				if (metadata != null) {
					// Alternative start-condition groups are diagnosed by the canonical start service.
					for (int prerequisite : metadata.prerequisites()) {
						QuestState state = target.getQuestStateList().getQuestState(prerequisite);
						if (state == null || state.getStatus() != QuestStatus.COMPLETE) {
							PacketSendUtility.sendMessage(admin,
								"You have to finish " + prerequisite + " first!");
						}
					}
				}
				PacketSendUtility.sendMessage(admin, "Quest not started. Some preconditions failed");
			}
		} catch (NumberFormatException e) {
			PacketSendUtility.sendMessage(admin, "Quest Id Not Found!");
		}
	}
}
