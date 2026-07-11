package com.aionemu.gameserver.quest.handlers.raksang_ruins;

import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.HandlerResult;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 拉克桑遗迹任务脚本：Test Bomb Test Bomb（任务 ID 18738）。
 * Raksang Ruins quest script: Test Bomb Test Bomb (quest ID 18738).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _18738Test_Bomb_Test_Bomb extends QuestHandler {

    private final static int questId = 18738;
    public _18738Test_Bomb_Test_Bomb() {
        super(questId);
    }
	
	@Override
	public void register() {
		qe.registerQuestNpc(206378).addOnQuestStart(questId);
		qe.registerQuestNpc(206379).addOnQuestStart(questId);
		qe.registerQuestNpc(206380).addOnQuestStart(questId);
		qe.registerQuestNpc(804965).addOnTalkEvent(questId);
		qe.registerQuestItem(164000342, questId); //Improved Life Drain Bomb.
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 206378 || targetId == 206379 || targetId == 206380) {
				switch (env.getDialog()) {
					case START_DIALOG: {
						return sendQuestDialog(env, 4762);
					}
					case ACCEPT_QUEST:
					case ACCEPT_QUEST_SIMPLE:
					    giveQuestItem(env, 164000342, 10); //Improved Life Drain Bomb.
						return sendQuestStartDialog(env);
					case REFUSE_QUEST_SIMPLE:
				        return closeDialogWindow(env);
				}
			}
		}
		if (qs == null || qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 804965) {
				if (env.getDialog() == QuestDialog.USE_OBJECT) {
					return sendQuestDialog(env, 10002);
				}
				return sendQuestEndDialog(env);
			}
		}
		return false;
	}
	
	@Override
	public HandlerResult onItemUseEvent(final QuestEnv env, Item item) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs != null && qs.getStatus() == QuestStatus.START) {
			if (player.getWorldId() == 300610000) {
				int itemId = item.getItemId();
				int var = qs.getQuestVarById(0);
				int var1 = qs.getQuestVarById(1);
				if (itemId == 164000342) { //Improved Life Drain Bomb.
					if (var == 0) {
						if (var1 >= 0 && var1 < 9) {
							changeQuestStep(env, var1, var1 + 1, false, 1);
							return HandlerResult.SUCCESS;
						} else if (var1 == 9) {
                            qs.setQuestVarById(0, 1);
							qs.setStatus(QuestStatus.REWARD);
							updateQuestStatus(env);
							return HandlerResult.SUCCESS;
						}
					}
				}
			}
		}
		return HandlerResult.UNKNOWN;
	}
}
