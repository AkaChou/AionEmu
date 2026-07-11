package com.aionemu.gameserver.quest.handlers.inggison;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.model.QuestDialog;

/**
 * 因吉森任务脚本：Easy As 4 3 2 1（任务 ID 11227）。
 * Inggison quest script: Easy As 4 3 2 1 (quest ID 11227).
 *
 * @author Kortana (Encom)
 */
public class _11227Easy_As_4_3_2_1 extends QuestHandler
{
	private final static int questId = 11227;
	private final static int[] mob_ids = { 217068, 217069, 217070, 217071 };
	
	public _11227Easy_As_4_3_2_1() {
		super(questId);
	}
	
	@Override
	public void register() {
		qe.registerQuestNpc(799076).addOnQuestStart(questId);
		qe.registerQuestNpc(799076).addOnTalkEvent(questId);
		for (int mob_id : mob_ids) {
			qe.registerQuestNpc(mob_id).addOnKillEvent(questId);
		}
	}
	
	@Override
    public boolean onDialogEvent(final QuestEnv env) {
		final Player player = env.getPlayer();
        final QuestState qs = player.getQuestStateList().getQuestState(questId);
        int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 799076) {
				switch (env.getDialog()) {
                    case START_DIALOG: {
						return sendQuestDialog(env, 4762);
					} case ASK_ACCEPTION: {
						return sendQuestDialog(env, 4);
					} case ACCEPT_QUEST: {
						return sendQuestStartDialog(env);
					} case REFUSE_QUEST: {
				        return closeDialogWindow(env);
					}
                }
			}
		} else if (qs.getStatus() == QuestStatus.REWARD) {
            if (targetId == 799076) {
                if (env.getDialog() == QuestDialog.USE_OBJECT) {
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
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs == null || qs.getStatus() != QuestStatus.START) {
			return false;
		}
		int var = qs.getQuestVarById(0);
		int targetId = 0;
		Npc npc = null;
		if (env.getVisibleObject() instanceof Npc) {
			npc = (Npc) env.getVisibleObject();
			targetId = npc.getNpcId();
		}
		switch (targetId) {
			case 217071:
				qs.setQuestVarById(0, var + 1);
				updateQuestStatus(env);
				return true;
			case 217070:
				qs.setQuestVarById(0, var + 1);
				updateQuestStatus(env);
				return true;
			case 217069:
				qs.setQuestVarById(0, var + 1);
				updateQuestStatus(env);
				return true;
			case 217068:
				qs.setStatus(QuestStatus.REWARD);
				updateQuestStatus(env);
				return true;
		}
		return false;
	}
}
