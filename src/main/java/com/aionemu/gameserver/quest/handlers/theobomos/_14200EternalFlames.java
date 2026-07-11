package com.aionemu.gameserver.quest.handlers.theobomos;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 西奥博莫斯任务脚本：Eternal Flames（任务 ID 14200）。
 * Theobomos quest script: Eternal Flames (quest ID 14200).
 *
 * @author FrozenKiller
 */
public class _14200EternalFlames extends QuestHandler {

    private final static int questId = 14200;
    public _14200EternalFlames() {
        super(questId);
    }

    @Override
    public void register() {
        qe.registerQuestNpc(798155).addOnQuestStart(questId); // Atropos
        qe.registerQuestNpc(798155).addOnTalkEvent(questId); // Atropos
        qe.registerQuestNpc(798206).addOnTalkEvent(questId); // Josnack
        qe.registerQuestNpc(700390).addOnKillEvent(questId); // Eternal Flame
    }

    @Override
    public boolean onDialogEvent(final QuestEnv env) {
        final Player player = env.getPlayer();
        int targetId = env.getTargetId();
        QuestState qs = player.getQuestStateList().getQuestState(questId);
        QuestDialog dialog = env.getDialog();
        if (qs == null || qs.getStatus() == QuestStatus.NONE) {
            if (targetId == 798155) { // Atropos
                if (dialog == QuestDialog.START_DIALOG) {
                    return sendQuestDialog(env, 1011);
                }
                else {
                    return sendQuestStartDialog(env);
                }
            }
        }
        else if (qs.getStatus() == QuestStatus.START) {
            int var = qs.getQuestVarById(0);
            if (targetId == 798206) { // Josnack
                switch (dialog) {
                    case START_DIALOG: {
                        if (var == 0) {
                            return sendQuestDialog(env, 1352);
                        }
                    }
                    case STEP_TO_1: {
                        qs.setQuestVar(0);
                        updateQuestStatus(env);
                        updateQuestStatus(env); // That's not an misstake it's needed for the Quest Update Sound
                        return closeDialogWindow(env);
                    }
                    default:
                        break;
                }
            }
            else if (targetId == 798155) { // Atropos
                switch (dialog) {
                    case START_DIALOG: {
                        if (var == 3) {
                            return sendQuestDialog(env, 2375);
                        }
                    }
                    case SELECT_REWARD: {
                        qs.setStatus(QuestStatus.REWARD);
                        updateQuestStatus(env);
                        return sendQuestDialog(env, 5);
                    }
                    default:
                        break;
                }
            }
        }
        else if (qs == null || qs.getStatus() == QuestStatus.REWARD) {
            if (targetId == 798155) { // Atropos
                return sendQuestEndDialog(env);
            }
        }
        return false;
    }

    @Override
    public boolean onKillEvent(QuestEnv env) {
        Player player = env.getPlayer();
        QuestState qs = player.getQuestStateList().getQuestState(questId);
        if (qs == null || qs.getStatus() != QuestStatus.START) {
            return false;
        }
		int targetId = env.getTargetId();
        int var = qs.getQuestVarById(0);
        if (var >= 0 && var < 3) {
            return defaultOnKillEvent(env, 700390, var, var + 1); // 0 - 3
        }
        else {
            return false;
        }
    }
}
