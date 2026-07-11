package com.aionemu.gameserver.quest.handlers.theobomos;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.utils.MathUtil;

/**
 * 西奥博莫斯任务脚本：Cursed Zirius（任务 ID 3057）。
 * Theobomos quest script: Cursed Zirius (quest ID 3057).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _3057Cursed_Zirius extends QuestHandler {

	private final static int questId = 3057;
	public _3057Cursed_Zirius() {
		super(questId);
	}
	
	public void register() {
		qe.registerQuestNpc(798213).addOnQuestStart(questId);
		qe.registerQuestNpc(798213).addOnTalkEvent(questId);
		qe.registerQuestNpc(214576).addOnAttackEvent(questId);
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		final Player player = env.getPlayer();
		final QuestState qs = player.getQuestStateList().getQuestState(questId);
		QuestDialog dialog = env.getDialog();
		int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 798213) { 
				if (dialog == QuestDialog.START_DIALOG) {
					return sendQuestDialog(env, 4762);
				} else {
					return sendQuestStartDialog(env);
				}
			}
		} else if (qs == null || qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 798213) {
				switch (dialog) {
					case USE_OBJECT: {
						return sendQuestDialog(env, 10002);
					} default: {
						return sendQuestEndDialog(env);
					}
				}
			}
		}
		return false;
	}
	
	@Override
	public boolean onAttackEvent(QuestEnv env) {
		final Player player = env.getPlayer();
		final QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs != null && qs.getStatus() == QuestStatus.START) {
			int targetId = env.getTargetId();
			if (targetId == 214576) {
				if (MathUtil.getDistance(env.getVisibleObject(), 1691.41f, 219.09f, 72.62f) <= 30) {
					((Npc) env.getVisibleObject()).getController().onDie(player);
					changeQuestStep(env, 0, 1, true);
					return true;
				}
			}
		}
		return false;
	}
}
