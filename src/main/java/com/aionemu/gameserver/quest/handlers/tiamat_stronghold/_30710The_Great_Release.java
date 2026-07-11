package com.aionemu.gameserver.quest.handlers.tiamat_stronghold;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.QuestService;

/**
 * 提亚马特要塞任务脚本：The Great Release（任务 ID 30710）。
 * Tiamat Stronghold quest script: The Great Release (quest ID 30710).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _30710The_Great_Release extends QuestHandler
{
	private final static int questId = 30710;
	private final static int npcs [] = {804870, 701498};
	
	public _30710The_Great_Release() {
		super(questId);
	}
	
	@Override
	public void register() {
		qe.registerQuestNpc(804870).addOnQuestStart(questId);
		for (int npc: npcs) {
			qe.registerQuestNpc(npc).addOnTalkEvent(questId);
		}
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		QuestDialog dialog = env.getDialog();
		int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 804870) {
				if (dialog == QuestDialog.START_DIALOG) {
					return sendQuestDialog(env, 4762);
				} else {
					return sendQuestStartDialog(env);
				}
			}
		} else if(qs.getStatus() == QuestStatus.START) {
			if (targetId == 701498) {
				QuestService.addNewSpawn(player.getWorldId(), player.getInstanceId(), 800457, player.getX(), player.getY(), player.getZ(), (byte) 0); //Gabelline.
				return useQuestObject(env, 0, 1, true, false);
			}
		} else if (qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 804870) {
				if (dialog == QuestDialog.USE_OBJECT) {
					return sendQuestDialog(env, 10002);
				} else {
					return sendQuestEndDialog(env);
				}
			}
		}
		return false;
	}
}
