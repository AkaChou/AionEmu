package com.aionemu.gameserver.quest.handlers.noble_siel_supreme;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.QuestService;

/**
 * 高贵希尔至尊任务脚本：Improved Mace（任务 ID 30343）。
 * Noble Siel Supreme quest script: Improved Mace (quest ID 30343).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _30343Improved_Mace extends QuestHandler
{
	private final static int questId = 30343;
	
	public _30343Improved_Mace() {
		super(questId);
	}
	
	@Override
	public void register() {
		int[] debilkarims = {215795}; //Debilkarim The Maker.
		qe.registerQuestNpc(799336).addOnQuestStart(questId); //Tataka.
		qe.registerQuestNpc(799336).addOnTalkEvent(questId); //Tataka.
		qe.registerGetingItem(182209741, questId);
		for (int debilkarim: debilkarims) {
			qe.registerQuestNpc(debilkarim).addOnKillEvent(questId);
		}
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		QuestDialog dialog = env.getDialog();
		int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 799336) { //Tataka.
				if (dialog == QuestDialog.START_DIALOG) {
					return sendQuestDialog(env, 4762);
				} else {
					return sendQuestStartDialog(env);
				}
			}
		} else if (qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 799336) { //Tataka.
				if (dialog == QuestDialog.USE_OBJECT) {
					if (player.getInventory().getItemCountByItemId(182209741) > 0) {
						return sendQuestDialog(env, 10002);
					}
				} else {
					removeQuestItem(env, 182209741, 1);
					return sendQuestEndDialog(env);
				}
			}
		}
		return false;
	}
	
	@Override
	public boolean onKillEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		int targetId = env.getTargetId();
		if (qs != null && qs.getStatus() == QuestStatus.START) {
			switch (targetId) {
				case 215795: { //Debilkarim The Maker.
					if (QuestService.collectItemCheck(env, true)) {
						return giveQuestItem(env, 182209741, 1);
					}
				}
			}
		}
		return false;
	}
	
	@Override
	public boolean onGetItemEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs != null && qs.getStatus() == QuestStatus.START) {
			changeQuestStep(env, 0, 0, true);
			return true;
		}
		return false;
	}
}
