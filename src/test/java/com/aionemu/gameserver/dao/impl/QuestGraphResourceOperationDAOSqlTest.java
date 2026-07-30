package com.aionemu.gameserver.dao.impl;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.EscortCoordinatesDestination;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.EscortSource;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.StartEscortAction;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.CleanupLease;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.EscortResourceIdentity;

class QuestGraphResourceOperationDAOSqlTest {

	@Test
	void registryUsesHashedOperationKeyExactPayloadDeleteAndReservedObjectIndex() {
		assertTrue(QuestGraphResourceOperationDAO.MAX_OPERATION_KEY_LENGTH == 1024);
		assertTrue(QuestGraphResourceOperationDAO.SELECT_QUERY.contains("`operation_hash` = ?"));
		assertTrue(QuestGraphResourceOperationDAO.INSERT_QUERY.contains("`reserved_object_id`"));
		assertTrue(QuestGraphResourceOperationDAO.DELETE_QUERY.contains("`resource_payload` = ?"));
		assertTrue(QuestGraphResourceOperationDAO.USED_IDS_QUERY.contains("`resource_payload`"));
		assertTrue(QuestGraphResourceOperationDAO.USED_IDS_QUERY.endsWith("ORDER BY `player_id`, `operation_hash`"));
		assertTrue(Arrays.asList(new GameDAOClassProvider().daoClasses()).contains(QuestGraphResourceOperationDAO.class));
	}

	@Test
	void eventNpcIdentityRemainsReservedAcrossRestartAndOversizedKeysFailBeforeSql() {
		StartEscortAction action = new StartEscortAction(EscortSource.EVENT_NPC, 0, (byte) 0, "4212", true, false,
			true, false, new EscortCoordinatesDestination(1, 2, 3));
		EscortResourceIdentity identity = new EscortResourceIdentity(7, 2634, 880001, 203709, 210010000, 1,
			1, 2, 3, 203709, 880001, false, "old-walker", action, "event-npc-operation");

		assertTrue(QuestGraphResourceOperationDAO.ownsObjectId(CleanupLease.escort(identity)));
		assertThrows(IllegalArgumentException.class,
			() -> new QuestGraphResourceOperationDAO().find(7, "x".repeat(QuestGraphResourceOperationDAO.MAX_OPERATION_KEY_LENGTH + 1)));
	}
}
