package com.aionemu.gameserver.quest.handlers.trials_of_eternity;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.QuestService;

/**
 * 永恒试炼任务脚本：Records Of The Abyss（任务 ID 16838）。
 * Trials of Eternity quest script: Records Of The Abyss (quest ID 16838).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _16838Records_Of_The_Abyss extends QuestHandler {

    private final static int questId = 16838;
    public _16838Records_Of_The_Abyss() {
        super(questId);
    }
	
    public void register() {
        qe.registerQuestNpc(806566).addOnTalkEvent(questId);
		qe.registerQuestNpc(703454).addOnTalkEvent(questId);
		qe.registerQuestNpc(806566).addOnAtDistanceEvent(questId);
    }
	
    @Override
	public boolean onDialogEvent(QuestEnv env) {
		final Player player = env.getPlayer();
		int targetId = env.getTargetId();
		final QuestState qs = player.getQuestStateList().getQuestState(questId);
		QuestDialog dialog = env.getDialog();
        if (qs == null || qs.getStatus() == QuestStatus.START) {
			if (targetId == 806566) {
				switch (env.getDialog()) {
					case START_DIALOG: {
                        return sendQuestDialog(env, 1011);
					} case CHECK_COLLECTED_ITEMS: {
						if (QuestService.collectItemCheck(env, true)) {
							qs.setStatus(QuestStatus.REWARD);
							updateQuestStatus(env);
							return sendQuestDialog(env, 10000);
						} else {
							return sendQuestDialog(env, 10001);
						}
					}
				}
			}
		}
        if (targetId == 703454) {
			if (dialog == QuestDialog.USE_OBJECT) {
				closeDialogWindow(env);
				return true;
			}
		} 
        else if (qs == null || qs.getStatus() == QuestStatus.REWARD) {
            if (targetId == 806566) {
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
}
