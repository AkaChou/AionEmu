package com.aionemu.gameserver.dao.impl;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.QuestStatus;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.CleanupLease;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.InstanceSpawnResourceIdentity;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.Lifecycle;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.QuestHistory;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.SpawnPlacementKind;

class PlayerQuestGraphStateDAOSqlTest {

	@Test
	void selectIsDeterministicAndUpsertPersistsRecoveryColumns() {
		assertTrue(PlayerQuestGraphStateDAO.SELECT_QUERY.endsWith("ORDER BY `quest_id`"));
		assertTrue(PlayerQuestGraphStateDAO.UPSERT_QUERY.contains("ON DUPLICATE KEY UPDATE"));
		assertTrue(PlayerQuestGraphStateDAO.UPSERT_QUERY.contains("IF(VALUES(`revision`) > `revision`"));
		assertTrue(PlayerQuestGraphStateDAO.UPSERT_QUERY.contains("`next_deadline_at` = IF("));
		assertTrue(PlayerQuestGraphStateDAO.UPSERT_QUERY.contains("`state_payload` = IF("));
		assertTrue(PlayerQuestGraphStateDAO.UPSERT_QUERY.contains("`revision` = GREATEST(`revision`, VALUES(`revision`))"));
		assertTrue(PlayerQuestGraphStateDAO.INSERT_QUERY.startsWith("INSERT INTO `player_quest_graph_states`"));
		assertTrue(PlayerQuestGraphStateDAO.UPDATE_CAS_QUERY.endsWith("AND `revision` = ?"));
		assertTrue(PlayerQuestGraphStateDAO.USED_RESOURCE_IDS_QUERY.endsWith("ORDER BY `player_id`, `quest_id`"));
		assertTrue(Arrays.asList(new GameDAOClassProvider().daoClasses()).contains(PlayerQuestGraphStateDAO.class));
	}

	@Test
	void materializedStateLeaseKeepsObjectIdUsedAfterOperationRegistryRelease() {
		InstanceSpawnResourceIdentity identity = new InstanceSpawnResourceIdentity(7, 1230, 990001, 216608,
			SpawnPlacementKind.PLAYER, 0, 7, 210040000, 3, 10, 20, 30, (byte) 0, "cleanup-before-cas");
		PlayerQuestGraphState state = new PlayerQuestGraphState(1230, 1, 4, "active", QuestStatus.START,
			QuestHistory.EMPTY, null, Lifecycle.ACTIVE, Map.of(), Map.of(), null,
			Map.of(identity.idempotencyKey(), CleanupLease.instanceSpawn(identity)), null);

		assertArrayEquals(new int[] { 990001 }, PlayerQuestGraphStateDAO.materializedResourceObjectIds(java.util.List.of(state)));
	}
}
