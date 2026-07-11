package com.aionemu.gameserver.quest.handlers.aturam_sky_fortress;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.world.zone.ZoneName;

/**
 * 阿图拉姆天空要塞任务脚本：Floating Doom（任务 ID 28300）。
 * Aturam Sky Fortress quest script: Floating Doom (quest ID 28300).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _28300Floating_Doom  extends QuestHandler
{
	private final static int questId = 28300;
	
	public _28300Floating_Doom() {
		super(questId);
	}
	
	@Override
	public void register() {
		qe.registerQuestNpc(801904).addOnQuestStart(questId);
		qe.registerQuestNpc(801904).addOnTalkEvent(questId);
		qe.registerQuestNpc(804821).addOnTalkEvent(questId);
		qe.registerQuestNpc(799530).addOnTalkEvent(questId);
		qe.registerOnEnterZone(ZoneName.get("ATURAM_SKY_FORTRESS_2_300240000"), questId);
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 801904) {
				switch (env.getDialog()) {
					case START_DIALOG:
						return sendQuestDialog(env, 4762);
					default:
						return sendQuestStartDialog(env);
				}
			}
		} else if (qs.getStatus() == QuestStatus.START && qs.getQuestVarById(0) == 0) {
			if (targetId == 804821) {
				switch (env.getDialog()) {
					case START_DIALOG: {
                        changeQuestStep(env, 0, 1, false);
						return closeDialogWindow(env);
                    } case SELECT_ACTION_1013:
						return sendQuestDialog(env, 1013);
					case STEP_TO_1:
						changeQuestStep(env, 0, 1, false);
						return closeDialogWindow(env);
				}
			}
		} else if ((qs.getStatus() == QuestStatus.REWARD)) {
			if (targetId == 799530) {
				switch (env.getDialog()) {
					case USE_OBJECT:
						return sendQuestDialog(env, 10002);
					case SELECT_REWARD:
						return sendQuestDialog(env, 5);
					default:
						return sendQuestEndDialog(env);
				}
			}
		}
		return false;
	}
	
	@Override
	public boolean onEnterZoneEvent(QuestEnv env, ZoneName zoneName) {
		if (zoneName == ZoneName.get("ATURAM_SKY_FORTRESS_2_300240000")) {
			Player player = env.getPlayer();
			if (player == null) {
				return false;
			}	
			QuestState qs = player.getQuestStateList().getQuestState(questId);
			if (qs != null && qs.getStatus() == QuestStatus.START) {
				int var = qs.getQuestVarById(0);
				if (var == 1) {
					changeQuestStep(env, 1, 1, true);
					return true;
				}
			}
		}
		return false;
	}
}
