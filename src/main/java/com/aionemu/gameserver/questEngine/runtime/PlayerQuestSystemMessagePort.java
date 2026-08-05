package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.SystemMessageId;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.questEngine.definition.QuestSystemMessage;
import com.aionemu.gameserver.questEngine.definition.QuestSystemMessagePacket;
import com.aionemu.gameserver.questEngine.definition.QuestSystemMessageTarget;
import com.aionemu.gameserver.utils.PacketSendUtility;

import java.util.Objects;

/** Production system-message boundary for the currently modeled quest messages. */
public final class PlayerQuestSystemMessagePort implements QuestSystemMessagePort {
	private final QuestPlayerPort players;
	private final MessageOperations operations;

	public PlayerQuestSystemMessagePort(QuestPlayerPort players) {
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
		});
	}

	PlayerQuestSystemMessagePort(QuestPlayerPort players, MessageOperations operations) {
		this.players = Objects.requireNonNull(players, "players");
		this.operations = Objects.requireNonNull(operations, "operations");
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
				var quest = DataManager.QUEST_DATA == null ? null : DataManager.QUEST_DATA.getQuestById(snapshot.questId());
				if (quest == null || quest.getName() == null) {
					throw new IllegalStateException("QUEST_FAILED requires quest metadata for " + snapshot.questId());
				}
				operations.questFailed(player, quest.getName());
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
