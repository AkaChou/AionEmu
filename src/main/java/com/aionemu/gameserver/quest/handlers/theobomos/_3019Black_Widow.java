package com.aionemu.gameserver.quest.handlers.theobomos;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.QuestService;

/**
 * 西奥博莫斯任务脚本：Black Widow（任务 ID 3019）。
 * Theobomos quest script: Black Widow (quest ID 3019).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _3019Black_Widow extends QuestHandler {

    private final static int questId = 3019;
    public _3019Black_Widow() {
        super(questId);
    }
	
    @Override
    public void register() {
        qe.registerQuestNpc(730106).addOnQuestStart(questId);
        qe.registerQuestNpc(730106).addOnTalkEvent(questId);
        qe.registerQuestNpc(798150).addOnTalkEvent(questId);
    }
	
    @Override
    public boolean onDialogEvent(QuestEnv env) {
        final Player player = env.getPlayer();
        int targetId = 0;
        if (env.getVisibleObject() instanceof Npc) {
            targetId = ((Npc) env.getVisibleObject()).getNpcId();
        }
        final QuestState qs = player.getQuestStateList().getQuestState(questId);
        QuestDialog dialog = env.getDialog();
        if (qs == null || qs.getStatus() == QuestStatus.NONE) {
            if (targetId == 730106) {
                switch (dialog) {
                    case USE_OBJECT: {
                        return sendQuestDialog(env, 4762);
                    } case STEP_TO_1: {
                        QuestService.startQuest(env);
						return closeDialogWindow(env);
                    } default:
                        return sendQuestStartDialog(env);
                }
            }
        } else if (qs.getStatus() == QuestStatus.START) {
            int var = qs.getQuestVarById(0);
            if (targetId == 798150) {
                switch (dialog) {
                    case START_DIALOG: {
                        if (var == 0) {
                            return sendQuestDialog(env, 1011);
                        }
				    } case CHECK_COLLECTED_ITEMS: {
                        return checkQuestItems(env, 0, 1, true, 5, 10001);
                    } 
                }
            }
        } 
        else if (qs != null && qs.getStatus() == QuestStatus.REWARD) {
            if (targetId == 798150) {
                return sendQuestEndDialog(env);
            }
        }
        return false;
    }
}
