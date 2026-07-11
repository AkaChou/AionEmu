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
 * 拉克桑遗迹任务脚本：Its A Dirty Job（任务 ID 28740）。
 * Raksang Ruins quest script: Its A Dirty Job (quest ID 28740).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _28740Its_A_Dirty_Job extends QuestHandler {

    private final static int questId = 28740;
    public _28740Its_A_Dirty_Job() {
        super(questId);
    }
	
	@Override
	public void register() {
		qe.registerQuestNpc(804732).addOnTalkEvent(questId);
		qe.registerOnEnterZone(ZoneName.get("BOMIRE_VILLAGE_220080000"), questId);
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.START) {
			if (targetId == 804732) {
				switch (env.getDialog()) {
					case START_DIALOG: {
						return sendQuestDialog(env, 1011);
				    } case CHECK_COLLECTED_ITEMS: {
                        return checkQuestItems(env, 0, 1, true, 5, 10001);
                    } 
				}
			}
		} else if (qs != null && qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 804732) {
				return sendQuestEndDialog(env);
			}
		}
		return false;
	}

	@Override
    public boolean onEnterZoneEvent(QuestEnv env, ZoneName zoneName) {
        Player player = env.getPlayer();
        QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (zoneName == ZoneName.get("BOMIRE_VILLAGE_220080000")) {
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
