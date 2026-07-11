package com.aionemu.gameserver.quest.handlers.gelkmaros;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 格尔克马洛斯任务脚本：Practical Research（任务 ID 21458）。
 * Gelkmaros quest script: Practical Research (quest ID 21458).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _21458Practical_Research extends QuestHandler {

	private final static int questId = 21458;
	private final static int[] npc_ids = {799249, 204052};
	public _21458Practical_Research() {
		super(questId);
	}
	
	@Override
	public void register() {
		qe.registerQuestNpc(799249).addOnQuestStart(questId); //Irkale.
		for (int npc_id : npc_ids)
		qe.registerQuestNpc(npc_id).addOnTalkEvent(questId);
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		final Player player = env.getPlayer();
		int targetId = 0;
		if (env.getVisibleObject() instanceof Npc)
			targetId = ((Npc) env.getVisibleObject()).getNpcId();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
		    if (targetId == 799249) { //Irkale.
				if (env.getDialog() == QuestDialog.START_DIALOG)
					return sendQuestDialog(env, 1011);
				else if (env.getDialogId() == 1007) {
					return sendQuestDialog(env, 4);
				}
				else if (env.getDialogId() == 1002) {
					return sendQuestStartDialog(env, 182209507, 1);
				}
			}
		} 
        if (qs == null)
			return false;
        else if (qs.getStatus() != QuestStatus.START) {
		int var = qs.getQuestVarById(0);
		 if (targetId == 204052) { //Vidar.
			switch (env.getDialog()) {
				case START_DIALOG:
				if (var == 0)
					return sendQuestDialog(env, 1352);
				case STEP_TO_1:
				if (var == 0) {
					removeQuestItem(env, 182209507, 1);
					qs.setQuestVarById(0, var + 1);
					qs.setStatus(QuestStatus.REWARD);
					updateQuestStatus(env);
                    return closeDialogWindow(env);
				}
				return false;
                }
			}
		}
		else if (qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 799249) { //Irkale.
				if (env.getDialog() == QuestDialog.USE_OBJECT)
					return sendQuestDialog(env, 2375);
				else if (env.getDialogId() == 1009)
					return sendQuestDialog(env, 5);
				else
					return sendQuestEndDialog(env);
			}
		}
		return false;
	}
}
