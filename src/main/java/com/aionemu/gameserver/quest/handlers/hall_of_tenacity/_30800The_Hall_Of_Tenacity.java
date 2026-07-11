package com.aionemu.gameserver.quest.handlers.hall_of_tenacity;

import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.HandlerResult;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 坚韧大厅任务脚本：The Hall Of Tenacity（任务 ID 30800）。
 * Hall of Tenacity quest script: The Hall Of Tenacity (quest ID 30800).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _30800The_Hall_Of_Tenacity extends QuestHandler {

	private final static int questId = 30800;
	
	public _30800The_Hall_Of_Tenacity() {
		super(questId);
	}
	
	public void register() {
		qe.registerQuestItem(182216169, questId); //What do you know about the Arena of Tenacity ?
		qe.registerQuestNpc(834987).addOnQuestStart(questId); //Peronerk.
		qe.registerQuestNpc(834987).addOnTalkEvent(questId); //Peronerk.
	}
	
	@Override
	public HandlerResult onItemUseEvent(QuestEnv env, Item item) {
		final Player player = env.getPlayer();
        final QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs != null && qs.getStatus() == QuestStatus.START) {
			if (qs.getQuestVarById(0) == 0) {
				qs.setQuestVar(1);
				changeQuestStep(env, 1, 1, true);
				return HandlerResult.SUCCESS;
			}
		}
		return HandlerResult.FAILED;
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		QuestDialog dialog = env.getDialog();
		int targetId = env.getTargetId();
		final Player player = env.getPlayer();
		final QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 834987) { //Peronerk.
				if (dialog == QuestDialog.START_DIALOG) {
					return sendQuestDialog(env, 4762);
				} else {
					return sendQuestStartDialog(env, 182216169, 1);
				}
			}
		} else if (qs.getStatus() == QuestStatus.REWARD) {
            if (targetId == 834987) { //Peronerk.
                if (env.getDialog() == QuestDialog.START_DIALOG) {
                    return sendQuestDialog(env, 10002);
				} else if (env.getDialog() == QuestDialog.SELECT_REWARD) {
					return sendQuestDialog(env, 5);
				} else {
					// 你对孤独竞技场了解多少？ / What do you know about the Arena of Tenacity ?
					removeQuestItem(env, 182216169, 1);
					return sendQuestEndDialog(env);
				}
			}
		}
		return false;
	}
}
