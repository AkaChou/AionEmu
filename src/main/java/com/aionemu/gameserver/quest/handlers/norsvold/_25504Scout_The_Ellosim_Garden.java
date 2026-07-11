package com.aionemu.gameserver.quest.handlers.norsvold;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 诺斯沃尔德任务脚本：Scout The Ellosim Garden（任务 ID 25504）。
 * Norsvold quest script: Scout The Ellosim Garden (quest ID 25504).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _25504Scout_The_Ellosim_Garden extends QuestHandler {

    private final static int questId = 25504;
    public _25504Scout_The_Ellosim_Garden() {
        super(questId);
    }
	
    public void register() {
        qe.registerQuestNpc(806102).addOnQuestStart(questId);
        qe.registerQuestNpc(806102).addOnTalkEvent(questId);
		qe.registerQuestNpc(241658).addOnKillEvent(questId);
		qe.registerQuestNpc(241659).addOnKillEvent(questId);
		qe.registerQuestNpc(241668).addOnKillEvent(questId);
		qe.registerQuestNpc(241669).addOnKillEvent(questId);
		qe.registerQuestNpc(241670).addOnKillEvent(questId);
		qe.registerQuestNpc(241671).addOnKillEvent(questId);
		qe.registerQuestNpc(241672).addOnKillEvent(questId);
		qe.registerQuestNpc(241673).addOnKillEvent(questId);
		qe.registerQuestNpc(241674).addOnKillEvent(questId);
		qe.registerQuestNpc(241675).addOnKillEvent(questId);
		qe.registerQuestNpc(241676).addOnKillEvent(questId);
		qe.registerQuestNpc(241677).addOnKillEvent(questId);
		qe.registerQuestNpc(241678).addOnKillEvent(questId);
		qe.registerQuestNpc(241679).addOnKillEvent(questId);
    }
	
    @Override
    public boolean onDialogEvent(QuestEnv env) {
        Player player = env.getPlayer();
        int targetId = env.getTargetId();
        QuestState qs = player.getQuestStateList().getQuestState(questId);
        if (qs == null || qs.getStatus() == QuestStatus.NONE) {
            if (targetId == 806102) {
                if (env.getDialog() == QuestDialog.START_DIALOG) {
                    return sendQuestDialog(env, 4762);
                } else {
                    return sendQuestStartDialog(env);
                }
            }
        } 
        else if (qs == null || qs.getStatus() == QuestStatus.REWARD) {
            if (targetId == 806102) {
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
                case 241658:
                case 241659:
                case 241668:
                case 241669:
                case 241670:
                case 241671:
                case 241672:
                case 241673:
                case 241674:
                case 241675:
                case 241676:
                case 241677:
                case 241678:
                case 241679:
                if (qs.getQuestVarById(1) < 30) {
					qs.setQuestVarById(1, qs.getQuestVarById(1) + 1);
					updateQuestStatus(env);
				} if (qs.getQuestVarById(1) >= 30) {
					qs.setQuestVarById(0, 1);
					qs.setStatus(QuestStatus.REWARD);
					updateQuestStatus(env);
				}
            }
        }
        return false;
    }
}
