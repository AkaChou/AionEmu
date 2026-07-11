package com.aionemu.gameserver.quest.handlers.archives_of_eternity;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.world.zone.ZoneName;

/**
 * 永恒档案馆任务脚本：A Call For Champions（任务 ID 26800）。
 * Archives of Eternity quest script: A Call For Champions (quest ID 26800).
 *
 * @author (Encom)
 */
public class _26800A_Call_For_Champions extends QuestHandler {

    private final static int questId = 26800;
    public _26800A_Call_For_Champions() {
        super(questId);
    }
	
	@Override
	public void register() {
		qe.registerQuestNpc(806079).addOnQuestStart(questId); //Feregan.
        qe.registerQuestNpc(806079).addOnTalkEvent(questId); //Feregan.
		qe.registerQuestNpc(806233).addOnTalkEvent(questId); //Enfitenta.
		qe.registerQuestNpc(806149).addOnTalkEvent(questId); //Feregan.
		qe.registerOnEnterZone(ZoneName.get("IDETERNITY_01_Q16800_301540000"), questId);
		qe.registerOnEnterZone(ZoneName.get("DF_TOWER_SENSORY_AREA_Q26800_220120000"), questId);
	}
	
	@Override
    public boolean onDialogEvent(QuestEnv env) {
        Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		int targetId = env.getTargetId();
        if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 806079) { //Feregan.
				switch (env.getDialog()) {
                    case START_DIALOG: {
                        return sendQuestDialog(env, 4762);
					} case SELECT_ACTION_4763: {
						return sendQuestDialog(env, 4763);
					} case ACCEPT_QUEST:
					case ACCEPT_QUEST_SIMPLE: {
						return sendQuestStartDialog(env);
					} case REFUSE_QUEST_SIMPLE: {
				        return closeDialogWindow(env);
					} case STEP_TO_1: {
						return closeDialogWindow(env);
					}
                }
			}
		} else if (qs.getStatus() == QuestStatus.START) {
            int var = qs.getQuestVarById(0);  
		    if (targetId == 806233) { //Enfitenta.
				switch (env.getDialog()) {
					case START_DIALOG: {
						if (var == 1) {
							return sendQuestDialog(env, 1352);
						}
					} case SELECT_ACTION_1353: {
						if (var == 1) {
							return sendQuestDialog(env, 1353);
						}
					} case SET_REWARD: {
						changeQuestStep(env, 1, 2, false);
						return closeDialogWindow(env);
					}
				}
			}
		} else if (qs == null || qs.getStatus() == QuestStatus.REWARD) {
            if (targetId == 806149) { //Feregan.
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
        Player player = env.getPlayer();
        QuestState qs = player.getQuestStateList().getQuestState(questId);
        if (qs != null && qs.getStatus() == QuestStatus.START) {
            int var = qs.getQuestVarById(0);
			if (zoneName == ZoneName.get("DF_TOWER_SENSORY_AREA_Q26800_220120000")) {
				if (var == 0) {
					changeQuestStep(env, 0, 1, false);
					return true;
				}
			} else if (zoneName == ZoneName.get("IDETERNITY_01_Q16800_301540000")) {
				if (var == 2) {
					playQuestMovie(env, 932);
					changeQuestStep(env, 2, 3, true);
					return true;
				}
			}
		}
		return false;
	}
}
