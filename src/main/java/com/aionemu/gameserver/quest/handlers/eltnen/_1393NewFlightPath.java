package com.aionemu.gameserver.quest.handlers.eltnen;

import com.aionemu.gameserver.model.EmotionType;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.model.gameobjects.state.CreatureState;
import com.aionemu.gameserver.network.aion.serverpackets.SM_EMOTION;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.zone.ZoneName;

/**
 * 艾特南任务脚本：New Flight Path（任务 ID 1393）。
 * Eltnen quest script: New Flight Path (quest ID 1393).
 *
 * @author Balthazar
 */
public class _1393NewFlightPath extends QuestHandler {

	private final static int questId = 1393;

	public _1393NewFlightPath() {
		super(questId);
	}

	@Override
	public void register() {
		qe.registerQuestNpc(204041).addOnQuestStart(questId);
		qe.registerQuestNpc(204041).addOnTalkEvent(questId);
		qe.registerOnEnterZone(ZoneName.get("LEPHARIST_BASTION_210020000"), questId);
	}

	@Override
	public boolean onDialogEvent(QuestEnv env) {
		final Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		int targetId = 0;
		if (env.getVisibleObject() instanceof Npc)
			targetId = ((Npc) env.getVisibleObject()).getNpcId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 204041) {
				switch (env.getDialog()) {
					case START_DIALOG: {
						return sendQuestDialog(env, 1011);
					}
					default:
						return sendQuestStartDialog(env);
				}
			}
		}
		else if (qs != null && qs.getStatus() == QuestStatus.START) {
			if (targetId == 204041) {
				switch (env.getDialog()) {
					case START_DIALOG: {
						return sendQuestDialog(env, 1003);
					}
					case STEP_TO_1: {
						player.setState(CreatureState.FLIGHT_TELEPORT);
						player.unsetState(CreatureState.ACTIVE);
						player.setFlightTeleportId(17001);
						PacketSendUtility.sendPacket(player, new SM_EMOTION(player, EmotionType.START_FLYTELEPORT, 17001, 0));
						return closeDialogWindow(env);
					}
				}
			}
		}
		else if (qs != null && qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 204041) {
				switch (env.getDialog()) {
					case USE_OBJECT: {
                         return sendQuestDialog(env, 1352);
					}
					case SELECT_REWARD: {
                        return sendQuestDialog(env, 5);
				    }
					default:
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
			changeQuestStep(env, 0, 0, true);
			return true;
		}
		return false;
	}
}
