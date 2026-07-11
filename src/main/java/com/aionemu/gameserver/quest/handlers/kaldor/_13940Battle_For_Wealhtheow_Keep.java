package com.aionemu.gameserver.quest.handlers.kaldor;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 卡尔多尔任务脚本：Battle For Wealhtheow Keep（任务 ID 13940）。
 * Kaldor quest script: Battle For Wealhtheow Keep (quest ID 13940).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _13940Battle_For_Wealhtheow_Keep extends QuestHandler {

    private final static int questId = 13940;
	private final static int[] LDF5Fortress7011Guard = {251920, 251925, 251930, 251945, 251950, 251955, 252100, 252000, 252010, 252020, 252025, 252030, 252035, 252105};
    public _13940Battle_For_Wealhtheow_Keep() {
        super(questId);
    }
	
    public void register() {
        qe.registerQuestNpc(802431).addOnQuestStart(questId); //Alphioh.
        qe.registerQuestNpc(802431).addOnTalkEvent(questId); //Alphioh.
		for (int mob: LDF5Fortress7011Guard) {
			qe.registerQuestNpc(mob).addOnKillEvent(questId);
		}
    }
	
    @Override
    public boolean onDialogEvent(QuestEnv env) {
        final Player player = env.getPlayer();
		int targetId = env.getTargetId();
		final QuestState qs = player.getQuestStateList().getQuestState(questId);
        QuestDialog dialog = env.getDialog();
		if (qs == null || qs.getStatus() == QuestStatus.NONE || qs.canRepeat()) {
            if (targetId == 802431) { //Alphioh.
                if (dialog == QuestDialog.START_DIALOG) {
                    return sendQuestDialog(env, 4762);
                } else {
                    return sendQuestStartDialog(env);
                }
            }
        }
        else if (qs == null || qs.getStatus() == QuestStatus.REWARD) {
            if (targetId == 802431) { //Alphioh.
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
        final Player player = env.getPlayer();
        final QuestState qs = player.getQuestStateList().getQuestState(questId);
        if (qs != null && qs.getStatus() == QuestStatus.START) {
            switch (env.getTargetId()) {
				case 251920:
				case 251925:
				case 251930:
				case 251945:
				case 251950:
				case 251955:
				case 252100:
				case 252000:
				case 252010:
				case 252020:
				case 252025:
				case 252030:
				case 252035:
				case 252105:
                if (qs.getQuestVarById(1) < 10) {
					qs.setQuestVarById(1, qs.getQuestVarById(1) + 1);
					updateQuestStatus(env);
				} if (qs.getQuestVarById(1) >= 10) {
					qs.setQuestVarById(0, 1);
					qs.setStatus(QuestStatus.REWARD);
					updateQuestStatus(env);
				}
            }
        }
        return false;
    }
}
