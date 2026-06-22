package quest.morheim;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/*
* DainAvenger
*/

public class _24242AlltheSpellsandWhistles extends QuestHandler {

	private static final int questId = 24242;
	public _24242AlltheSpellsandWhistles() {
		super(questId);
	}
	
	@Override
	public void register() {
		qe.registerQuestNpc(204372).addOnQuestStart(questId);
		qe.registerQuestNpc(204372).addOnTalkEvent(questId);
		qe.registerQuestNpc(204408).addOnTalkEvent(questId);
		qe.registerQuestNpc(804606).addOnTalkEvent(questId);
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 204372) {
				if (env.getDialog() == QuestDialog.START_DIALOG) {
					return sendQuestDialog(env, 1011);
				} else {
					return sendQuestStartDialog(env);
				}
			}
		}
        else if (qs != null && qs.getStatus() == QuestStatus.START) {
			if (targetId == 204408) {
				if (env.getDialog() == QuestDialog.START_DIALOG) {
					return sendQuestDialog(env, 1352);
				}
				else if (env.getDialog() == QuestDialog.STEP_TO_1) {
					qs.setQuestVarById(0, qs.getQuestVarById(0) + 1);
					updateQuestStatus(env);
					return closeDialogWindow(env);
				}
			}
			if (targetId == 804606) {
				if (env.getDialog() == QuestDialog.USE_OBJECT) {
                   if (qs.getQuestVarById(0) == 1) {
                        return sendQuestDialog(env, 1693);
                   } else if (qs.getQuestVarById(0) == 2 && player.getInventory().getItemCountByItemId(182215583) == 1) {
                        return sendQuestDialog(env, 2375);
                   }
                }
				else if (env.getDialog() == QuestDialog.STEP_TO_2) {
					qs.setQuestVarById(0, qs.getQuestVarById(0) + 1);
					updateQuestStatus(env);
					return closeDialogWindow(env);
				}
				else if (env.getDialog() == QuestDialog.CHECK_COLLECTED_ITEMS_SIMPLE) {
					qs.setQuestVarById(0, qs.getQuestVarById(0) + 1);
					qs.setStatus(QuestStatus.REWARD);
					updateQuestStatus(env);
					return sendQuestDialog(env, 5);
                } else {
					return closeDialogWindow(env);
				}
			}
		}
        else if (qs != null && qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 804606) {
				return sendQuestEndDialog(env);
			}
		}
		return false;
	}
}