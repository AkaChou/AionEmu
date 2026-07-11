package com.aionemu.gameserver.quest.handlers.noble_siel_supreme;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.QuestService;

/**
 * 高贵希尔至尊任务脚本：New Gun（任务 ID 30247）。
 * Noble Siel Supreme quest script: New Gun (quest ID 30247).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _30247New_Gun extends QuestHandler
{
	private final static int questId = 30247;
	
	public _30247New_Gun() {
		super(questId);
	}
	
	@Override
	public void register() {
		int[] debilkarims = {215795}; //Debilkarim The Maker.
		qe.registerQuestNpc(799032).addOnQuestStart(questId); //Gefeios.
		qe.registerQuestNpc(799032).addOnTalkEvent(questId); //Gefeios.
		qe.registerGetingItem(182213285, questId);
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
			if (targetId == 799032) { //Gefeios.
				if (dialog == QuestDialog.START_DIALOG) {
					return sendQuestDialog(env, 4762);
				} else {
					return sendQuestStartDialog(env);
				}
			}
		} else if (qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 799032) { //Gefeios.
				if (dialog == QuestDialog.USE_OBJECT) {
					if (player.getInventory().getItemCountByItemId(182213285) > 0) {
						return sendQuestDialog(env, 10002);
					}
				} else {
					removeQuestItem(env, 182213285, 1);
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
						return giveQuestItem(env, 182213285, 1);
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
