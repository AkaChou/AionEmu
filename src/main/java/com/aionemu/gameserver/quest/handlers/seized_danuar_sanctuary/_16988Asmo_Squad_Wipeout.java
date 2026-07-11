package com.aionemu.gameserver.quest.handlers.seized_danuar_sanctuary;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 被占达努阿尔圣所任务脚本：Asmo Squad Wipeout（任务 ID 16988）。
 * Seized Danuar Sanctuary quest script: Asmo Squad Wipeout (quest ID 16988).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _16988Asmo_Squad_Wipeout extends QuestHandler {

    private final static int questId = 16988;
    public _16988Asmo_Squad_Wipeout() {
        super(questId);
    }
	
    public void register() {
        qe.registerQuestNpc(801953).addOnQuestStart(questId);
        qe.registerQuestNpc(801953).addOnTalkEvent(questId);
		qe.registerQuestNpc(804865).addOnTalkEvent(questId);
		qe.registerQuestNpc(233129).addOnKillEvent(questId);
		qe.registerQuestNpc(233130).addOnKillEvent(questId);
		qe.registerQuestNpc(233131).addOnKillEvent(questId);
    }
	
    @Override
    public boolean onDialogEvent(QuestEnv env) {
        Player player = env.getPlayer();
        int targetId = env.getTargetId();
        QuestState qs = player.getQuestStateList().getQuestState(questId);
        if (qs == null || qs.getStatus() == QuestStatus.NONE) {
            if (targetId == 801953) {
                if (env.getDialog() == QuestDialog.START_DIALOG) {
                    return sendQuestDialog(env, 4762);
                } else {
                    return sendQuestStartDialog(env);
                }
            }
        }
        else if (qs == null || qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 804865) {
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
                case 233129:
				case 233130:
				case 233131:
                if (qs.getQuestVarById(1) < 5) {
					qs.setQuestVarById(1, qs.getQuestVarById(1) + 1);
					updateQuestStatus(env);
				} if (qs.getQuestVarById(1) >= 5) {
					qs.setQuestVarById(0, 1);
					qs.setStatus(QuestStatus.REWARD);
					updateQuestStatus(env);
				}
            }
        }
        return false;
    }
}
