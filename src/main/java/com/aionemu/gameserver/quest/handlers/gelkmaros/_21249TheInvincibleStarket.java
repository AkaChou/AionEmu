package com.aionemu.gameserver.quest.handlers.gelkmaros;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.QuestService;

/**
 * 格尔克马洛斯任务脚本：The Invincible Starket（任务 ID 21249）。
 * Gelkmaros quest script: The Invincible Starket (quest ID 21249).
 *
 * @author Cheatkiller
 */
public class _21249TheInvincibleStarket extends QuestHandler {

	private final static int questId = 21249;
	public _21249TheInvincibleStarket() {
		super(questId);
	}

	public void register() {
		qe.registerQuestNpc(799416).addOnQuestStart(questId);
		qe.registerQuestNpc(799529).addOnTalkEvent(questId);
		qe.registerQuestNpc(799417).addOnTalkEvent(questId);
	}

	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		QuestDialog dialog = env.getDialog();
		int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 799416) { 
				if (dialog == QuestDialog.START_DIALOG) {
					return sendQuestDialog(env, 4762);
				}
				else
					return sendQuestStartDialog(env);
			}
		}
		else if (qs.getStatus() == QuestStatus.START) {
			if (targetId == 799416) {
				if (dialog == QuestDialog.START_DIALOG) {
					if(qs.getQuestVarById(0) == 0)
						return sendQuestDialog(env, 1011);
				}
				else if (dialog == QuestDialog.STEP_TO_1) {
					Npc npc = (Npc) env.getVisibleObject();
					npc.getController().scheduleRespawn();
					npc.getController().onDelete();
					QuestService.addNewSpawn(player.getWorldId(), player.getInstanceId(), 799529, player.getX(), player.getY(), player.getZ(), (byte) 0);
					return defaultCloseDialog(env, 0, 1);
				}
			}
			else if (targetId == 799529) {
				if (dialog == QuestDialog.START_DIALOG) {
					if(qs.getQuestVarById(0) == 1)
						return sendQuestDialog(env, 1352);
				}
				else if (dialog == QuestDialog.SET_REWARD) {
					qs.setQuestVar(2);
					Npc npc = (Npc) env.getVisibleObject();
					npc.getController().onDelete();
					return defaultCloseDialog(env, 2, 2, true, false);
				}
			}
		}
		else if (qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 799417) {
				if (dialog == QuestDialog.USE_OBJECT) {
					return sendQuestDialog(env, 10002);
				}
				return sendQuestEndDialog(env);
			}
		}
		return false;
	}
}
