package com.aionemu.gameserver.quest.handlers.iluma;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 伊卢玛任务脚本：Singing The Hymn Of Aion（任务 ID 15500）。
 * Iluma quest script: Singing The Hymn Of Aion (quest ID 15500).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _15500Singing_The_Hymn_Of_Aion extends QuestHandler {

    private final static int questId = 15500;
    public _15500Singing_The_Hymn_Of_Aion() {
        super(questId);
    }
	
    public void register() {
        qe.registerQuestNpc(806089).addOnQuestStart(questId);
        qe.registerQuestNpc(806089).addOnTalkEvent(questId);
		qe.registerQuestNpc(241656).addOnKillEvent(questId);
		qe.registerQuestNpc(241657).addOnKillEvent(questId);
		qe.registerQuestNpc(241658).addOnKillEvent(questId);
		qe.registerQuestNpc(241659).addOnKillEvent(questId);
		qe.registerQuestNpc(241660).addOnKillEvent(questId);
		qe.registerQuestNpc(241661).addOnKillEvent(questId);
		qe.registerQuestNpc(241662).addOnKillEvent(questId);
		qe.registerQuestNpc(241663).addOnKillEvent(questId);
		qe.registerQuestNpc(241664).addOnKillEvent(questId);
		qe.registerQuestNpc(241665).addOnKillEvent(questId);
    }
	
    @Override
    public boolean onDialogEvent(QuestEnv env) {
        Player player = env.getPlayer();
        int targetId = env.getTargetId();
        QuestState qs = player.getQuestStateList().getQuestState(questId);
        if (qs == null || qs.getStatus() == QuestStatus.NONE || qs.canRepeat()) {
            if (targetId == 806089) {
                if (env.getDialog() == QuestDialog.START_DIALOG) {
                    return sendQuestDialog(env, 4762);
                } else {
                    return sendQuestStartDialog(env);
                }
            }
        } 
        else if (qs == null || qs.getStatus() == QuestStatus.REWARD) {
            if (targetId == 806089) {
                if (env.getDialog() == QuestDialog.START_DIALOG) {
                    return sendQuestDialog(env, 10002);
				} else if (env.getDialog() == QuestDialog.SELECT_REWARD) {
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
                case 241656:
				case 241657:
				case 241658:
				case 241659:
				case 241660:
				case 241661:
				case 241662:
				case 241663:
				case 241664:
				case 241665:
                if (qs.getQuestVarById(1) < 20) {
					qs.setQuestVarById(1, qs.getQuestVarById(1) + 1);
					updateQuestStatus(env);
				} if (qs.getQuestVarById(1) >= 20) {
					qs.setQuestVarById(0, 1);
					qs.setStatus(QuestStatus.REWARD);
					updateQuestStatus(env);
				}
            }
        }
        return false;
    }
}
