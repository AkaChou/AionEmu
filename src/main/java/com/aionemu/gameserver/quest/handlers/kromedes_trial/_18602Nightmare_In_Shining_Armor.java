package com.aionemu.gameserver.quest.handlers.kromedes_trial;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.lifecycle.GameEngineServices;
import com.aionemu.gameserver.model.TeleportAnimation;
import com.aionemu.gameserver.services.QuestService;
import com.aionemu.gameserver.services.teleport.TeleportService2;

/**
 * 克罗米德试炼任务脚本：Nightmare In Shining Armor（任务 ID 18602）。
 * Kromedes Trial quest script: Nightmare In Shining Armor (quest ID 18602).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _18602Nightmare_In_Shining_Armor extends QuestHandler {

	private final static int questId = 18602;
	private final static int[] npc_ids = {205229, 730308, 700939};
	
	public _18602Nightmare_In_Shining_Armor() {
		super(questId);
	}
	
	@Override
	public void register() {
		qe.registerOnDie(questId);
		qe.registerOnEnterWorld(questId);
		qe.registerOnMovieEndQuest(454, questId);
		for (int npc_id : npc_ids)
			qe.registerQuestNpc(npc_id).addOnTalkEvent(questId);
		qe.registerQuestNpc(205229).addOnQuestStart(questId);
		qe.registerQuestNpc(217005).addOnKillEvent(questId);
	}
	
	@Override
	public boolean onEnterWorldEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs != null && qs.getStatus() == QuestStatus.START) {
			if (player.getWorldId() != 300230000) {
				int var = qs.getQuestVarById(0);
				if (var > 0) {
					changeQuestStep(env, var, 0, false);
					return true;
				}
			}
		}
		return false;
	}
	
	@Override
	public boolean onDieEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs != null && qs.getStatus() == QuestStatus.START) {
			int var = qs.getQuestVarById(0);
			if (var > 0) {
				changeQuestStep(env, var, 0, false);
				return true;
			}
		}
		return false;
	}
	
	@Override
	public boolean onKillEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs == null || qs.getStatus() != QuestStatus.START)
			return false;
		int targetId = 0;
		if (env.getVisibleObject() instanceof Npc)
			targetId = ((Npc) env.getVisibleObject()).getNpcId();
		if (targetId == 217005 && qs.getQuestVarById(0) == 3) {
			playQuestMovie(env, 455);
			return defaultOnKillEvent(env, targetId, 3, true);
		}
		return false;
	}
	
	@Override
	public boolean onMovieEndEvent(QuestEnv env, int movieId) {
		if (movieId != 454)
			return false;
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs == null || qs.getStatus() != QuestStatus.START || qs.getQuestVarById(0) != 2)
			return false;
		TeleportService2.teleportTo(player, 300230000, player.getInstanceId(), 687.631104f, 675.972412f,
			201.040802f, (byte) 90, TeleportAnimation.NO_ANIMATION);
		return true;
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		int targetId = env.getTargetId();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs == null || qs.getStatus() == QuestStatus.NONE || qs.canRepeat()) {
			if (targetId == 205229) {
				if (env.getDialog() == QuestDialog.START_DIALOG) {
					return sendQuestDialog(env, 4762);
				} else {
					return sendQuestStartDialog(env);
				}
			}
		} else if (qs.getStatus() == QuestStatus.START) {
			int var = qs.getQuestVarById(0);
			if (targetId == 205229) {
				if (env.getDialog() == QuestDialog.START_DIALOG) {
					return sendQuestDialog(env, 1011);
				} else if (env.getDialog() == QuestDialog.STEP_TO_1) {
					if (!TeleportService2.teleportToInstance(player, 300230000, 244.98566f, 244.14162f,
							189.52058f, (byte) 30)) {
						return false;
					}
					changeQuestStep(env, 0, 1, false);
					return closeDialogWindow(env);
				}
			} else if (targetId == 730308) {
				if (env.getDialog() == QuestDialog.USE_OBJECT) {
					if (var == 1) {
						return sendQuestDialog(env, 1693);
					}
				} else if (env.getDialog() == QuestDialog.STEP_TO_2 && var == 1) {
					if (!checkItemExistence(env, 185000109, 1, true)) {
						return sendQuestDialog(env, 10001);
					}
					closeDialogWindow(env);
					changeQuestStep(env, 1, 2, false);
					playQuestMovie(env, 454);
					QuestService.addNewSpawn(300230000, player.getInstanceId(), 282089, 653f, 774f, 216f, (byte) 0);
					return true;
				}
			} else if (targetId == 700939) {
				if (env.getDialog() == QuestDialog.USE_OBJECT) {
					if (var == 2) {
						return sendQuestDialog(env, 1693);
					}
				} else if (env.getDialog() == QuestDialog.STEP_TO_3) {
					GameEngineServices.skillEngine().getSkill(player, 19288, 1, player).useNoAnimationSkill();
					return defaultCloseDialog(env, 2, 3);
				}
			}
		} else if (qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 205229) {
				return sendQuestEndDialog(env);
			}
		}
		return false;
	}
}
