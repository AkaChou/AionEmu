package com.aionemu.gameserver.quest.handlers.reshanta;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.QuestService;

/**
 * 雷山塔任务脚本：The Left Wing Chamber（任务 ID 23841）。
 * Reshanta quest script: The Left Wing Chamber (quest ID 23841).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _23841The_Left_Wing_Chamber extends QuestHandler {

    private final static int questId = 23841;
	private final static int[] IDAbReLowWcielA = {214740, 214741, 214742, 214743, 214824, 214825, 214826, 215424};
    public _23841The_Left_Wing_Chamber() {
        super(questId);
    }
	
    public void register() {
        qe.registerQuestNpc(263296).addOnQuestStart(questId); //Srim.
        qe.registerQuestNpc(263296).addOnTalkEvent(questId); //Srim.
		qe.registerQuestNpc(263296).addOnAtDistanceEvent(questId); //Srim.
		for (int mob: IDAbReLowWcielA) {
			qe.registerQuestNpc(mob).addOnKillEvent(questId);
		}
    }
	
    @Override
    public boolean onDialogEvent(QuestEnv env) {
        final Player player = env.getPlayer();
		final QuestState qs = player.getQuestStateList().getQuestState(questId);
        if (qs == null || qs.getStatus() == QuestStatus.REWARD) {
            if (env.getTargetId() == 263296) { //Srim.
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
				case 214740:
				case 214741:
				case 214742:
				case 214743:
				case 214824:
				case 214825:
				case 214826:
				case 215424:
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
