package com.aionemu.gameserver.quest.handlers.padmarashka_cave;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 帕德玛拉什卡洞穴任务脚本：Padmarashka Legacy（任务 ID 21296）。
 * Padmarashka Cave quest script: Padmarashka Legacy (quest ID 21296).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _21296Padmarashka_Legacy extends QuestHandler {

	private final static int questId = 21296;
	public _21296Padmarashka_Legacy() {
		super(questId);
	}
	
	@Override
	public void register() {
		qe.registerQuestNpc(799444).addOnQuestStart(questId); //Kimin.
		qe.registerQuestNpc(799444).addOnTalkEvent(questId); //Kimin.
		qe.registerQuestNpc(799318).addOnTalkEvent(questId); //Batalrion.
		qe.registerQuestNpc(799010).addOnTalkEvent(questId); //Richelle.
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		final Player player = env.getPlayer();
		int targetId = 0;
		if (env.getVisibleObject() instanceof Npc)
			targetId = ((Npc) env.getVisibleObject()).getNpcId();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
        if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 799444) { //Kimin.
				if (env.getDialog() == QuestDialog.START_DIALOG) {
					return sendQuestDialog(env, 1011);
				} else if (env.getDialogId() == 1002) {
					if (giveQuestItem(env, 182213039, 1)) //Rejeton De Padmarashka.
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
				case 799318: { //Batalrion.
					switch (env.getDialog()) {
						case START_DIALOG: {
							return sendQuestDialog(env, 1352);
						} case STEP_TO_1: {
							qs.setQuestVarById(0, qs.getQuestVarById(0) + 1);
							updateQuestStatus(env);
					        return closeDialogWindow(env);
						}
					}
				} case 799225: { //Richelle.
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
			if (targetId == 799225) { //Richelle.
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
