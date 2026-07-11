package com.aionemu.gameserver.quest.handlers.high_daevanion;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 高等大天使任务脚本：Overcoming Chaos（任务 ID 15326）。
 * High Daevanion quest script: Overcoming Chaos (quest ID 15326).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _15326Overcoming_Chaos extends QuestHandler {

    private final static int questId = 15326;
	private final static int[] DF5P1 = {219692, 219693, 219694, 219695, 219696, 219697, 219698, 219699, 219700, 219776, 219777, 219778, 219779, 219780, 219781, 219782, 219783, 219784, 219785, 219786};
    public _15326Overcoming_Chaos() {
        super(questId);
    }
	
    public void register() {
        qe.registerQuestNpc(805331).addOnQuestStart(questId); //Machina.
        qe.registerQuestNpc(805331).addOnTalkEvent(questId); //Machina.
		for (int mob: DF5P1) {
			qe.registerQuestNpc(mob).addOnKillEvent(questId);
		}
    }
	
    @Override
    public boolean onDialogEvent(QuestEnv env) {
        Player player = env.getPlayer();
        int targetId = env.getTargetId();
        QuestState qs = player.getQuestStateList().getQuestState(questId);
        QuestDialog dialog = env.getDialog();
        if (qs == null || qs.getStatus() == QuestStatus.NONE || qs.canRepeat()) {
            if (targetId == 805331) { //Machina.
                if (dialog == QuestDialog.START_DIALOG) {
                    return sendQuestDialog(env, 4762);
                } else {
                    return sendQuestStartDialog(env);
                }
            }
        } else if (qs == null || qs.getStatus() == QuestStatus.REWARD) {
            if (targetId == 805331) { //Machina.
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
	
    @Override
    public boolean onKillEvent(QuestEnv env) {
		final Player player = env.getPlayer();
        final QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs == null || qs.getStatus() != QuestStatus.START) {
            return false;
        }
        int var = qs.getQuestVarById(0);
		int var1 = qs.getQuestVarById(1);
        if (var == 0 && var1 >= 0 && var1 < 9) {
			return defaultOnKillEvent(env, DF5P1, var1, var1 + 1, 1);
		} else if (var == 0 && var1 == 9) {
			qs.setQuestVarById(1, 0);
			qs.setQuestVar(1);
			qs.setStatus(QuestStatus.REWARD);
			updateQuestStatus(env);
			return true;
		}
		return false;
	}
}
