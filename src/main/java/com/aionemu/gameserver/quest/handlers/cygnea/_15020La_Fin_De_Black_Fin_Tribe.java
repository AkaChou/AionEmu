package com.aionemu.gameserver.quest.handlers.cygnea;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 希格尼娅任务脚本：La Fin De Black Fin Tribe（任务 ID 15020）。
 * Cygnea quest script: La Fin De Black Fin Tribe (quest ID 15020).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _15020La_Fin_De_Black_Fin_Tribe extends QuestHandler {

    private final static int questId = 15020;
    public _15020La_Fin_De_Black_Fin_Tribe() {
        super(questId);
    }
	
    public void register() {
        qe.registerQuestNpc(804876).addOnQuestStart(questId);
        qe.registerQuestNpc(804876).addOnTalkEvent(questId);
		qe.registerQuestNpc(235826).addOnKillEvent(questId);
		qe.registerQuestNpc(235827).addOnKillEvent(questId);
    }
	
    @Override
    public boolean onDialogEvent(QuestEnv env) {
        Player player = env.getPlayer();
        int targetId = env.getTargetId();
        QuestState qs = player.getQuestStateList().getQuestState(questId);
        QuestDialog dialog = env.getDialog();
        if (qs == null || qs.getStatus() == QuestStatus.NONE) {
            if (targetId == 804876) {
                if (dialog == QuestDialog.START_DIALOG) {
                    return sendQuestDialog(env, 4762);
                } else {
                    return sendQuestStartDialog(env);
                }
            }
        }
        else if (qs != null && qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 804876) {
				if (env.getDialogId() == 1352) {
					return sendQuestDialog(env, 5);
				} else {
					return sendQuestEndDialog(env);
				}
			}
		}
        return false;
    }
	
    public boolean onKillEvent(QuestEnv env) {
        Player player = env.getPlayer();
        QuestState qs = player.getQuestStateList().getQuestState(questId);
        if (qs != null && qs.getStatus() == QuestStatus.START) {
			int var = qs.getQuestVarById(0);
			if (var == 0) {
				int targetId = env.getTargetId();
				int var1 = qs.getQuestVarById(1);
				int var2 = qs.getQuestVarById(2);
				switch (targetId) {
					case 235826: {
						if (var1 < 4) {
							return defaultOnKillEvent(env, 235826, 0, 4, 1);
						}
						else if (var1 == 4) {
							if (var2 == 5) {
								qs.setQuestVar(1);
								qs.setStatus(QuestStatus.REWARD);
								updateQuestStatus(env);
								return true;
							}
							else {
								return defaultOnKillEvent(env, 235826, 4, 5, 1);
							}
						}
						break;
					}
					case 235827: {
						if (var2 < 4) {
							return defaultOnKillEvent(env, 235827, 0, 4, 2);
						}
						else if (var2 == 4) {
							if (var1 == 5) {
								qs.setQuestVar(1);
								qs.setStatus(QuestStatus.REWARD);
								updateQuestStatus(env);
								return true;
							}
							else {
								return defaultOnKillEvent(env, 235827, 4, 5, 2);
							}
						}
						break;
					}
				}
			}
		}
		return false;
	}
}	
