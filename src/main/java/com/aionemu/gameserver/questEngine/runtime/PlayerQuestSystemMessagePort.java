package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.lifecycle.GameEngineServices;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.SystemMessageId;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.questEngine.definition.QuestMetadata;
import com.aionemu.gameserver.questEngine.definition.QuestSystemMessage;
import com.aionemu.gameserver.questEngine.definition.QuestSystemMessagePacket;
import com.aionemu.gameserver.questEngine.definition.QuestSystemMessageTarget;
import com.aionemu.gameserver.utils.PacketSendUtility;

import java.util.Objects;
import java.util.function.IntFunction;

/** Production system-message boundary for the currently modeled quest messages. */
public final class PlayerQuestSystemMessagePort implements QuestSystemMessagePort {
	private final QuestPlayerPort players;
	private final MessageOperations operations;
	private final IntFunction<QuestMetadata> metadata;

	public PlayerQuestSystemMessagePort(QuestPlayerPort players) {
		this(players, questId -> GameEngineServices.questEngine().questCatalog().findMetadata(questId).orElse(null));
	}

	PlayerQuestSystemMessagePort(QuestPlayerPort players, IntFunction<QuestMetadata> metadata) {
		this(players, new MessageOperations() {
			@Override
			public void questFailed(Player player, String questName) {
				PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(SystemMessageId.QUEST_FAILED_$1, questName));
			}

			@Override
			public void warehouseFull(Player player) {
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_WAREHOUSE_FULL_INVENTORY);
			}

			@Override
			public void packet(Player player, QuestSystemMessagePacket message) {
				int objectId = message.target() == QuestSystemMessageTarget.PLAYER ? player.getObjectId() : 0;
				PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(message.npcShout(), message.messageId(),
					objectId, message.textColorId(), message.params().toArray()));
			}
		}, metadata);
	}

	PlayerQuestSystemMessagePort(QuestPlayerPort players, MessageOperations operations) {
		this(players, operations,
			questId -> GameEngineServices.questEngine().questCatalog().findMetadata(questId).orElse(null));
	}

	PlayerQuestSystemMessagePort(QuestPlayerPort players, MessageOperations operations,
			IntFunction<QuestMetadata> metadata) {
		this.players = Objects.requireNonNull(players, "players");
		this.operations = Objects.requireNonNull(operations, "operations");
		this.metadata = Objects.requireNonNull(metadata, "metadata");
	}

	@Override
	public boolean send(QuestSnapshot snapshot, QuestMutationPlan plan, QuestSystemMessage message) {
		Objects.requireNonNull(snapshot, "snapshot");
		Objects.requireNonNull(plan, "plan");
		Objects.requireNonNull(message, "message");
		Player player = players.find(snapshot.playerId());
		if (player == null) {
			return false;
		}
		switch (message) {
			case QUEST_FAILED -> {
				QuestMetadata questMetadata = metadata.apply(snapshot.questId());
				if (questMetadata == null) {
					throw new IllegalStateException("QUEST_FAILED requires quest metadata for " + snapshot.questId());
				}
				operations.questFailed(player, questMetadata.name());
			}
			case WAREHOUSE_FULL_INVENTORY -> operations.warehouseFull(player);
		}
		return true;
	}

	@Override
	public boolean send(QuestSnapshot snapshot, QuestMutationPlan plan, QuestSystemMessagePacket message) {
		Objects.requireNonNull(snapshot, "snapshot");
		Objects.requireNonNull(plan, "plan");
		Objects.requireNonNull(message, "message");
		Player player = players.find(snapshot.playerId());
		if (player == null) {
			return false;
		}
		operations.packet(player, message);
		return true;
	}

	interface MessageOperations {
		void questFailed(Player player, String questName);

		void warehouseFull(Player player);

		default void packet(Player player, QuestSystemMessagePacket message) {
			throw new UnsupportedOperationException("raw quest system messages are not configured");
		}
	}
}
