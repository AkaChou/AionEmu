package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.lifecycle.GameEngineServices;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.EmotionType;
import com.aionemu.gameserver.model.EmotionId;
import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.npcFaction.ENpcFactionQuestState;
import com.aionemu.gameserver.model.gameobjects.player.npcFaction.NpcFaction;
import com.aionemu.gameserver.model.gameobjects.state.CreatureState;
import com.aionemu.gameserver.model.templates.QuestTemplate;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ASCENSION_MORPH;
import com.aionemu.gameserver.network.aion.serverpackets.SM_EMOTION;
import com.aionemu.gameserver.questEngine.definition.QuestPlayerEmotion;
import com.aionemu.gameserver.services.ClassChangeService;
import com.aionemu.gameserver.utils.PacketSendUtility;

import java.util.Objects;

/**
 * Real {@link QuestEffectPort}: after commit, sends the ascension-morph and
 * flight-teleport packets to the player. Both effects mirror the legacy
 * handler wiring (see {@code _1002Request_Of_The_Elim}).
 */
public final class PlayerQuestEffectPort implements QuestEffectPort {
	private final QuestPlayerPort players;
	private final EffectOperations effectOperations;

	public PlayerQuestEffectPort(QuestPlayerPort players) {
		this(players, new EffectOperations() {
			@Override
			public void apply(Player player, int skillId, int durationMillis) {
				GameEngineServices.skillEngine().applyEffectDirectly(skillId, player, player, durationMillis);
			}

			@Override
			public void remove(Player player, int effectId) {
				player.getEffectController().removeEffect(effectId);
			}
		});
	}

	PlayerQuestEffectPort(QuestPlayerPort players, EffectOperations effectOperations) {
		this.players = Objects.requireNonNull(players, "players");
		this.effectOperations = Objects.requireNonNull(effectOperations, "effectOperations");
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
	public boolean setPlayerClass(QuestSnapshot snapshot, QuestMutationPlan plan, PlayerClass playerClass) {
		Objects.requireNonNull(snapshot, "snapshot");
		Objects.requireNonNull(plan, "plan");
		if (playerClass == null || playerClass == PlayerClass.ALL || playerClass.isStartingClass()) {
			throw new IllegalArgumentException("playerClass must be a concrete advanced class");
		}
		Player player = players.find(snapshot.playerId());
		if (player == null) {
			return false;
		}
		ClassChangeService.setClass(player, playerClass);
		return player.getPlayerClass() == playerClass;
	}

	@Override
	public boolean startNpcFactionQuest(QuestSnapshot snapshot, QuestMutationPlan plan, int npcFactionId) {
		Objects.requireNonNull(snapshot, "snapshot");
		Objects.requireNonNull(plan, "plan");
		Player player = players.find(snapshot.playerId());
		QuestTemplate template = npcFactionTemplate(snapshot, npcFactionId);
		if (player == null || template == null || player.getNpcFactions() == null) {
			return false;
		}
		NpcFaction faction = player.getNpcFactions().getNpcFactionById(npcFactionId);
		if (faction == null || !faction.isActive() || faction.getQuestId() != snapshot.questId()) {
			return false;
		}
		player.getNpcFactions().startQuest(template);
		return faction.getState() == ENpcFactionQuestState.START;
	}

	@Override
	public boolean completeNpcFactionQuest(QuestSnapshot snapshot, QuestMutationPlan plan, int npcFactionId) {
		Objects.requireNonNull(snapshot, "snapshot");
		Objects.requireNonNull(plan, "plan");
		Player player = players.find(snapshot.playerId());
		QuestTemplate template = npcFactionTemplate(snapshot, npcFactionId);
		if (player == null || template == null || player.getNpcFactions() == null) {
			return false;
		}
		NpcFaction faction = player.getNpcFactions().getNpcFactionById(npcFactionId);
		if (faction == null || !faction.isActive()) {
			return false;
		}
		player.getNpcFactions().completeQuest(template);
		return faction.getState() == ENpcFactionQuestState.COMPLETE;
	}

	@Override
	public boolean abortNpcFactionQuest(QuestSnapshot snapshot, QuestMutationPlan plan, int npcFactionId) {
		Objects.requireNonNull(snapshot, "snapshot");
		Objects.requireNonNull(plan, "plan");
		Player player = players.find(snapshot.playerId());
		QuestTemplate template = npcFactionTemplate(snapshot, npcFactionId);
		if (player == null || template == null || player.getNpcFactions() == null) {
			return false;
		}
		NpcFaction faction = player.getNpcFactions().getNpcFactionById(npcFactionId);
		if (faction == null || !faction.isActive()) {
			return false;
		}
		player.getNpcFactions().abortQuest(template);
		return faction.getState() == ENpcFactionQuestState.NOTING;
	}

	private static QuestTemplate npcFactionTemplate(QuestSnapshot snapshot, int npcFactionId) {
		if (npcFactionId <= 0 || DataManager.QUEST_DATA == null) {
			return null;
		}
		QuestTemplate template = DataManager.QUEST_DATA.getQuestById(snapshot.questId());
		return template != null && template.getNpcFactionId() == npcFactionId ? template : null;
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
	public boolean applyEffect(QuestSnapshot snapshot, QuestMutationPlan plan, int skillId, int durationMillis) {
		Objects.requireNonNull(snapshot, "snapshot");
		Objects.requireNonNull(plan, "plan");
		if (skillId <= 0 || durationMillis < 0) {
			throw new IllegalArgumentException("skillId must be positive and durationMillis non-negative");
		}
		Player player = players.find(snapshot.playerId());
		if (player == null) {
			return false;
		}
		effectOperations.apply(player, skillId, durationMillis);
		return true;
	}

	@Override
	public boolean removeEffect(QuestSnapshot snapshot, QuestMutationPlan plan, int effectId) {
		Objects.requireNonNull(snapshot, "snapshot");
		Objects.requireNonNull(plan, "plan");
		if (effectId <= 0) {
			throw new IllegalArgumentException("effectId must be positive");
		}
		Player player = players.find(snapshot.playerId());
		if (player == null) {
			return false;
		}
		effectOperations.remove(player, effectId);
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

	interface EffectOperations {
		void apply(Player player, int skillId, int durationMillis);

		void remove(Player player, int effectId);
	}
}
