package com.aionemu.gameserver.quest.handlers.kaldor;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 卡尔多尔任务脚本：Tree Is Company（任务 ID 13809）。
 * Kaldor quest script: Tree Is Company (quest ID 13809).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _13809Tree_Is_Company extends QuestHandler
{
	private final static int questId = 13809;
	
	public _13809Tree_Is_Company() {
		super(questId);
	}
	
	@Override
	public void register() {
		qe.registerQuestNpc(802427).addOnQuestStart(questId); //Caetess.
        qe.registerQuestNpc(802427).addOnTalkEvent(questId); //Caetess.
        qe.registerQuestNpc(730969).addOnTalkEvent(questId); //Scorched Tree.
        qe.registerQuestNpc(730970).addOnTalkEvent(questId); //Cindery Tree.
		qe.registerQuestNpc(730971).addOnTalkEvent(questId); //Burnt Tree.
	}
	
	@Override
	public boolean onDialogEvent(final QuestEnv env) {
		Player player = env.getPlayer();
		int targetId = env.getTargetId();
		QuestDialog dialog = env.getDialog();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 802427) { //Caetess.
				if (dialog == QuestDialog.START_DIALOG) {
					return sendQuestDialog(env, 1011);
				} else {
					return sendQuestStartDialog(env);
				}
			}
		} else if (qs != null && qs.getStatus() == QuestStatus.START) {
			int var = qs.getQuestVarById(0);
			switch (targetId) {
				case 730969: { //Scorched Tree.
                    switch (env.getDialog()) {
						case USE_OBJECT: {
                            return useQuestObject(env, 0, 1, false, 0);
                        }
                    }
                    break;
                } case 730970: { //Cindery Tree.
                    switch (env.getDialog()) {
                        case USE_OBJECT: {
                            return useQuestObject(env, 1, 2, false, 0);
                        }
                    }
                    break;
                } case 730971: { //Burnt Tree.
                    switch (env.getDialog()) {
                        case USE_OBJECT: {
                            return useQuestObject(env, 2, 3, false, 0);
                        }
                    }
                    break;
                } case 802427: { //Caetess.
				    switch (dialog) {
					    case START_DIALOG:
						    return sendQuestDialog(env, 2375);
					    case SELECT_REWARD:
						    changeQuestStep(env, 3, 4, true);
						    return sendQuestDialog(env, 5);
					}
				}
			}
		} else if (qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 802427) { //Caetess.
				return sendQuestEndDialog(env);
			}
		}
		return false;
	}
}
