package com.aionemu.gameserver.quest.handlers.inggison;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 因吉森任务脚本：Maintaining The Illusion（任务 ID 11003）。
 * Inggison quest script: Maintaining The Illusion (quest ID 11003).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _11003Maintaining_The_Illusion extends QuestHandler {

	private final static int questId = 11003;
	public _11003Maintaining_The_Illusion() {
		super(questId);
	}
	
	@Override
	public void register() {
		qe.registerQuestNpc(798933).addOnQuestStart(questId); //Phailos.
		qe.registerQuestNpc(798942).addOnTalkEvent(questId); //Harknes.
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		final Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		int targetId = 0;
		if (env.getVisibleObject() instanceof Npc)
			targetId = ((Npc) env.getVisibleObject()).getNpcId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 798933) {
				if (env.getDialog() == QuestDialog.START_DIALOG)
					return sendQuestDialog(env, 1011);
				else
					return sendQuestStartDialog(env);
			}
		}
		else if (qs == null || qs.getStatus() == QuestStatus.START) {
			switch (targetId) {
				case 798942: {
					switch (env.getDialog()) {
						case START_DIALOG: {
							return sendQuestDialog(env, 2375);
                        }
						case CHECK_COLLECTED_ITEMS: {
							return checkQuestItems(env, 0, 1, true, 5, 2376); // 7
						}
					}
				}
			}
		}
		else if (qs == null || qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 798942) {
				if (env.getDialogId() == 39)
					return sendQuestDialog(env, 5);
				else
					return sendQuestEndDialog(env);
			}
		}
		return false;
	}
}
