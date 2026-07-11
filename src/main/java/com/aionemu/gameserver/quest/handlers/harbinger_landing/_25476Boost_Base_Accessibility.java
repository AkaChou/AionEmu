package com.aionemu.gameserver.quest.handlers.harbinger_landing;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * 先驱者登陆点任务脚本：Boost Base Accessibility（任务 ID 25476）。
 * Harbinger Landing quest script: Boost Base Accessibility (quest ID 25476).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _25476Boost_Base_Accessibility extends QuestHandler {

    private final static int questId = 25476;
	private static final Set<Integer> ab1BLv4D01;
    public _25476Boost_Base_Accessibility() {
        super(questId);
    }
	
	static {
		ab1BLv4D01 = new HashSet<Integer>();
		ab1BLv4D01.add(805826);
		ab1BLv4D01.add(805827);
		ab1BLv4D01.add(805828);
	}
	
	@Override
	public void register() {
		Iterator<Integer> iter = ab1BLv4D01.iterator();
		while (iter.hasNext()) {
			int ab1Id = iter.next();
			qe.registerQuestNpc(ab1Id).addOnQuestStart(questId);
			qe.registerQuestNpc(ab1Id).addOnTalkEvent(questId);
			qe.registerQuestNpc(883279).addOnKillEvent(questId);
			qe.registerQuestNpc(883280).addOnKillEvent(questId);
			qe.registerQuestNpc(883281).addOnKillEvent(questId);
			qe.registerQuestNpc(883282).addOnKillEvent(questId);
			qe.registerQuestNpc(883283).addOnKillEvent(questId);
		}
	}
	
    @Override
    public boolean onDialogEvent(QuestEnv env) {
        int targetId = env.getTargetId();
		final Player player = env.getPlayer();
		if (!ab1BLv4D01.contains(targetId)) {
			return false;
		}
		QuestState qs = player.getQuestStateList().getQuestState(questId);
        if (qs == null || qs.getStatus() == QuestStatus.NONE || qs.canRepeat()) {
            switch (env.getDialog()) {
				case START_DIALOG: {
					return sendQuestDialog(env, 4762);
				} case ACCEPT_QUEST:
				case ACCEPT_QUEST_SIMPLE: {
					return sendQuestStartDialog(env);
				} case REFUSE_QUEST_SIMPLE: {
				    return closeDialogWindow(env);
				}
			}
        }
        else if (qs.getStatus() == QuestStatus.REWARD) {
            if (env.getDialog() == QuestDialog.START_DIALOG) {
                return sendQuestDialog(env, 10002);
		    } else {
				return sendQuestEndDialog(env);
			}
		}
        return false;
    }
	
    public boolean onKillEvent(QuestEnv env) {
        Player player = env.getPlayer();
        QuestState qs = player.getQuestStateList().getQuestState(questId);
        if (qs != null && qs.getStatus() == QuestStatus.START) {
            switch (env.getTargetId()) {
				case 883279:
				case 883280:
				case 883281:
				case 883282:
				case 883283:
                if (qs.getQuestVarById(1) < 8) {
					qs.setQuestVarById(1, qs.getQuestVarById(1) + 1);
					updateQuestStatus(env);
				} if (qs.getQuestVarById(1) >= 8) {
					qs.setQuestVarById(0, 1);
					qs.setStatus(QuestStatus.REWARD);
					updateQuestStatus(env);
				}
            }
        }
        return false;
    }
}
