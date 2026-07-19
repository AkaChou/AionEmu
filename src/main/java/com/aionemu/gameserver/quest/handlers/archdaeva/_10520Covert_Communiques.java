package com.aionemu.gameserver.quest.handlers.archdaeva;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.QuestEngine;
import com.aionemu.gameserver.questEngine.handlers.HandlerResult;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.QuestService;
import com.aionemu.gameserver.services.instance.InstanceService;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.utils.ThreadPoolManager;
import com.aionemu.gameserver.world.WorldMapInstance;
import com.aionemu.gameserver.world.zone.ZoneName;

/**
 * 大天使任务脚本：Covert Communiques（任务 ID 10520）。
 * Archdaeva quest script: Covert Communiques (quest ID 10520). Source KOR: https://www.youtube.com/watch?v=8Qt-ZODwhoA
 *
 * @author Encom
 */
public class _10520Covert_Communiques extends QuestHandler {

	public static final int questId = 10520;
	private final static int[] npcs = {203726, 203752, 806073, 806076};
	public _10520Covert_Communiques() {
		super(questId);
	}
	
    @Override
    public void register() {
        for (int npc: npcs) {
            qe.registerQuestNpc(npc).addOnTalkEvent(questId);
        }
		qe.registerOnEnterWorld(questId);
		qe.registerQuestItem(182215953, questId); //Orders To Report To Iluma.
		qe.registerQuestItem(182215973, questId); //Sealed Letter From Pernos.
		qe.registerOnEnterZone(ZoneName.get("LF6_SENSORY_AREA_Q10520_210100000"), questId);
		qe.registerOnMovieEndQuest(995, questId);
    }
	
	@Override
    public boolean onEnterWorldEvent(QuestEnv env) {
        Player player = env.getPlayer();
        if (player.getWorldId() == 110010000) { //Sanctum.
            QuestState qs = player.getQuestStateList().getQuestState(questId);
            if (qs == null) {
                env.setQuestId(questId);
                if (QuestService.startQuest(env)) {
                    return true;
                }
            }
        }
        return false;
    }
	
    @Override
	public boolean onDialogEvent(QuestEnv env) {
		final Player player = env.getPlayer();
        final QuestState qs = player.getQuestStateList().getQuestState(questId);
        if (qs == null) {
            return false;
        }
        int var = qs.getQuestVarById(0);
        int targetId = 0;
        if (env.getVisibleObject() instanceof Npc) {
            targetId = ((Npc) env.getVisibleObject()).getNpcId();
        } if (qs.getStatus() == QuestStatus.START) {
			if (targetId == 203752) { //Jucleas.
				switch (env.getDialog()) {
					case START_DIALOG: {
						if (var == 0) {
							return sendQuestDialog(env, 1011);
						}
					} case SELECT_ACTION_1012: {
						if (var == 0) {
							return sendQuestDialog(env, 1012);
						}
					} case STEP_TO_1: {
						// 来自佩尔诺斯的封印信件。 / Sealed Letter From Pernos.
						giveQuestItem(env, 182215973, 1);
						changeQuestStep(env, 0, 1, false);
						return closeDialogWindow(env);
					}
				}
			} if (targetId == 806073) { //Felen.
				switch (env.getDialog()) {
					case START_DIALOG: {
						if (var == 2) {
							return sendQuestDialog(env, 1693);
						}
					} case SELECT_ACTION_1694: {
						if (var == 2) {
							return sendQuestDialog(env, 1694);
						}
					} case STEP_TO_3: {
						// 前往伊卢玛报到的命令。 / Orders To Report To Iluma.
						giveQuestItem(env, 182215953, 1);
						changeQuestStep(env, 2, 3, false);
						return closeDialogWindow(env);
					}
				}
			} if (targetId == 203726) { //Polyidus.
				switch (env.getDialog()) {
					case START_DIALOG: {
						if (var == 3) {
							return sendQuestDialog(env, 2034);
						}
					} case SELECT_ACTION_2035: {
						if (var == 3) {
							return sendQuestDialog(env, 2035);
						}
					} case STEP_TO_4: {
						TeleportService2.teleportTo(env.getPlayer(), 210100000, 1456.6283f, 1299.3306f, 336.49023f, (byte) 8);
						// 前往伊卢玛报到的命令。 / Orders To Report To Iluma.
						removeQuestItem(env, 182215953, 1);
						changeQuestStep(env, 3, 4, false);
						return closeDialogWindow(env);
					}
				}
			} if (targetId == 806076) { //Weatha.
				switch (env.getDialog()) {
					case START_DIALOG: {
						if (var == 5) {
							return sendQuestDialog(env, 2716);
						}
					} case SELECT_ACTION_2717: {
						if (var == 5) {
							return sendQuestDialog(env, 2717);
						}
					} case SET_REWARD: {
						qs.setStatus(QuestStatus.REWARD);
						updateQuestStatus(env);
						return closeDialogWindow(env);
					}
				}
			}
		} else if (qs.getStatus() == QuestStatus.REWARD) {
            if (targetId == 806076) { //Weatha.
                if (env.getDialog() == QuestDialog.USE_OBJECT) {
                    return sendQuestDialog(env, 10002);
                } else {
                    int[] ilumaMission = {10521, 10522, 10523, 10524, 10525, 10526, 10527, 10528, 10529, 10530};
                    for (int quest: ilumaMission) {
                        GameEngineServices.questEngine().onEnterZoneMissionEnd(new QuestEnv(env.getVisibleObject(), env.getPlayer(), quest, env.getDialogId()));
                    }
                    return sendQuestEndDialog(env);
                }
            }
        }
		return false;
	}
	
	@Override
    public boolean onEnterZoneEvent(QuestEnv env, ZoneName zoneName) {
        final Player player = env.getPlayer();
        final QuestState qs = player.getQuestStateList().getQuestState(questId);
        if (qs != null && qs.getStatus() == QuestStatus.START) {
            int var = qs.getQuestVarById(0);
			int instanceId = player.getInstanceId();
			if (zoneName == ZoneName.get("LF6_SENSORY_AREA_Q10520_210100000")) {
				if (var == 4) {
					playQuestMovie(env, 995);
					return true;
				}
			}
		}
		return false;
	}
	
	@Override
	public boolean onMovieEndEvent(QuestEnv env, int movieId) {
		Player player = env.getPlayer();
		if (movieId == 995) {
			if (!TeleportService2.teleportToInstance(player, 301580000, 431, 491, 99)) {
				return false;
			}
			changeQuestStep(env, 4, 5, false);
			return true;
		}
		return false;
	}
	
	@Override
    public HandlerResult onItemUseEvent(final QuestEnv env, Item item) {
		final Player player = env.getPlayer();
        final QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs == null) {
			return HandlerResult.UNKNOWN;
		} if (qs.getStatus() == QuestStatus.START) {
			int var = qs.getQuestVarById(0);
			// 来自佩尔诺斯的封印信件。 / Sealed Letter From Pernos.
			if (item.getItemTemplate().getTemplateId() == 182215973) {
				if (var == 1) {
					qs.setQuestVar(2);
					updateQuestStatus(env);
					// 来自佩尔诺斯的封印信件。 / Sealed Letter From Pernos.
					removeQuestItem(env, 182215973, 1);
					return HandlerResult.SUCCESS;
				}
			}
		}
		return HandlerResult.FAILED;
	}
}
