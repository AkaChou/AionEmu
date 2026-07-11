package com.aionemu.gameserver.quest.handlers.high_daevanion;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 高等大天使任务脚本：Tenacious Spree（任务 ID 25324）。
 * High Daevanion quest script: Tenacious Spree (quest ID 25324).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _25324Tenacious_Spree extends QuestHandler {

	private static final int questId = 25324;
	private final static int[] Enshar = {219791, 219792, 219793, 219794, 219795, 219796, 219797, 219798, 219799, 219800, 219997, 220012};
	private final static int[] Levinshor = {233921, 233922, 233923, 233924, 233925, 233926, 233927, 233928};
	private final static int[] Kaldor = {234273, 234275, 234276, 234523};
    public _25324Tenacious_Spree() {
        super(questId);
    }
	
    public void register() {
        qe.registerQuestNpc(805343).addOnQuestStart(questId); //Mashinee.
        qe.registerQuestNpc(805343).addOnTalkEvent(questId); //Mashinee.
		for (int mob: Enshar) {
			qe.registerQuestNpc(mob).addOnKillEvent(questId);
		} for (int mob: Levinshor) {
			qe.registerQuestNpc(mob).addOnKillEvent(questId);
		} for (int mob: Kaldor) {
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
            if (targetId == 805343) { //Mashinee.
                if (dialog == QuestDialog.START_DIALOG) {
                    return sendQuestDialog(env, 4762);
                } else {
                    return sendQuestStartDialog(env);
                }
            }
        } else if (qs == null || qs.getStatus() == QuestStatus.REWARD) {
            if (targetId == 805343) { //Mashinee.
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
        if (var == 0 && var1 >= 0 && var1 < 19) {
			return defaultOnKillEvent(env, Enshar, var1, var1 + 1, 1);
		} else if (var == 0 && var1 == 19) {
			qs.setQuestVarById(1, 0);
			changeQuestStep(env, 0, 1, false);
			updateQuestStatus(env);
			return true;
		} if (var == 1 && var1 >= 0 && var1 < 19) {
			return defaultOnKillEvent(env, Levinshor, var1, var1 + 1, 1);
		} else if (var == 1 && var1 == 19) {
			qs.setQuestVarById(1, 0);
			changeQuestStep(env, 1, 2, false);
			updateQuestStatus(env);
			return true;
		} if (var == 2 && var1 >= 0 && var1 < 19) {
			return defaultOnKillEvent(env, Kaldor, var1, var1 + 1, 1);
		} else if (var == 2 && var1 == 19) {
			qs.setQuestVarById(1, 0);
			qs.setQuestVar(3);
			qs.setStatus(QuestStatus.REWARD);
			updateQuestStatus(env);
			return true;
		}
		return false;
	}
}
