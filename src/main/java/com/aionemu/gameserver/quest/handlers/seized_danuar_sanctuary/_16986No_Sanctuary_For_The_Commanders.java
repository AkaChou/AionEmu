package com.aionemu.gameserver.quest.handlers.seized_danuar_sanctuary;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 被占达努阿尔圣所任务脚本：No Sanctuary For The Commanders（任务 ID 16986）。
 * Seized Danuar Sanctuary quest script: No Sanctuary For The Commanders (quest ID 16986).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _16986No_Sanctuary_For_The_Commanders extends QuestHandler {

    private final static int questId = 16986;
    public _16986No_Sanctuary_For_The_Commanders() {
        super(questId);
    }
	
    public void register() {
        qe.registerQuestNpc(804862).addOnQuestStart(questId);
        qe.registerQuestNpc(804862).addOnTalkEvent(questId);
		qe.registerQuestNpc(804864).addOnTalkEvent(questId);
		qe.registerQuestNpc(235619).addOnKillEvent(questId);
		qe.registerQuestNpc(235620).addOnKillEvent(questId);
		qe.registerQuestNpc(235621).addOnKillEvent(questId);
		qe.registerQuestNpc(235624).addOnKillEvent(questId);
		qe.registerQuestNpc(235625).addOnKillEvent(questId);
		qe.registerQuestNpc(235626).addOnKillEvent(questId);
    }
	
    @Override
    public boolean onDialogEvent(QuestEnv env) {
        Player player = env.getPlayer();
        int targetId = env.getTargetId();
        QuestState qs = player.getQuestStateList().getQuestState(questId);
        if (qs == null || qs.getStatus() == QuestStatus.NONE) {
            if (targetId == 804862) {
                if (env.getDialog() == QuestDialog.START_DIALOG) {
                    return sendQuestDialog(env, 4762);
                } else {
                    return sendQuestStartDialog(env);
                }
            }
        }
        else if (qs == null || qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 804864) {
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
            switch (env.getTargetId()) {
                case 235619:
				case 235620:
				case 235621:
				case 235624:
				case 235625:
				case 235626:
                if (qs.getQuestVarById(1) < 1) {
					qs.setQuestVarById(1, qs.getQuestVarById(1) + 1);
					updateQuestStatus(env);
				} if (qs.getQuestVarById(1) >= 1) {
					qs.setQuestVarById(0, 1);
					qs.setStatus(QuestStatus.REWARD);
					updateQuestStatus(env);
				}
            }
        }
        return false;
    }
}
