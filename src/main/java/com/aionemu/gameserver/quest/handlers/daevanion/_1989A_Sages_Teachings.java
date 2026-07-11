package com.aionemu.gameserver.quest.handlers.daevanion;

import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 大天使之路任务脚本：A Sages Teachings（任务 ID 1989）。
 * Daevanion quest script: A Sages Teachings (quest ID 1989).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _1989A_Sages_Teachings extends QuestHandler {
	
	private final static int questId = 1989;
	public _1989A_Sages_Teachings() {
		super(questId);
	}
	
	@Override
	public void register() {
		qe.registerQuestNpc(203704).addOnQuestStart(questId);
		qe.registerQuestNpc(203705).addOnQuestStart(questId);
		qe.registerQuestNpc(203706).addOnQuestStart(questId);
		qe.registerQuestNpc(203707).addOnQuestStart(questId);
		qe.registerQuestNpc(801214).addOnQuestStart(questId);
		qe.registerQuestNpc(801215).addOnQuestStart(questId);
		qe.registerQuestNpc(203771).addOnQuestStart(questId);
		qe.registerQuestNpc(203771).addOnTalkEvent(questId);
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		final Player player = env.getPlayer();
		int targetId = 0;
		if (env.getVisibleObject() instanceof Npc)
			targetId = ((Npc) env.getVisibleObject()).getNpcId();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 203771) {
				if (env.getDialog() == QuestDialog.START_DIALOG)
					return sendQuestDialog(env, 1011);
				else
					return sendQuestStartDialog(env);
			}
		}
		if (qs == null)
			return false;
		int var = qs.getQuestVarById(0);
		if (qs.getStatus() == QuestStatus.START) {
			PlayerClass playerClass = player.getCommonData().getPlayerClass();
			switch (targetId) {
				case 203704:
					switch (env.getDialog()) {
						case START_DIALOG:
							if (playerClass == PlayerClass.GLADIATOR ||
							    playerClass == PlayerClass.TEMPLAR)
								return sendQuestDialog(env, 1352);
							else
								return sendQuestDialog(env, 1438);
						case STEP_TO_1:
                        return defaultCloseDialog(env, 0, 1);
					}
				case 203705:
					switch (env.getDialog()) {
						case START_DIALOG:
							if (playerClass == PlayerClass.ASSASSIN ||
							    playerClass == PlayerClass.RANGER)
								return sendQuestDialog(env, 1693);
							else
								return sendQuestDialog(env, 1779);
						case STEP_TO_1:
                        return defaultCloseDialog(env, 0, 1);
					}
				case 203706:
					switch (env.getDialog()) {
						case START_DIALOG:
							if (playerClass == PlayerClass.SORCERER ||
							    playerClass == PlayerClass.SPIRIT_MASTER)
								return sendQuestDialog(env, 2034);
							else
								return sendQuestDialog(env, 2120);
						case STEP_TO_1:
                        return defaultCloseDialog(env, 0, 1);
					}
				case 203707:
					switch (env.getDialog()) {
						case START_DIALOG:
							if (playerClass == PlayerClass.CLERIC ||
							    playerClass == PlayerClass.CHANTER)
								return sendQuestDialog(env, 2375);
							else
								return sendQuestDialog(env, 2461);
						case STEP_TO_1:
                        return defaultCloseDialog(env, 0, 1);
					}
				case 801214:
					switch (env.getDialog()) {
						case START_DIALOG:
							if (playerClass == PlayerClass.GUNSLINGER ||
								playerClass == PlayerClass.AETHERTECH)
								return sendQuestDialog(env, 2548);
							else
								return sendQuestDialog(env, 2568);
						case STEP_TO_1:
                        return defaultCloseDialog(env, 0, 1);
					}
				case 801215:
					switch (env.getDialog()) {
						case START_DIALOG:
							if (playerClass == PlayerClass.SONGWEAVER)
								return sendQuestDialog(env, 2633);
							else
								return sendQuestDialog(env, 2653);
						case STEP_TO_1:
                        return defaultCloseDialog(env, 0, 1);
					}
				case 203771:
					switch (env.getDialog()) {
						case START_DIALOG:
							if (var == 1)
								return sendQuestDialog(env, 2716);
							else if (var == 2)
								return sendQuestDialog(env, 3057);
							else if (var == 3) {
								if (player.getCommonData().getDp() < 4000)
									return sendQuestDialog(env, 3484);
								else
									return sendQuestDialog(env, 3398);
							} else if (var == 4) {
								if (player.getCommonData().getDp() < 4000)
									return sendQuestDialog(env, 3825);
								else
									return sendQuestDialog(env, 3739);
							}
						case SELECT_REWARD:
							if (var == 3) {
								playQuestMovie(env, 105);
								player.getCommonData().setDp(0);
								qs.setStatus(QuestStatus.REWARD);
								updateQuestStatus(env);
								return sendQuestDialog(env, 5);
							} else if (var == 4) {
								playQuestMovie(env, 105);
								player.getCommonData().setDp(0);
								qs.setStatus(QuestStatus.REWARD);
								updateQuestStatus(env);
								return sendQuestDialog(env, 5);
							} else
								return this.sendQuestEndDialog(env);
						case STEP_TO_2:
							qs.setQuestVarById(0, var + 1);
							updateQuestStatus(env);
							return sendQuestDialog(env, 3057);
						case STEP_TO_4:
							qs.setQuestVarById(0, 3);
							updateQuestStatus(env);
					        return closeDialogWindow(env);
						case STEP_TO_5:
							qs.setQuestVarById(0, 4);
							updateQuestStatus(env);
					        return closeDialogWindow(env);
					}
			}
		} else if (qs == null || qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 203771)
				return sendQuestEndDialog(env);
		}
		return false;
	}
}
