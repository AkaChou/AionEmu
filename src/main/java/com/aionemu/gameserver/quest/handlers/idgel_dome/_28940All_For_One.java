package com.aionemu.gameserver.quest.handlers.idgel_dome;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 伊德格尔穹顶任务脚本：All For One（任务 ID 28940）。
 * Idgel Dome quest script: All For One (quest ID 28940).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _28940All_For_One extends QuestHandler
{
	public static final int questId = 28940;
	
	public _28940All_For_One() {
		super(questId);
	}
	
	@Override
	public void register() {
		qe.registerQuestNpc(802433).addOnQuestStart(questId); //Feroz.
		qe.registerQuestNpc(802384).addOnTalkEvent(questId); //Salade.
		qe.registerQuestNpc(802384).addOnTalkEvent(questId); //Salade.
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		QuestDialog dialog = env.getDialog();
		int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 802433) { //Salade.
				switch (dialog) {
					case START_DIALOG: {
						return sendQuestDialog(env, 1011);
					}
					case ACCEPT_QUEST:
					case ACCEPT_QUEST_SIMPLE:
						return sendQuestStartDialog(env);
				}
			}
		} else if (qs.getStatus() == QuestStatus.START) {
			switch (targetId) {
				case 802384: { //Salade.
					switch (dialog) {
						case START_DIALOG: {
							return sendQuestDialog(env, 2375);
						} case SELECT_REWARD: {
							changeQuestStep(env, 0, 0, true);
							return sendQuestEndDialog(env);
						}
					}
				}
			}
		} else if (qs.getStatus() == QuestStatus.REWARD) {
		    if (targetId == 802384) { //Salade.
			    return sendQuestEndDialog(env);
		    }
		}
		return false;
	}
}
