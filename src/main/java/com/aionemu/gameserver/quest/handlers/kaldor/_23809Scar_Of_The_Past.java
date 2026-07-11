package com.aionemu.gameserver.quest.handlers.kaldor;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 卡尔多尔任务脚本：Scar Of The Past（任务 ID 23809）。
 * Kaldor quest script: Scar Of The Past (quest ID 23809).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _23809Scar_Of_The_Past extends QuestHandler
{
	private final static int questId = 23809;
	
	public _23809Scar_Of_The_Past() {
		super(questId);
	}
	
	@Override
	public void register() {
		qe.registerQuestNpc(802429).addOnQuestStart(questId); //Vidarr.
        qe.registerQuestNpc(802429).addOnTalkEvent(questId); //Vidarr.
        qe.registerQuestNpc(730969).addOnTalkEvent(questId); //Scorched Tree.
        qe.registerQuestNpc(730970).addOnTalkEvent(questId); //Cindery Tree.
		qe.registerQuestNpc(730971).addOnTalkEvent(questId); //Burnt Tree.
	}
	
	@Override
	public boolean onDialogEvent(final QuestEnv env) {
		Player player = env.getPlayer();
		int targetId = env.getTargetId();
		QuestDialog dialog = env.getDialog();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 802429) { //Vidarr.
				if (dialog == QuestDialog.START_DIALOG) {
					return sendQuestDialog(env, 1011);
				} else {
					return sendQuestStartDialog(env);
				}
			}
		} else if (qs != null && qs.getStatus() == QuestStatus.START) {
			int var = qs.getQuestVarById(0);
			switch (targetId) {
				case 730969: { //Scorched Tree.
                    switch (env.getDialog()) {
						case USE_OBJECT: {
                            return useQuestObject(env, 0, 1, false, 0);
                        }
                    }
                    break;
                } case 730970: { //Cindery Tree.
                    switch (env.getDialog()) {
                        case USE_OBJECT: {
                            return useQuestObject(env, 1, 2, false, 0);
                        }
                    }
                    break;
                } case 730971: { //Burnt Tree.
                    switch (env.getDialog()) {
                        case USE_OBJECT: {
                            return useQuestObject(env, 2, 3, false, 0);
                        }
                    }
                    break;
                } case 802429: { //Vidarr.
				    switch (dialog) {
					    case START_DIALOG:
						    return sendQuestDialog(env, 2375);
					    case SELECT_REWARD:
						    changeQuestStep(env, 3, 4, true);
						    return sendQuestDialog(env, 5);
					}
				}
			}
		} else if (qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 802429) { //Vidarr.
				return sendQuestEndDialog(env);
			}
		}
		return false;
	}
}
