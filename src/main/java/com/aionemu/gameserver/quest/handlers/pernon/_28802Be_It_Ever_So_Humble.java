package com.aionemu.gameserver.quest.handlers.pernon;

import com.aionemu.gameserver.lifecycle.GameHousingServices;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 佩尔农任务脚本：Be It Ever So Humble（任务 ID 28802）。
 * Pernon quest script: Be It Ever So Humble (quest ID 28802).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _28802Be_It_Ever_So_Humble extends QuestHandler
{
	private static final int questId = 28802;
	
	public _28802Be_It_Ever_So_Humble() {
		super(questId);
	}
	
	@Override
	public void register() {
		qe.registerQuestNpc(830102).addOnQuestStart(questId);
		qe.registerQuestNpc(830102).addOnTalkEvent(questId);
		qe.registerQuestNpc(830153).addOnTalkEvent(questId);
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		QuestDialog dialog = env.getDialog();
		int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 830102) {
				switch (dialog) {
					case START_DIALOG: {
						return sendQuestDialog(env, 1011);
					}
					case ACCEPT_QUEST:
					case ACCEPT_QUEST_SIMPLE:
						return sendQuestStartDialog(env);
				}
			}
		} else if (qs.getStatus() == QuestStatus.START) {
			switch (targetId) {
				case 830153: {
					switch (dialog) {
						case START_DIALOG: {
							return sendQuestDialog(env, 2375);
						} case SELECT_REWARD: {
							changeQuestStep(env, 0, 0, true);
							return sendQuestEndDialog(env);
						}
					}
				}
			}
		} else if (qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 830153) {
				if (dialog.equals(QuestDialog.SELECT_NO_REWARD)) {
					GameHousingServices.housingService().registerPlayerStudio(player);
				}
				return sendQuestEndDialog(env);
			}
		}
		return false;
	}
}
