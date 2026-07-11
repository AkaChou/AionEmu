package com.aionemu.gameserver.quest.handlers.ascension;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.teleport.TeleportService2;

/**
 * 飞升任务脚本：Dispatch To Altgard Assassin Ranger（任务 ID 2902）。
 * Ascension quest script: Dispatch To Altgard Assassin Ranger (quest ID 2902).
 *
 * @author (Encom)
 */
public class _2902Dispatch_To_Altgard_Assassin_Ranger extends QuestHandler {

	private final static int questId = 2902;
	public _2902Dispatch_To_Altgard_Assassin_Ranger() {
		super(questId);
	}
	
	@Override
    public boolean onLvlUpEvent(QuestEnv env) {
        return defaultOnLvlUpEvent(env, 2009, true);
    }
	
	@Override
	public void register() {
		qe.registerOnLevelUp(questId);
		qe.registerQuestNpc(204191).addOnTalkEvent(questId);
		qe.registerQuestNpc(203559).addOnTalkEvent(questId);
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		final Player player = env.getPlayer();
		final QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs == null) {
			return false;
		}
		int var = qs.getQuestVarById(0);
		int targetId = 0;
		if (env.getVisibleObject() instanceof Npc) {
			targetId = ((Npc) env.getVisibleObject()).getNpcId();
		}
		if (qs.getStatus() == QuestStatus.START) {
			switch (targetId) {
				case 204191: {
					switch (env.getDialog()) {
						case START_DIALOG:
						if (var == 0) {
							return sendQuestDialog(env, 1352);
						}
						break;
						case STEP_TO_1:
						if (var == 0) {
							qs.setQuestVarById(0, var + 1);
							updateQuestStatus(env);
							TeleportService2.teleportTo(player, 220030000, player.getInstanceId(), 1748f, 1807f, 255f);
						    return closeDialogWindow(env);
						}
					}
				} case 203559:
				switch (env.getDialog()) {
					case START_DIALOG:
					if (var == 1) {
						return sendQuestDialog(env, 2375);
					}
					case SELECT_REWARD:
					if (var == 1) {
						qs.setStatus(QuestStatus.REWARD);
						updateQuestStatus(env);
				        return sendQuestEndDialog(env);
					}
				}
			}
		} else if (qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 203559) {
				return sendQuestEndDialog(env);
			}
		}
		return false;
	}
}
