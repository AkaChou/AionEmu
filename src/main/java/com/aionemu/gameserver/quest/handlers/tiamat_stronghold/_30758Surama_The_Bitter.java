package com.aionemu.gameserver.quest.handlers.tiamat_stronghold;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 提亚马特要塞任务脚本：Surama The Bitter（任务 ID 30758）。
 * Tiamat Stronghold quest script: Surama The Bitter (quest ID 30758).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _30758Surama_The_Bitter extends QuestHandler
{
	private final static int questId = 30758;
	private final static int npcs [] = {800369, 800438};
	
	public _30758Surama_The_Bitter() {
		super(questId);
	}
	
	@Override
	public void register() {
		qe.registerQuestNpc(800369).addOnQuestStart(questId);
		for (int npc : npcs) {
			qe.registerQuestNpc(npc).addOnTalkEvent(questId);
		}
		qe.registerQuestNpc(219356).addOnKillEvent(questId);
	}
	
	@Override
	public boolean onKillEvent(QuestEnv env) {
	    return defaultOnKillEvent(env, 219356, 5, true);
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		QuestDialog dialog = env.getDialog();
		int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 800369) { 
				if (dialog == QuestDialog.START_DIALOG) {
					return sendQuestDialog(env, 4762);
				} else {
					return sendQuestStartDialog(env);
				}
			}
		} else if (qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 800438) { 
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
