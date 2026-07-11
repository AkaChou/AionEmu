package com.aionemu.gameserver.quest.handlers.mission;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.ai2.manager.EmoteManager;
import com.aionemu.gameserver.ai2.event.AIEventType;
import com.aionemu.gameserver.model.TeleportAnimation;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.QuestService;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.WorldMapType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 主线任务脚本：A Lone Defense（任务 ID 14026）。
 * Campaign mission quest script: A Lone Defense (quest ID 14026).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _14026A_Lone_Defense extends QuestHandler {

    private final static int questId = 14026;
    private static List<Integer> mobs = new ArrayList<Integer>();
    static {
        mobs.add(213576); //Draconute Scout.
		mobs.add(213577); //Chandala Mage.
        mobs.add(213578); //Chandala Scaleguard.
        mobs.add(213579); //Chandala Fangblade.
    }
	
    public _14026A_Lone_Defense() {
        super(questId);
    }
	
    @Override
    public void register() {
        qe.registerOnDie(questId);
        qe.registerOnLogOut(questId);
		qe.registerOnLevelUp(questId);
        qe.registerOnQuestTimerEnd(questId);
		qe.registerOnEnterZoneMissionEnd(questId);
        qe.registerQuestNpc(203901).addOnTalkEvent(questId); //Telemachus.
        qe.registerQuestNpc(204020).addOnTalkEvent(questId); //Mabangtah.
        qe.registerQuestNpc(204044).addOnTalkEvent(questId); //Kimeia.
        for (int mob: mobs) {
            qe.registerQuestNpc(mob).addOnKillEvent(questId);
        }
    }
	
	@Override
    public boolean onZoneMissionEndEvent(QuestEnv env) {
        int[] quests = {14020, 14021, 14022, 14023, 14024, 14025};
        return defaultOnZoneMissionEndEvent(env, quests);
    }
	
    @Override
    public boolean onLvlUpEvent(QuestEnv env) {
        int[] quests = {14020, 14021, 14022, 14023, 14024, 14025};
        return defaultOnLvlUpEvent(env, quests, false);
    }
	
    @Override
    public boolean onDialogEvent(QuestEnv env) {
        final Player player = env.getPlayer();
        QuestState qs = player.getQuestStateList().getQuestState(questId);
		int targetId = env.getTargetId();
        if (qs == null) {
            return false;
        } if (qs.getStatus() == QuestStatus.START) {
            switch (targetId) {
                case 203901: { //Telemachus.
                    switch (env.getDialog()) {
                        case START_DIALOG: {
                            if (qs.getQuestVarById(0) == 0) {
                                return sendQuestDialog(env, 1011);
                            }
                        } case STEP_TO_1: {
                            qs.setQuestVar(1);
                            updateQuestStatus(env);
                            TeleportService2.teleportTo(player, WorldMapType.ELTNEN.getId(), 1596.1948f, 1529.9152f, 317, (byte) 120, TeleportAnimation.BEAM_ANIMATION);
                            return closeDialogWindow(env);
                        }
                    }
                } case 204020: { //Mabangtah.
                    switch (env.getDialog()) {
                        case START_DIALOG: {
                            if (qs.getQuestVarById(0) == 1) {
                                return sendQuestDialog(env, 1352);
                            }
                        } case STEP_TO_2: {
                            qs.setQuestVar(2);
                            updateQuestStatus(env);
                            giveQuestItem(env, 182215324, 1);
                            TeleportService2.teleportTo(player, WorldMapType.ELTNEN.getId(), 2500.15f, 780.9f, 409, (byte) 15, TeleportAnimation.BEAM_ANIMATION);
                            return closeDialogWindow(env);
                        }
                    }
                } case 204044: { //Kimeia.
                    switch (env.getDialog()) {
                        case START_DIALOG: {
                            switch (qs.getQuestVarById(0)) {
                                case 2: {
                                    return sendQuestDialog(env, 1693);
                                } case 4: {
                                    return sendQuestDialog(env, 2034);
                                }
                            }
                        } case STEP_TO_3: {
                            qs.setQuestVar(3);
                            updateQuestStatus(env);
                            QuestService.questTimerStart(env, 120);
                            spawn(player);
                            return closeDialogWindow(env);
                        } case STEP_TO_4: {
                            removeQuestItem(env, 182201013, 1);
                            qs.setStatus(QuestStatus.REWARD);
                            updateQuestStatus(env);
                            TeleportService2.teleportTo(player, WorldMapType.ELTNEN.getId(), 271.69f, 2787.04f, 272.47f, (byte) 50, TeleportAnimation.BEAM_ANIMATION);
                            return closeDialogWindow(env);
                        }
                    }
                }
            }
        } else if (qs.getStatus() == QuestStatus.REWARD) {
            if (targetId == 203901) { //Telemachus.
                switch (env.getDialog()) {
                    case START_DIALOG: {
                        return sendQuestDialog(env, 2375);
                    } case SELECT_REWARD: {
                        return sendQuestDialog(env, 5);
                    } default:
                        return sendQuestEndDialog(env);
                }
            }
        }
        return false;
    }
	
    @Override
    public boolean onQuestTimerEndEvent(QuestEnv env) {
        Player player = env.getPlayer();
        QuestState qs = player.getQuestStateList().getQuestState(questId);
        if (qs != null && qs.getStatus() == QuestStatus.START) {
            int var = qs.getQuestVarById(0);
            if (var == 3) {
                qs.setQuestVar(4);
                updateQuestStatus(env);
                return true;
            }
        }
        return false;
    }
	
    @Override
    public boolean onDieEvent(QuestEnv env) {
        Player player = env.getPlayer();
        QuestState qs = player.getQuestStateList().getQuestState(questId);
        if (qs == null || qs.getStatus() != QuestStatus.START) {
            return false;
        } if (qs.getQuestVarById(0) == 3) {
            QuestService.questTimerEnd(env);
            qs.setQuestVar(2);
            updateQuestStatus(env);
            return true;
        }
        return false;
    }
	
    @Override
    public boolean onLogOutEvent(QuestEnv env) {
        Player player = env.getPlayer();
        QuestState qs = player.getQuestStateList().getQuestState(questId);
        if (qs != null && qs.getStatus() == QuestStatus.START) {
            int var = qs.getQuestVarById(0);
            if (var == 3) {
                qs.setQuestVar(2);
                updateQuestStatus(env);
                return true;
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
            if (var == 3) {
                int targetId = env.getTargetId();
                if (mobs.contains(targetId)) {
                    spawn(player);
                    return true;
                }
            }
        }
        return false;
    }
	
    private void spawn(Player player) {
    int mobToSpawn = mobs.get(Rnd.get(0, 2));
    float x = 0;
    float y = 0;
    final float z = 217.48f;
    switch (mobToSpawn) {
        case 213576: { //Draconute Scout.
            x = 254.74f;
            y = 236.72f;
            break;
        } case 213577: { //Chandala Mage.
            x = 257.92f;
            y = 237.39f;
            break;
        } case 213578: { //Chandala Scaleguard.
            x = 261.86f;
            y = 237.5f;
            break;
        } case 213579: { //Chandala Fangblade.
            x = 268.86f;
            y = 243.5f;
            break;
        }
    }
    
    Npc spawn = (Npc) QuestService.spawnQuestNpc(310040000, player.getInstanceId(), mobToSpawn, x, y, z, (byte) 95);
    Collection<Npc> allNpcs = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getNpcs();
    Npc target = null;
    for (Npc npc : allNpcs) {
        if (npc.getNpcId() == 204044) { //Kimeia.
            target = npc;
        }
    } 
    
    if (target != null) {
        spawn.setTarget(target);
        spawn.getMoveController().moveToTargetObject();
        spawn.getAggroList().addHate(target, 1000);
        EmoteManager.emoteStartAttacking(spawn);
        if (spawn.getAi2() != null) {
            spawn.getAi2().onGeneralEvent(AIEventType.ATTACK);
        }
    }
    }
}
