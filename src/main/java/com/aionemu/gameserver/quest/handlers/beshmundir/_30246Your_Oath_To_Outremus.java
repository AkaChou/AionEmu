package com.aionemu.gameserver.quest.handlers.beshmundir;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 贝希蒙迪尔任务脚本：Your Oath To Outremus（任务 ID 30246）。
 * Beshmundir quest script: Your Oath To Outremus (quest ID 30246).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _30246Your_Oath_To_Outremus extends QuestHandler
{
	private final static int questId = 30246;
	
	public _30246Your_Oath_To_Outremus() {
		super(questId);
	}
	
	public void register() {
		qe.registerQuestNpc(798926).addOnQuestStart(questId);
		qe.registerQuestNpc(798926).addOnTalkEvent(questId);
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		QuestDialog dialog = env.getDialog();
		int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 798926) { 
				if (dialog == QuestDialog.START_DIALOG) {
					return sendQuestDialog(env, 1011);
				} else {
					return sendQuestStartDialog(env);
				}
			}
		} else if (qs.getStatus() == QuestStatus.START) {
			if (targetId == 798926) { 
				if (dialog == QuestDialog.START_DIALOG) {
						return sendQuestDialog(env, 2375);
				}
			  else if (dialog == QuestDialog.SELECT_REWARD) {
			  	qs.setQuestVar(1);
			  	return defaultCloseDialog(env, 1, 1, true, true);
			}
		}
		} else if (qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 798926) {
				if (dialog == QuestDialog.USE_OBJECT) {
					return sendQuestDialog(env, 2375);
				}
				return sendQuestEndDialog(env);
			}
		}
		return false;
	}
}
