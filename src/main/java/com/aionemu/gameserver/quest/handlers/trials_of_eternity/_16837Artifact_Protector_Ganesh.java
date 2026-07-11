package com.aionemu.gameserver.quest.handlers.trials_of_eternity;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.QuestService;

/**
 * 永恒试炼任务脚本：Artifact Protector Ganesh（任务 ID 16837）。
 * Trials of Eternity quest script: Artifact Protector Ganesh (quest ID 16837).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _16837Artifact_Protector_Ganesh extends QuestHandler {

    private final static int questId = 16837;
    public _16837Artifact_Protector_Ganesh() {
        super(questId);
    }
	
    public void register() {
        qe.registerQuestNpc(806564).addOnTalkEvent(questId);
		qe.registerQuestNpc(806568).addOnTalkEvent(questId);
		qe.registerQuestNpc(246418).addOnKillEvent(questId);
		qe.registerQuestNpc(806564).addOnAtDistanceEvent(questId);
    }
	
    @Override
    public boolean onDialogEvent(QuestEnv env) {
        final Player player = env.getPlayer();
		int targetId = env.getTargetId();
		final QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs == null || qs.getStatus() == QuestStatus.REWARD) {
            if (targetId == 806568) {
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
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			QuestService.startQuest(env);
			return true;
		}
		return false;
	}
	
	@Override
    public boolean onKillEvent(QuestEnv env) {
        final Player player = env.getPlayer();
        final QuestState qs = player.getQuestStateList().getQuestState(questId);
        int targetId = env.getTargetId();
		if (qs != null && qs.getStatus() == QuestStatus.START) {
            int var = qs.getQuestVarById(0);
            if (var == 0) {
				switch (targetId) {
                    case 246418: { //가네슈.
					    qs.setQuestVar(1);
						qs.setStatus(QuestStatus.REWARD);
					    updateQuestStatus(env);
						return true;
					}
                }
			}
        }
        return false;
    }
}
