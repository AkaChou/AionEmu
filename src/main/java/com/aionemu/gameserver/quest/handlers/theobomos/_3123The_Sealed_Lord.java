package com.aionemu.gameserver.quest.handlers.theobomos;

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
 * 西奥博莫斯任务脚本：The Sealed Lord（任务 ID 3123）。
 * Theobomos quest script: The Sealed Lord (quest ID 3123).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _3123The_Sealed_Lord extends QuestHandler {

	private final static int questId = 3123;
	public _3123The_Sealed_Lord() {
		super(questId);
	}
	
	@Override
	public boolean onEnterZoneEvent(QuestEnv env, ZoneName zoneName) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
        if (qs == null || qs.getStatus() == QuestStatus.NONE) {
           QuestService.startQuest(env);
        } 
		return defaultOnEnterZoneEvent(env, zoneName, ZoneName.get("THEOBOMOS_LAB_INTERIOR_310110000"));
	}
	
	@Override
	public void register() {
		qe.registerQuestNpc(237258).addOnTalkEvent(questId);
		qe.registerQuestNpc(237253).addOnKillEvent(questId); //Fiery Sealing Stone.
		qe.registerQuestNpc(237246).addOnKillEvent(questId); //Watcher Queen Arachne.
		qe.registerQuestNpc(237248).addOnKillEvent(questId); //Watcher Silikor Of Memory.
		qe.registerQuestNpc(237249).addOnKillEvent(questId); //Watcher Jilitia.
		qe.registerQuestNpc(237250).addOnKillEvent(questId); //Sealed Unstable Triroan.
		qe.registerQuestNpc(237251).addOnKillEvent(questId); //Corrupted Ifrit.
		qe.registerOnEnterZone(ZoneName.get("THEOBOMOS_LAB_INTERIOR_310110000"), questId);
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
		    if (targetId == 237258) {
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
        if (var == 0) { //Fiery Sealing Stone.
            return defaultOnKillEvent(env, 237253, 0, 1);
        } else if(var == 1) { //Watcher Queen Arachne.
            return defaultOnKillEvent(env, 237246, 1, 2);
        } else if(var == 2) { //Watcher Silikor Of Memory.
            return defaultOnKillEvent(env, 237248, 2, 3);
        } else if(var == 3) { //Watcher Jilitia.
            return defaultOnKillEvent(env, 237249, 3, 4);
        } else if(var == 4) { //Sealed Unstable Triroan.
            return defaultOnKillEvent(env, 237250, 4, 5);
        } else if(var == 5) { //Corrupted Ifrit.
            if (env.getTargetId() == 237251) {
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
