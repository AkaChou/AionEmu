package com.aionemu.gameserver.quest.handlers.evergale_canyon;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.QuestService;

/**
 * 永风峡谷任务脚本：Main Mission To Retrieve The Core Fragment（任务 ID 23961）。
 * Evergale Canyon quest script: Main Mission To Retrieve The Core Fragment (quest ID 23961).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _23961Main_Mission_To_Retrieve_The_Core_Fragment extends QuestHandler {

    private final static int questId = 23961;
	private final static int[] npcs = {835222, 835224};
    public _23961Main_Mission_To_Retrieve_The_Core_Fragment() {
        super(questId);
    }
	
	@Override
	public void register() {
		for (int npc: npcs) {
            qe.registerQuestNpc(npc).addOnTalkEvent(questId);
        }
		qe.registerOnEnterWorld(questId);
	}
	
	@Override
    public boolean onDialogEvent(QuestEnv env) {
        final Player player = env.getPlayer();
        final QuestState qs = player.getQuestStateList().getQuestState(questId);
		int targetId = env.getTargetId();
        if (qs == null || qs.getStatus() == QuestStatus.START) {
			if (targetId == 835222) {
				switch (env.getDialog()) {
					case START_DIALOG: {
							return sendQuestDialog(env, 1011);
					} case SELECT_ACTION_1012: {
							playQuestMovie(env, 963);
							return sendQuestDialog(env, 1012);
					} case SELECT_ACTION_1013: {
							return sendQuestDialog(env, 1013);
					} case SET_REWARD: {
						qs.setStatus(QuestStatus.REWARD);
						updateQuestStatus(env);
						return closeDialogWindow(env);
					}
				}
			}
		} 
        else if (qs == null || qs.getStatus() == QuestStatus.REWARD) {
            if (targetId == 835224) {
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
    public boolean onEnterWorldEvent(QuestEnv env) {
        final Player player = env.getPlayer();
        final QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (player.getWorldId() == 302350000) { //Windy Gorge 5.5
            if (qs == null) {
                env.setQuestId(questId);
                if (QuestService.startQuest(env)) {
					return true;
				}
            }
        }
        return false;
    }
}
