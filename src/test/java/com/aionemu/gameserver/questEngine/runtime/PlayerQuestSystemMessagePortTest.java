package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.definition.QuestMetadata;
import com.aionemu.gameserver.questEngine.definition.QuestSystemMessage;
import com.aionemu.gameserver.questEngine.definition.QuestSystemMessagePacket;
import com.aionemu.gameserver.questEngine.definition.QuestSystemMessageTarget;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerQuestSystemMessagePortTest {
	@Test
	void sendsWarehouseFullMessageThroughTheProductionBoundary() {
		Player player = new ObjenesisStd().newInstance(Player.class);
		List<String> calls = new ArrayList<>();
		PlayerQuestSystemMessagePort port = new PlayerQuestSystemMessagePort(playerId -> player,
			new PlayerQuestSystemMessagePort.MessageOperations() {
				@Override
				public void questFailed(Player target, String questName) {
					calls.add("failed:" + questName);
				}

				@Override
				public void warehouseFull(Player target) {
					calls.add("warehouse-full:" + (target == player));
				}
			});
		QuestSnapshot snapshot = new QuestSnapshot(7, 10032, QuestStatus.START, 3, Map.of());
		QuestMutationPlan plan = new QuestMutationPlan(10032, QuestStatus.START, 3, List.of(), List.of());

		assertTrue(port.send(snapshot, plan, QuestSystemMessage.WAREHOUSE_FULL_INVENTORY));

		assertEquals(List.of("warehouse-full:true"), calls);
	}

	@Test
	void skipsMessageWhenPlayerIsOfflineAfterCommit() {
		PlayerQuestSystemMessagePort port = new PlayerQuestSystemMessagePort(playerId -> null,
			new PlayerQuestSystemMessagePort.MessageOperations() {
				@Override
				public void questFailed(Player target, String questName) {
				}

				@Override
				public void warehouseFull(Player target) {
				}
			});
		QuestSnapshot snapshot = new QuestSnapshot(7, 10032, QuestStatus.START, 3, Map.of());
		QuestMutationPlan plan = new QuestMutationPlan(10032, QuestStatus.START, 3, List.of(), List.of());

		assertFalse(port.send(snapshot, plan, QuestSystemMessage.WAREHOUSE_FULL_INVENTORY));
	}

	@Test
	void sendsQuestSpecificPacketWithTheAuthoritativePlayerObjectTarget() {
		Player player = new ObjenesisStd().newInstance(Player.class);
		List<String> calls = new ArrayList<>();
		PlayerQuestSystemMessagePort port = new PlayerQuestSystemMessagePort(playerId -> player,
			new PlayerQuestSystemMessagePort.MessageOperations() {
				@Override
				public void questFailed(Player target, String questName) {
				}

				@Override
				public void warehouseFull(Player target) {
				}

				@Override
				public void packet(Player target, QuestSystemMessagePacket message) {
					calls.add(message.messageId() + ":" + message.target() + ":" + message.params().size());
				}
			});
		QuestSnapshot snapshot = new QuestSnapshot(7, 18602, QuestStatus.START, 3, Map.of());
		QuestMutationPlan plan = new QuestMutationPlan(18602, QuestStatus.START, 3, List.of(), List.of());

		assertTrue(port.send(snapshot, plan,
			new QuestSystemMessagePacket(1111307, QuestSystemMessageTarget.PLAYER, false, 2, List.of())));
		assertEquals(List.of("1111307:PLAYER:0"), calls);
	}

	@Test
	void questFailedMessageUsesCanonicalMetadataName() {
		Player player = new ObjenesisStd().newInstance(Player.class);
		List<String> calls = new ArrayList<>();
		QuestMetadata metadata = QuestMetadata.minimal("Canonical quest name", 1, "QUEST");
		PlayerQuestSystemMessagePort port = new PlayerQuestSystemMessagePort(playerId -> player,
			new PlayerQuestSystemMessagePort.MessageOperations() {
				@Override
				public void questFailed(Player target, String questName) {
					calls.add(questName);
				}

				@Override
				public void warehouseFull(Player target) {
				}
			}, questId -> questId == 10032 ? metadata : null);
		QuestSnapshot snapshot = new QuestSnapshot(7, 10032, QuestStatus.START, 3, Map.of());
		QuestMutationPlan plan = new QuestMutationPlan(10032, QuestStatus.START, 3, List.of(), List.of());

		assertTrue(port.send(snapshot, plan, QuestSystemMessage.QUEST_FAILED));

		assertEquals(List.of("Canonical quest name"), calls);
	}
}
