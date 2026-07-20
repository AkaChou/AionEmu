package com.aionemu.gameserver.quest.handlers.inggison;

import com.aionemu.gameserver.model.TeleportAnimation;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.teleport.TeleportService2;

/**
 * 因吉森任务脚本：Proof Of Talent（任务 ID 11076）。
 * Inggison quest script: Proof Of Talent (quest ID 11076).
 *
 * @author Cheatkiller
 */
public class _11076ProofOfTalent extends QuestHandler {

	private final static int questId = 11076;

	public _11076ProofOfTalent() {
		super(questId);
	}

	public void register() {
		qe.registerQuestNpc(799025).addOnQuestStart(questId);
		qe.registerQuestNpc(799084).addOnTalkEvent(questId);
		qe.registerQuestNpc(799025).addOnTalkEvent(questId);
		qe.registerOnEnterWindStream(questId);
	}

	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		QuestDialog dialog = env.getDialog();
		int targetId = env.getTargetId();
		
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 799025) { 
				if (dialog == QuestDialog.START_DIALOG) {
					return sendQuestDialog(env, 4762);
				}
				else {
					return sendQuestStartDialog(env);
				}
			}
		}
		else if (qs.getStatus() == QuestStatus.START) {
			if (targetId == 799084) {
				if (dialog == QuestDialog.START_DIALOG) {
					if(qs.getQuestVarById(0) == 3)
						return sendQuestDialog(env, 2034);
				}
				else if (dialog == QuestDialog.STEP_TO_4) {
					TeleportService2.teleportTo(player, 210050000, 1338.6f, 279.6f, 590, (byte) 80, TeleportAnimation.BEAM_ANIMATION);
					return defaultCloseDialog(env, 3, 4, true, false);
				}
			}
		}
		else if (qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 799025) {
				if (dialog == QuestDialog.USE_OBJECT) {
					return sendQuestDialog(env, 10002);
				}
				return sendQuestEndDialog(env);
			}
		}
		return false;
	}
	
	@Override
	public boolean onEnterWindStreamEvent(QuestEnv env, int teleportId) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs != null && qs.getStatus() == QuestStatus.START) {
			if(player.getWorldId() == 210050000) {
				if(teleportId == 405001)
					changeQuestStep(env, 0, 1, false);
				else if(teleportId == 406001)
					changeQuestStep(env, 1, 2, false);
				else if(teleportId == 407001)
					changeQuestStep(env, 2, 3, false);
				return true;
			}
		}
		return false;
	}
}
	
		
