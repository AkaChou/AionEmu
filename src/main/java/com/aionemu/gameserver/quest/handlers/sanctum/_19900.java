package com.aionemu.gameserver.quest.handlers.sanctum;

import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.QuestService;

/**
 * 圣所任务脚本（任务 ID 19900）。
 * Sanctum quest script (quest ID 19900).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _19900 extends QuestHandler {

	private final static int questId = 19900;
	public _19900() {
		super(questId);
	}
	
	@Override
	public void register() {
        qe.registerQuestNpc(836073).addOnTalkEvent(questId);
		qe.registerOnLevelUp(questId);
		qe.registerOnEnterWorld(questId);
	}
	
	public boolean onLvlUpEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (player.getLevel() >= 10 && (qs == null || qs.getStatus() == QuestStatus.NONE) && player.getRace() == Race.ELYOS) {
			giveQuestItem(env, 190080010, 1);
			return QuestService.startQuest(env);
		}
		return false;
	}

	@Override
	public boolean onEnterWorldEvent(QuestEnv env) { // Fix for player who already have this Quest
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs != null && qs.getStatus() == QuestStatus.START && player.getInventory().getItemCountByItemId(190080010) < 1 && player.getWorldId() == 110010000) {
			return giveQuestItem(env, 190080010, 1);
		}
		return false;
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		final Player player = env.getPlayer();
        final QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs == null || qs.getStatus() == QuestStatus.REWARD) {
            if (env.getTargetId() == 836073) {
                if (env.getDialog() == QuestDialog.START_DIALOG) {
                    return sendQuestDialog(env, 10002);
				} else if (env.getDialog() == QuestDialog.SELECT_REWARD) {
					return sendQuestDialog(env, 5);
				} else {
					return sendQuestEndDialog(env);
				}
			}
		}
		return false;
	}
}
