package com.aionemu.gameserver.quest.handlers.norsvold;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.world.zone.ZoneName;

/**
 * 诺斯沃尔德任务脚本：Butterfly March（任务 ID 25553）。
 * Norsvold quest script: Butterfly March (quest ID 25553).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _25553Butterfly_March extends QuestHandler {

	private final static int questId = 25553;
	public _25553Butterfly_March() {
		super(questId);
	}
	
	public void register() {
		qe.registerQuestNpc(806101).addOnQuestStart(questId);
		qe.registerQuestNpc(806101).addOnTalkEvent(questId);
		qe.registerOnEnterZone(ZoneName.get("DF6_SENSORY_AREA_Q25553_A_TO_F_220110000"), questId);
		qe.registerOnEnterZone(ZoneName.get("DF6_SENSORY_AREA_Q25553_F_TO_A_220110000"), questId);
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		int targetId = env.getTargetId();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 806101) { 
				if (env.getDialog() == QuestDialog.START_DIALOG) {
					return sendQuestDialog(env, 4762);
				} else {
					return sendQuestStartDialog(env);
				}
			}
		} else if (qs == null || qs.getStatus() == QuestStatus.REWARD) {
            if (targetId == 806101) {
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
	
	@Override
    public boolean onEnterZoneEvent(QuestEnv env, ZoneName zoneName) {
        Player player = env.getPlayer();
        QuestState qs = player.getQuestStateList().getQuestState(questId);
        if (qs != null && qs.getStatus() == QuestStatus.START) {
            int var = qs.getQuestVarById(0);
			if (zoneName == ZoneName.get("DF6_SENSORY_AREA_Q25553_A_TO_F_220110000")) {
				if (var == 0) {
					changeQuestStep(env, 0, 1, false);
					return true;
				}
			} else if (zoneName == ZoneName.get("DF6_SENSORY_AREA_Q25553_F_TO_A_220110000")) {
				if (var == 1) {
					qs.setStatus(QuestStatus.REWARD);
					changeQuestStep(env, 1, 2, false);
					updateQuestStatus(env);
					return true;
				}
			}
		}
		return false;
	}
}
