package com.aionemu.gameserver.quest.handlers.raksang_ruins;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.QuestService;
import com.aionemu.gameserver.world.zone.ZoneName;

/**
 * 拉克桑遗迹任务脚本：A Versed Order（任务 ID 18741）。
 * Raksang Ruins quest script: A Versed Order (quest ID 18741).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _18741A_Versed_Order extends QuestHandler{

    private final static int questId = 18741;
	private final static int[] embercrackStepsDrillCorps = {236018, 236019, 236020, 236021, 236096, 236097, 236098};
    public _18741A_Versed_Order() {
        super(questId);
    }
	
	@Override
	public void register() {
		qe.registerQuestNpc(804707).addOnTalkEvent(questId);
		for (int mob: embercrackStepsDrillCorps) {
			qe.registerQuestNpc(mob).addOnKillEvent(questId);
		}
		qe.registerOnEnterZone(ZoneName.get("ERIVALE_TERRITORY_VILLAGE_210070000"), questId);
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		int targetId = env.getTargetId();
        if (qs == null || qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 804707) {
				if (env.getDialogId() == 1352) {
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
		if (zoneName == ZoneName.get("ERIVALE_TERRITORY_VILLAGE_210070000")) {
			if (qs == null || qs.canRepeat()) {
				env.setQuestId(questId);
				if (QuestService.startQuest(env)) {
					return true;
				}
			}
		}
		return false;
	}
	
	public boolean onKillEvent(QuestEnv env) {
        Player player = env.getPlayer();
        QuestState qs = player.getQuestStateList().getQuestState(questId);
        if (qs != null && qs.getStatus() == QuestStatus.START) {
            switch (env.getTargetId()) {
                case 236018:
				case 236019:
				case 236020:
				case 236021:
				case 236096:
				case 236097:
				case 236098:
                if (qs.getQuestVarById(1) < 50) {
					qs.setQuestVarById(1, qs.getQuestVarById(1) + 1);
					updateQuestStatus(env);
				} if (qs.getQuestVarById(1) >= 50) {
                    qs.setQuestVarById(0, 1);
					qs.setStatus(QuestStatus.REWARD);
					updateQuestStatus(env);
				}
            }
        }
        return false;
    }
}
