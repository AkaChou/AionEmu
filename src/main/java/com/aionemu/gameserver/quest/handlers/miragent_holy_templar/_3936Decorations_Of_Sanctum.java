package com.aionemu.gameserver.quest.handlers.miragent_holy_templar;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 米拉根特圣殿骑士任务脚本：Decorations Of Sanctum（任务 ID 3936）。
 * Miragent Holy Templar quest script: Decorations Of Sanctum (quest ID 3936).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _3936Decorations_Of_Sanctum extends QuestHandler {

	private final static int questId = 3936;
	public _3936Decorations_Of_Sanctum() {
		super(questId);
	}
	
	@Override
	public void register() {
		qe.registerQuestNpc(203710).addOnQuestStart(questId); //Dairos.
		qe.registerQuestNpc(203710).addOnTalkEvent(questId); //Dairos.
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		QuestDialog dialog = env.getDialog();
		int targetId = env.getTargetId();
		if (qs == null || (qs.getStatus() == QuestStatus.NONE || qs.getStatus() == QuestStatus.COMPLETE)) {
			if (targetId == 203710) { //Dairos.
				if (dialog == QuestDialog.START_DIALOG)
					return sendQuestDialog(env, 4762);
				else
					return sendQuestStartDialog(env);
			}
		} if (qs == null)
			return false;	
		int var = qs.getQuestVarById(0);
		if (qs.getStatus() == QuestStatus.START) {
			switch (targetId) {
				case 203710: //Dairos.
					switch (dialog) {
						case START_DIALOG:
							if (var == 0)
								return sendQuestDialog(env, 1011);
							else if (var == 1)
								return sendQuestDialog(env, 1352);
						case STEP_TO_1:
							return defaultCloseDialog(env, 0, 1);
						case CHECK_COLLECTED_ITEMS:
							long itemCount1 = player.getInventory().getItemCountByItemId(182206091);
							long itemCount2 = player.getInventory().getItemCountByItemId(182206092);
							long itemCount3 = player.getInventory().getItemCountByItemId(182206093);
							long itemCount4 = player.getInventory().getItemCountByItemId(182206094);
							if (itemCount1 >= 10 && itemCount2 >= 10 && itemCount3 >= 10 && itemCount4 >= 10) {
								removeQuestItem(env, 182206091, 10);
								removeQuestItem(env, 182206092, 10);
								removeQuestItem(env, 182206093, 10);
								removeQuestItem(env, 182206094, 10);
								changeQuestStep(env, 1, 1, true);
								return sendQuestDialog(env, 5);
							}
							else
								return sendQuestDialog(env, 10001);
					}
					break;
				default:
					return sendQuestStartDialog(env);
			}
		} else if (qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 203710) //Dairos.
				return sendQuestEndDialog(env);
		}
		return false;
	}
}
