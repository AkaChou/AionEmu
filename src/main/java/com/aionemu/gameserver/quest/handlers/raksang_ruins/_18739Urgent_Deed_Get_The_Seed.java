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
 * 拉克桑遗迹任务脚本：Urgent Deed Get The Seed（任务 ID 18739）。
 * Raksang Ruins quest script: Urgent Deed Get The Seed (quest ID 18739).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _18739Urgent_Deed_Get_The_Seed extends QuestHandler {

    private final static int questId = 18739;
    public _18739Urgent_Deed_Get_The_Seed() {
        super(questId);
    }
	
	@Override
	public void register() {
		qe.registerQuestNpc(804707).addOnTalkEvent(questId);
	    qe.registerOnEnterZone(ZoneName.get("ERIVALE_TERRITORY_VILLAGE_210070000"), questId);
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		QuestDialog dialog = env.getDialog();
		int targetId = env.getTargetId();
		if (qs.getStatus() == QuestStatus.START) {
			if (targetId == 804707) {
				switch (dialog) {
					case START_DIALOG: {
						return sendQuestDialog(env, 1011);
				    } case CHECK_COLLECTED_ITEMS: {
                        return checkQuestItems(env, 0, 1, true, 5, 10001);
                    } 
				}
			}
		} else if (qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 804707) {
				return sendQuestEndDialog(env);
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
}
