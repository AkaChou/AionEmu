package com.aionemu.gameserver.quest.handlers.altgard;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 奥特加德任务脚本：Solid Proof（任务 ID 2279）。
 * Altgard quest script: Solid Proof (quest ID 2279).
 *
 * @author Ritsu
 */
public class _2279SolidProof extends QuestHandler {

	private final static int questId = 2279;
	public _2279SolidProof() {
		super(questId);
	}

	@Override
	public void register() {
		qe.registerQuestNpc(203557).addOnQuestStart(questId); //Suthran
		qe.registerQuestNpc(203557).addOnTalkEvent(questId);
		qe.registerQuestNpc(203590).addOnTalkEvent(questId); //Emgata
		qe.registerQuestNpc(203682).addOnTalkEvent(questId); //Zemurru's Spirit
	}

	@Override
	public boolean onDialogEvent(QuestEnv env) {
		final Player player = env.getPlayer();
		int targetId = 0;
		if (env.getVisibleObject() instanceof Npc) {
			targetId = ((Npc) env.getVisibleObject()).getNpcId();
		}
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs == null || qs.getStatus() == QuestStatus.NONE){
			if (targetId == 203557) {//Suthran
				if (env.getDialog() == QuestDialog.START_DIALOG) {
					return sendQuestDialog(env, 1011);
				}
				else {
					return sendQuestStartDialog(env);
				}
			}
		}
		else if (qs.getStatus() == QuestStatus.START){
			int var = qs.getQuestVarById(0);
			if (targetId == 203590) {//Emgata
				switch (env.getDialog()){
				case START_DIALOG:
					if(var == 0) {
						return sendQuestDialog(env, 1352);
					}
				case STEP_TO_1:
					if (var == 0) {
						qs.setQuestVarById(0, qs.getQuestVarById(0) + 1);
						updateQuestStatus(env);
					    return closeDialogWindow(env);
					}
				}
			}
			if (targetId == 203682){//Zemurru's Spirit
				switch (env.getDialog()) {
				case START_DIALOG:
					if (var == 1) {
						return sendQuestDialog(env, 1693);
					}
				case STEP_TO_2:
					if (var == 1) {
						qs.setQuestVarById(0, qs.getQuestVarById(0) + 1);
						updateQuestStatus(env);
					    return closeDialogWindow(env);
					}
				}
			}
			if (targetId == 203557){//Suthran
				switch (env.getDialog()){
				case START_DIALOG:
					if (var == 2) {
						return sendQuestDialog(env, 2375);
					}
				    case SELECT_REWARD:
					qs.setStatus(QuestStatus.REWARD);
					updateQuestStatus(env);
				    return sendQuestEndDialog(env);
				}
			}
		}
		else if (qs != null && qs.getStatus() == QuestStatus.REWARD){
			if (targetId == 203557){//Suthran
				return sendQuestEndDialog(env);
			}
		}
		return false;
	}
}
