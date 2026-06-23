/*
 * =====================================================================================*
 * This file is part of Aion-Unique (Aion-Unique Home Software Development)             *
 * Aion-Unique Development is a closed Aion Project that use Old Aion Project Base      *
 * Like Aion-Lightning, Aion-Engine, Aion-Core, Aion-Extreme, Aion-NextGen, ArchSoft,   *
 * Aion-Ger, U3J, Encom And Other Aion project, All Credit Content                      *
 * That they make is belong to them/Copyright is belong to them. And All new Content    *
 * that Aion-Unique make the copyright is belong to Aion-Unique                         *
 * You may have agreement with Aion-Unique Development, before use this Engine/Source   *
 * You have agree with all of Term of Services agreement with Aion-Unique Development   *
 * =====================================================================================*
 */
package quest.eltnen;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.world.zone.ZoneName;

/****/
/** Author Ghostfur & Unknown (Aion-Unique)
/****/
public class _1336Scouting_For_Demokritos extends QuestHandler {

    private final static int questId = 1336;
    
    private static final int ZONE_1_MASK = 16;
    private static final int ZONE_2_MASK = 32;
    private static final int ZONE_3_MASK = 64;
    private static final int ALL_ZONES_MASK = 112;

    public _1336Scouting_For_Demokritos() {
        super(questId);
    }
    
    @Override
    public void register() {
        qe.registerQuestNpc(204006).addOnQuestStart(questId); //Demokritos.
        qe.registerQuestNpc(204006).addOnTalkEvent(questId); //Demokritos.
        qe.registerOnEnterZone(ZoneName.get("LF2_SENSORY_AREA_Q1336_1_210020000"), questId);
        qe.registerOnEnterZone(ZoneName.get("LF2_SENSORY_AREA_Q1336_2_210020000"), questId);
        qe.registerOnEnterZone(ZoneName.get("LF2_SENSORY_AREA_Q1336_3_210020000"), questId);
        qe.registerOnMovieEndQuest(43, questId);
        qe.registerOnMovieEndQuest(44, questId);
        qe.registerOnMovieEndQuest(45, questId);
    }
    
    @Override
    public boolean onDialogEvent(QuestEnv env) {
        Player player = env.getPlayer();
		int targetId = env.getTargetId();
        QuestState qs = player.getQuestStateList().getQuestState(questId);
        if (qs == null || qs.getStatus() == QuestStatus.NONE) {
            if (targetId == 204006) { //Demokritos.
                if (env.getDialog() == QuestDialog.START_DIALOG) {
                    return sendQuestDialog(env, 1011);
                }  
                if (env.getDialog() == QuestDialog.SELECT_ACTION_1012) {
                    return sendQuestDialog(env, 1012);
                }
                if (env.getDialog() == QuestDialog.ASK_ACCEPTION) {
                    return sendQuestDialog(env, 4);
                }
                if (env.getDialog() == QuestDialog.ACCEPT_QUEST) {
                    return sendQuestStartDialog(env);
                }
                if (env.getDialog() == QuestDialog.REFUSE_QUEST) {
                    return closeDialogWindow(env);
                }
            }
        } 
        else if (qs.getStatus() == QuestStatus.START) {
            if (targetId == 204006) { //Demokritos.
                if (env.getDialog() == QuestDialog.START_DIALOG) {
                    return sendQuestDialog(env, 1352);
                } 
                if (env.getDialog() == QuestDialog.SELECT_REWARD) {
                    qs.setStatus(QuestStatus.REWARD);
                    updateQuestStatus(env);
                    return sendQuestEndDialog(env); 
                }
            }
        }
        else if (qs.getStatus() == QuestStatus.REWARD) {
            if (targetId == 204006) { //Demokritos.
                return sendQuestEndDialog(env);
            }
        }
        return false;
    }

    @Override
    public boolean onEnterZoneEvent(QuestEnv env, ZoneName zoneName) {
        Player player = env.getPlayer();
        QuestState qs = player.getQuestStateList().getQuestState(questId);
        if (qs != null && qs.getStatus() == QuestStatus.START) {
            int currentMask = qs.getQuestVarById(0);
            int movieId = 0;
            
            if (zoneName == ZoneName.get("LF2_SENSORY_AREA_Q1336_1_210020000")) {
                if ((currentMask & ZONE_1_MASK) == 0) {
                    movieId = 43;
                }
            } else if (zoneName == ZoneName.get("LF2_SENSORY_AREA_Q1336_2_210020000")) {
                if ((currentMask & ZONE_2_MASK) == 0) {
                    movieId = 44;
                }
            } else if (zoneName == ZoneName.get("LF2_SENSORY_AREA_Q1336_3_210020000")) {
                if ((currentMask & ZONE_3_MASK) == 0) {
                    movieId = 45;
                }
            }
            
            if (movieId != 0) {
                playQuestMovie(env, movieId);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onMovieEndEvent(QuestEnv env, int movieId) {
        Player player = env.getPlayer();
        QuestState qs = player.getQuestStateList().getQuestState(questId);
        if (qs != null && qs.getStatus() == QuestStatus.START) {
            int currentMask = qs.getQuestVarById(0);
            int newMask = currentMask;
            
            if (movieId == 43) {
                newMask = currentMask | ZONE_1_MASK;
            } else if (movieId == 44) {
                newMask = currentMask | ZONE_2_MASK;
            } else if (movieId == 45) {
                newMask = currentMask | ZONE_3_MASK;
            }
            
            qs.setQuestVarById(0, newMask);
            updateQuestStatus(env);
            
            if (newMask == ALL_ZONES_MASK) {
			    changeQuestStep(env, 0, 1, false);
			    updateQuestStatus(env);
            }
            return true;
        }
        return false;
    }
}