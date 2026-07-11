package com.aionemu.gameserver.quest.handlers.udas_temple;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.world.zone.ZoneName;

/**
 * 乌达斯神殿任务脚本：Arachnophobia（任务 ID 30011）。
 * Udas Temple quest script: Arachnophobia (quest ID 30011).
 *
 * @author Majka Ajural
 */
public class _30011Arachnophobia extends QuestHandler {

	private final static int questId = 30011;
	public _30011Arachnophobia() {
		super(questId);
	}
	
    @Override
	public void register() {
		qe.registerQuestNpc(799031).addOnQuestStart(questId);
		qe.registerQuestNpc(799031).addOnTalkEvent(questId);
		qe.registerQuestNpc(215792).addOnKillEvent(questId);
		qe.registerOnEnterZone(ZoneName.get("SENSORYAREA_Q30011_300160000"), questId);
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		final Player player = env.getPlayer();
		int targetId = 0;
		if (env.getVisibleObject() instanceof Npc) {
			targetId = ((Npc) env.getVisibleObject()).getNpcId();
		}
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		QuestDialog dialog = env.getDialog();
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
		    if (targetId == 799031) { // Steurios
				switch (dialog) {
					case START_DIALOG:
						return sendQuestDialog(env, 1011);
				    case ASK_ACCEPTION: {
					    return sendQuestDialog(env, 4);
				    }
				    case ACCEPT_QUEST: {
					    return sendQuestStartDialog(env);
				    }
				    case REFUSE_QUEST: {
					   return closeDialogWindow(env);
                    }
				}
			}
		}
        if (qs == null || qs.getStatus() == QuestStatus.REWARD) {
		    if (targetId == 799031) // Steurios
			return sendQuestEndDialog(env);
		}
		return false;  
	}

	@Override
    public boolean onEnterZoneEvent(QuestEnv env, ZoneName zoneName) {
        Player player = env.getPlayer();
        QuestState qs = player.getQuestStateList().getQuestState(questId);
        if (qs != null && qs.getStatus() == QuestStatus.START) {
            int var = qs.getQuestVarById(0);
            if (zoneName.equals(ZoneName.get("SENSORYAREA_Q30011_300160000"))) {
                if (var == 0) {
                    changeQuestStep(env, 0, 1, false);
                    return true;
                }
            }
        }
        return false;
	}

	@Override
	public boolean onKillEvent(QuestEnv env) {
		return defaultOnKillEvent(env, 215792, 1, true);
	}
}
