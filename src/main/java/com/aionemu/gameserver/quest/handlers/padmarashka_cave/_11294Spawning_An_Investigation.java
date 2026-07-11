package com.aionemu.gameserver.quest.handlers.padmarashka_cave;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 帕德玛拉什卡洞穴任务脚本：Spawning An Investigation（任务 ID 11294）。
 * Padmarashka Cave quest script: Spawning An Investigation (quest ID 11294).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _11294Spawning_An_Investigation extends QuestHandler {

	private final static int questId = 11294;
	public _11294Spawning_An_Investigation() {
		super(questId);
	}
	
	@Override
	public void register() {
		qe.registerQuestNpc(799092).addOnQuestStart(questId); //Jamia.
		qe.registerQuestNpc(799092).addOnTalkEvent(questId); //Jamia.
		qe.registerQuestNpc(798926).addOnTalkEvent(questId); //Outremus.
		qe.registerQuestNpc(799010).addOnTalkEvent(questId); //Wivius.
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		final Player player = env.getPlayer();
		int targetId = 0;
		if (env.getVisibleObject() instanceof Npc)
			targetId = ((Npc) env.getVisibleObject()).getNpcId();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
        if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 799092) { //Jamia.
				if (env.getDialog() == QuestDialog.START_DIALOG) {
					return sendQuestDialog(env, 1011);
				} else if (env.getDialogId() == 1002) {
					if (giveQuestItem(env, 182213038, 1)) //Rejeton De Padmarashka.
						return sendQuestStartDialog(env);
					else
						return true;
				} else
					return sendQuestStartDialog(env);
			}
		} if (qs == null)
			return false;
		if (qs.getStatus() == QuestStatus.START) {
			switch (targetId) {
				case 798926: { //Outremus.
					switch (env.getDialog()) {
						case START_DIALOG: {
							return sendQuestDialog(env, 1352);
						} case STEP_TO_1: {
							qs.setQuestVarById(0, qs.getQuestVarById(0) + 1);
							updateQuestStatus(env);
					        return closeDialogWindow(env);
						}
					}
				} case 799010: { //Wivius.
					switch (env.getDialog()) {
						case START_DIALOG: {
							return sendQuestDialog(env, 2375);
						} case SELECT_REWARD: {
							qs.setQuestVar(2);
							qs.setStatus(QuestStatus.REWARD);
							updateQuestStatus(env);
							return sendQuestEndDialog(env);
						}
					}
				}
			}
		} else if (qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 799010) { //Wivius.
				switch (env.getDialog()) {
					case SELECT_REWARD: {
						return sendQuestDialog(env, 5);
					} default:
						return sendQuestEndDialog(env);
				}
			}
		}
		return false;
	}
}
