package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.model.EmotionType;
import com.aionemu.gameserver.model.EmotionId;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.state.CreatureState;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ASCENSION_MORPH;
import com.aionemu.gameserver.network.aion.serverpackets.SM_EMOTION;
import com.aionemu.gameserver.questEngine.definition.QuestPlayerEmotion;
import com.aionemu.gameserver.utils.PacketSendUtility;

import java.util.Objects;

/**
 * Real {@link QuestEffectPort}: after commit, sends the ascension-morph and
 * flight-teleport packets to the player. Both effects mirror the legacy
 * handler wiring (see {@code _1002Request_Of_The_Elim}).
 */
public final class PlayerQuestEffectPort implements QuestEffectPort {
	private final QuestPlayerPort players;

	public PlayerQuestEffectPort(QuestPlayerPort players) {
		this.players = Objects.requireNonNull(players, "players");
	}

	@Override
	public boolean morph(QuestSnapshot snapshot, QuestMutationPlan plan, int ascensionId) {
		Objects.requireNonNull(snapshot, "snapshot");
		Objects.requireNonNull(plan, "plan");
		if (ascensionId != 0 && ascensionId != 1) {
			throw new IllegalArgumentException("ascension morph state must be 0 or 1");
		}
		Player player = players.find(snapshot.playerId());
		if (player == null) {
			// 提交已成功但玩家已登出:无可发送对象,best-effort 跳过。
			return false;
		}
		PacketSendUtility.sendPacket(player, new SM_ASCENSION_MORPH(ascensionId));
		return true;
	}

	@Override
	public boolean playerEmotion(QuestSnapshot snapshot, QuestMutationPlan plan,
			QuestPlayerEmotion emotion) {
		Objects.requireNonNull(snapshot, "snapshot");
		Objects.requireNonNull(plan, "plan");
		Objects.requireNonNull(emotion, "emotion");
		Player player = players.find(snapshot.playerId());
		if (player == null) {
			return false;
		}
		int targetObjectId = snapshot.interactionObjectId();
		if (emotion != QuestPlayerEmotion.STAND) {
			throw new IllegalArgumentException("unsupported player emotion: " + emotion);
		}
		if (targetObjectId <= 0) {
			// STAND 必须发送给持有本次交互的 NPC；缺少权威对象时不能退化为目标 object 0 的数据包。
			// STAND is emitted against the NPC that owns the interaction. Never
			// turn a missing authoritative object into a packet targeting object 0.
			throw new IllegalStateException("player emotion requires an authoritative interaction objectId "
				+ "for quest " + snapshot.questId());
		}
		PacketSendUtility.broadcastPacket(player,
			new SM_EMOTION(player, EmotionType.EMOTE, EmotionId.STAND.id(), targetObjectId), true);
		return true;
	}

	@Override
	public boolean flightTeleport(QuestSnapshot snapshot, QuestMutationPlan plan, int flightTeleportId) {
		Objects.requireNonNull(snapshot, "snapshot");
		Objects.requireNonNull(plan, "plan");
		if (flightTeleportId <= 0) {
			throw new IllegalArgumentException("flightTeleportId must be positive");
		}
		Player player = players.find(snapshot.playerId());
		if (player == null) {
			// 提交已成功但玩家已登出:无可发送对象,best-effort 跳过。
			return false;
		}
		player.setState(CreatureState.FLIGHT_TELEPORT);
		player.unsetState(CreatureState.ACTIVE);
		player.setFlightTeleportId(flightTeleportId);
		PacketSendUtility.sendPacket(player,
			new SM_EMOTION(player, EmotionType.START_FLYTELEPORT, flightTeleportId, 0));
		return true;
	}
}
