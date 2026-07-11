package com.aionemu.gameserver.quest.handlers.norsvold;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 诺斯沃尔德任务脚本：Infiltrate Kojol Valley（任务 ID 25534）。
 * Norsvold quest script: Infiltrate Kojol Valley (quest ID 25534).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _25534Infiltrate_Kojol_Valley extends QuestHandler {

    private final static int questId = 25534;
    public _25534Infiltrate_Kojol_Valley() {
        super(questId);
    }
	
    public void register() {
        qe.registerQuestNpc(806112).addOnQuestStart(questId);
        qe.registerQuestNpc(806112).addOnTalkEvent(questId);
		qe.registerQuestNpc(241815).addOnKillEvent(questId);
		qe.registerQuestNpc(241816).addOnKillEvent(questId);
		qe.registerQuestNpc(241817).addOnKillEvent(questId);
		qe.registerQuestNpc(241818).addOnKillEvent(questId);
		qe.registerQuestNpc(241819).addOnKillEvent(questId);
		qe.registerQuestNpc(241820).addOnKillEvent(questId);
		qe.registerQuestNpc(241821).addOnKillEvent(questId);
		qe.registerQuestNpc(241822).addOnKillEvent(questId);
		qe.registerQuestNpc(241823).addOnKillEvent(questId);
		qe.registerQuestNpc(243207).addOnKillEvent(questId);
		qe.registerQuestNpc(243211).addOnKillEvent(questId);
		qe.registerQuestNpc(243215).addOnKillEvent(questId);
		qe.registerQuestNpc(243219).addOnKillEvent(questId);
		qe.registerQuestNpc(243223).addOnKillEvent(questId);
		qe.registerQuestNpc(243227).addOnKillEvent(questId);
		qe.registerQuestNpc(243231).addOnKillEvent(questId);
		qe.registerQuestNpc(243235).addOnKillEvent(questId);
		qe.registerQuestNpc(243239).addOnKillEvent(questId);
		qe.registerQuestNpc(243243).addOnKillEvent(questId);
		qe.registerQuestNpc(243247).addOnKillEvent(questId);
		qe.registerQuestNpc(243251).addOnKillEvent(questId);
		qe.registerQuestNpc(243255).addOnKillEvent(questId);
		qe.registerQuestNpc(243259).addOnKillEvent(questId);
		qe.registerQuestNpc(243263).addOnKillEvent(questId);
    }
	
    @Override
    public boolean onDialogEvent(QuestEnv env) {
        Player player = env.getPlayer();
        int targetId = env.getTargetId();
        QuestState qs = player.getQuestStateList().getQuestState(questId);
        if (qs == null || qs.getStatus() == QuestStatus.NONE) {
            if (targetId == 806112) {
                if (env.getDialog() == QuestDialog.START_DIALOG) {
                    return sendQuestDialog(env, 4762);
                } else {
                    return sendQuestStartDialog(env);
                }
            }
        } 
        else if (qs == null || qs.getStatus() == QuestStatus.REWARD) {
            if (targetId == 806112) {
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
				case 241815:
				case 241816:
				case 241817:
				case 241818:
				case 241819:
				case 241820:
				case 241821:
				case 241822:
				case 241823:
				case 243207:
				case 243211:
				case 243215:
				case 243219:
				case 243223:
				case 243227:
				case 243231:
				case 243235:
				case 243239:
				case 243243:
				case 243247:
				case 243251:
				case 243255:
				case 243259:
				case 243263:
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
