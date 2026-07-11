package com.aionemu.gameserver.quest.handlers.reshanta;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.QuestService;

/**
 * 雷山塔任务脚本：Occupy Siel Eastern Fortress（任务 ID 13845）。
 * Reshanta quest script: Occupy Siel Eastern Fortress (quest ID 13845).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _13845Occupy_Siel_Eastern_Fortress extends QuestHandler {

    private final static int questId = 13845;
	private final static int[] IDAbReLowEcielE = {214744, 214745, 214746, 214747, 214748, 214749, 214750, 214751, 214803};
    public _13845Occupy_Siel_Eastern_Fortress() {
        super(questId);
    }
	
    public void register() {
        qe.registerQuestNpc(263597).addOnQuestStart(questId); //Silvius.
        qe.registerQuestNpc(263597).addOnTalkEvent(questId); //Silvius.
		qe.registerQuestNpc(263597).addOnAtDistanceEvent(questId); //Silvius.
		for (int mob: IDAbReLowEcielE) {
			qe.registerQuestNpc(mob).addOnKillEvent(questId);
		}
    }
	
    @Override
    public boolean onDialogEvent(QuestEnv env) {
        final Player player = env.getPlayer();
		final QuestState qs = player.getQuestStateList().getQuestState(questId);
        if (qs == null || qs.getStatus() == QuestStatus.REWARD) {
            if (env.getTargetId() == 263597) { //Silvius.
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
	public boolean onAtDistanceEvent(QuestEnv env) {
		final Player player = env.getPlayer();
        final QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs == null || qs.getStatus() == QuestStatus.NONE || qs.canRepeat()) {
			QuestService.startQuest(env);
			return true;
		}
		return false;
	}
	
    public boolean onKillEvent(QuestEnv env) {
        final Player player = env.getPlayer();
        final QuestState qs = player.getQuestStateList().getQuestState(questId);
        if (qs != null && qs.getStatus() == QuestStatus.START) {
            switch (env.getTargetId()) {
				case 214744:
				case 214745:
				case 214746:
				case 214747:
				case 214748:
				case 214749:
				case 214750:
				case 214751:
				case 214803:
                if (qs.getQuestVarById(1) < 40) {
					qs.setQuestVarById(1, qs.getQuestVarById(1) + 1);
					updateQuestStatus(env);
				} if (qs.getQuestVarById(1) >= 40) {
					qs.setQuestVarById(0, 1);
					qs.setStatus(QuestStatus.REWARD);
					updateQuestStatus(env);
				}
            }
        }
        return false;
    }
}
