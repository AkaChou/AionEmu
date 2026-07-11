package com.aionemu.gameserver.quest.handlers.brusthonin;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.QuestService;
import com.aionemu.gameserver.world.zone.ZoneName;

/**
 * 布鲁斯特霍宁任务脚本：Domination Of Spite（任务 ID 4122）。
 * Brusthonin quest script: Domination Of Spite (quest ID 4122).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _4122Domination_Of_Spite extends QuestHandler {

	private final static int questId = 4122;
	public _4122Domination_Of_Spite() {
		super(questId);
	}
	
	@Override
	public boolean onEnterZoneEvent(QuestEnv env, ZoneName zoneName) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
        if (qs == null || qs.getStatus() == QuestStatus.NONE) {
           QuestService.startQuest(env);
        }
		return defaultOnEnterZoneEvent(env, zoneName, ZoneName.get("ADMA_STRONGHOLD_INTERIOR_320130000"));
	}
	
	@Override
	public void register() {
		qe.registerQuestNpc(205225).addOnTalkEvent(questId);
		qe.registerQuestNpc(237245).addOnKillEvent(questId); //Suspicious Pot.
		qe.registerQuestNpc(237240).addOnKillEvent(questId); //Enthralled Gutorum.
		qe.registerQuestNpc(237241).addOnKillEvent(questId); //Enthralled Karemiwen.
		qe.registerQuestNpc(237243).addOnKillEvent(questId); //Enthralled Zeeturun.
		qe.registerQuestNpc(237244).addOnKillEvent(questId); //Enthralled Lannok.
		qe.registerQuestNpc(237239).addOnKillEvent(questId); //Death Reaper.
		qe.registerOnEnterZone(ZoneName.get("ADMA_STRONGHOLD_INTERIOR_320130000"), questId);
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		QuestDialog dialog = env.getDialog();
		int targetId = env.getTargetId();
		if (env.getVisibleObject() instanceof Npc) {
			targetId = ((Npc) env.getVisibleObject()).getNpcId();
		}
        if (qs == null || qs.getStatus() == QuestStatus.NONE) {
           return false;
        } 
        else if (qs != null && qs.getStatus() == QuestStatus.REWARD) {
		    if (targetId == 205225) {
			    switch (dialog) {
					case SELECT_REWARD: {
						return sendQuestDialog(env, 5);
					} default:
						return sendQuestEndDialog(env);
				}
		    }
		}
		return false;
	}
	
    @Override
    public boolean onKillEvent(QuestEnv env) {
    Player player = env.getPlayer();
    QuestState qs = player.getQuestStateList().getQuestState(questId);
    if (qs != null && qs.getStatus() == QuestStatus.START) {
        int var = qs.getQuestVarById(0);
        if (var == 0) { //Suspicious Pot.
            return defaultOnKillEvent(env, 237245, 0, 1);
        } else if(var == 1) { //Enthralled Gutorum.
            return defaultOnKillEvent(env, 237240, 1, 2);
        } else if(var == 2) { //Enthralled Karemiwen.
            return defaultOnKillEvent(env, 237241, 2, 3);
        } else if(var == 3) { //Enthralled Zeeturun.
            return defaultOnKillEvent(env, 237243, 3, 4);
        } else if(var == 4) { //Enthralled Lannok.
            return defaultOnKillEvent(env, 237244, 4, 5);
        } else if(var == 5) { //Death Reaper.
            if (env.getTargetId() == 237239) {
                qs.setQuestVarById(0, 6);
                qs.setStatus(QuestStatus.REWARD);
                updateQuestStatus(env);
                return true;
            }
        }
    }
    return false;
    }
}
