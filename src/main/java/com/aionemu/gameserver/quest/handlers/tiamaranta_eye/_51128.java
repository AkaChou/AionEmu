package com.aionemu.gameserver.quest.handlers.tiamaranta_eye;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.world.zone.ZoneName;

/**
 * 提亚玛兰塔之眼任务脚本（任务 ID 51128）。
 * Tiamaranta Eye quest script (quest ID 51128).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _51128 extends QuestHandler {

    private final static int questId = 51128;
    public _51128() {
        super(questId);
    }
	
	@Override
	public void register() {
		qe.registerQuestNpc(836454).addOnQuestStart(questId); //DC1_Verbenk_E.
		qe.registerQuestNpc(205960).addOnTalkEvent(questId);  //Grimron.
		qe.registerOnEnterZone(ZoneName.get("ELYOS_BREAKOUT_600040000"), questId);
	}
	
	@Override
    public boolean onDialogEvent(QuestEnv env) {
        final Player player = env.getPlayer();
        int targetId = env.getTargetId();
        final QuestState qs = player.getQuestStateList().getQuestState(questId);
        if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 836454) { //DC1_Verbenk_E.
				if (env.getDialog() == QuestDialog.START_DIALOG) {
					return sendQuestDialog(env, 4762);
				} else {
					return sendQuestStartDialog(env);
				}
			}
		} else if (qs == null || qs.getStatus() == QuestStatus.REWARD) {
            if (targetId == 205960) {  //Grimron.
                if (env.getDialog() == QuestDialog.START_DIALOG) {
                    return sendQuestDialog(env, 10002);
                } else {
                    return sendQuestEndDialog(env);
                }
            }
        }
		return false;
	}
	
	@Override
    public boolean onEnterZoneEvent(QuestEnv env, ZoneName zoneName) {
        final Player player = env.getPlayer();
        final QuestState qs = player.getQuestStateList().getQuestState(questId);
        if (qs != null && qs.getStatus() == QuestStatus.START) {
            int var = qs.getQuestVarById(0);
			if (zoneName == ZoneName.get("ELYOS_BREAKOUT_600040000")) {
				if (var == 0) {
					changeQuestStep(env, 0, 1, true);
					return true;
				}
			}
		}
		return false;
	}
}
