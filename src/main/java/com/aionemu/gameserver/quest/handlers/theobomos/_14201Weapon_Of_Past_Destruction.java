package com.aionemu.gameserver.quest.handlers.theobomos;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.QuestService;

/**
 * 西奥博莫斯任务脚本：Weapon Of Past Destruction（任务 ID 14201）。
 * Theobomos quest script: Weapon Of Past Destruction (quest ID 14201).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _14201Weapon_Of_Past_Destruction extends QuestHandler {
	
    private final static int questId = 14201;
    public _14201Weapon_Of_Past_Destruction() {
        super(questId);
    }    
	
    @Override
    public void register() {
        qe.registerQuestNpc(798155).addOnQuestStart(questId); //Atropos.
        qe.registerQuestNpc(798155).addOnTalkEvent(questId); //Atropos.
		qe.registerQuestNpc(800407).addOnTalkEvent(questId); //Hongras.
		qe.registerQuestNpc(798212).addOnTalkEvent(questId); //Serimnir.
    }
	
	@Override
    public boolean onDialogEvent(final QuestEnv env) {
        final Player player = env.getPlayer();
		final QuestState qs = player.getQuestStateList().getQuestState(questId);
		QuestDialog dialog = env.getDialog();
        int targetId = env.getTargetId();
        if (qs == null || qs.getStatus() == QuestStatus.NONE) {
            if (targetId == 798155) { //Atropos.
                if (dialog == QuestDialog.START_DIALOG) {
                    return sendQuestDialog(env, 1011);
                } else {
                    return sendQuestStartDialog(env);
                }
            }
		} else if (qs != null && qs.getStatus() == QuestStatus.START) {
			int var = qs.getQuestVarById(0);
			if (targetId == 800407) { //Hongras.
				switch (dialog) {
					case START_DIALOG: {
						if (var == 0) {
							return sendQuestDialog(env, 1352);
						}
					} case STEP_TO_1: {
						qs.setQuestVar(1);
						updateQuestStatus(env);
						return closeDialogWindow(env);
					}
				}
			} else if (targetId == 798212) { //Serimnir.
				switch (dialog) {
					case START_DIALOG: {
						if (var == 1) {
							return sendQuestDialog(env, 1693);
						}
					}
					case STEP_TO_2: {
						qs.setQuestVar(2);
						updateQuestStatus(env);
						return closeDialogWindow(env);
					}
				}
			} else if (targetId == 798155) { //Atropos.
				switch (dialog) {
					case START_DIALOG: {
						if (var == 2) {
							return sendQuestDialog(env, 2375);
						}
					} case CHECK_COLLECTED_ITEMS_SIMPLE: {
						if (QuestService.collectItemCheck(env, true)) {
							changeQuestStep(env, 2, 2, true);
							return sendQuestDialog(env, 5);
                        } else {
							return closeDialogWindow(env);
						}
					}
				}
			}
        } else if (qs != null && qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 798155) { //Atropos.
                return sendQuestEndDialog(env);
			}
		}
        return false;
    }
}
