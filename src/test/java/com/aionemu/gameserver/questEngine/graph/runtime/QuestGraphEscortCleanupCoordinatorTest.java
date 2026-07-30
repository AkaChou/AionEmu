package com.aionemu.gameserver.questEngine.graph.runtime;

import static com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ActionResult.APPLIED;
import static com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ActionResult.FAILED;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.EscortSource;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.EscortZoneDestination;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.QuestStatus;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.StartEscortAction;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEscortActionAdapter.CleanupReason;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.PersistenceResult;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.CleanupLease;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.EscortResourceIdentity;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.Lifecycle;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.QuestHistory;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphStateList;

class QuestGraphEscortCleanupCoordinatorTest {

	@Test
	void physicalCleanupAndLeaseCasAdvanceTogether() {
		PlayerQuestGraphStateList states = statesWith(lease());
		AtomicInteger physical = new AtomicInteger();
		AtomicReference<PlayerQuestGraphState> persisted = new AtomicReference<>();
		QuestGraphEscortCleanupCoordinator coordinator = new QuestGraphEscortCleanupCoordinator(7, states, (lease, reason) -> {
			physical.incrementAndGet();
			assertEquals(CleanupReason.LOGOUT, reason);
			return APPLIED;
		}, (expectedRevision, state) -> {
			assertEquals(5L, expectedRevision);
			persisted.set(state);
			return PersistenceResult.APPLIED;
		});

		assertEquals(APPLIED, coordinator.cleanupAll(CleanupReason.LOGOUT));
		assertEquals(1, physical.get());
		assertEquals(6, persisted.get().getRevision());
		assertEquals(Map.of(), persisted.get().getCleanupLeases());
		assertEquals(persisted.get(), states.get(2333));
	}

	@Test
	void persistenceConflictRetainsLedgerForIdempotentCompensation() {
		CleanupLease lease = lease();
		PlayerQuestGraphStateList states = statesWith(lease);
		QuestGraphEscortCleanupCoordinator coordinator = new QuestGraphEscortCleanupCoordinator(7, states,
			(candidate, reason) -> APPLIED, (expectedRevision, state) -> PersistenceResult.CONFLICT);

		assertEquals(FAILED, coordinator.cleanupQuest(2333, CleanupReason.PLAYER_DEATH));
		assertEquals(5, states.get(2333).getRevision());
		assertEquals(Map.of(lease.resourceKey(), lease), states.get(2333).getCleanupLeases());
	}

	@Test
	void unresolvedLegacyLeaseFailsBeforePhysicalCleanup() {
		CleanupLease unresolved = new CleanupLease("QUEST_ESCORT", "legacy");
		PlayerQuestGraphStateList states = statesWith(unresolved);
		AtomicInteger physical = new AtomicInteger();
		QuestGraphEscortCleanupCoordinator coordinator = new QuestGraphEscortCleanupCoordinator(7, states, (lease, reason) -> {
			physical.incrementAndGet();
			return APPLIED;
		}, (expectedRevision, state) -> PersistenceResult.APPLIED);

		assertEquals(FAILED, coordinator.cleanupAll(CleanupReason.LOGOUT));
		assertEquals(0, physical.get());
		assertEquals(Map.of("legacy", unresolved), states.get(2333).getCleanupLeases());
	}

	private static PlayerQuestGraphStateList statesWith(CleanupLease lease) {
		PlayerQuestGraphStateList states = new PlayerQuestGraphStateList();
		states.addLoaded(new PlayerQuestGraphState(2333, 1, 5, "active", QuestStatus.START, QuestHistory.EMPTY, null,
			Lifecycle.ACTIVE, Map.of(), Map.of(), null, Map.of(lease.resourceKey(), lease), null));
		return states;
	}

	private static CleanupLease lease() {
		StartEscortAction action = new StartEscortAction(EscortSource.PLAYER_POSITION_SPAWN, 204416, (byte) 8, null,
			true, true, true, false, new EscortZoneDestination("DF2_ITEMUSEAREA_Q2333"));
		return CleanupLease.escort(new EscortResourceIdentity(7, 2333, 990777, 204416, 210040000, 3, 10, 20, 30,
			0, 0, true, "old-walker", action, "durable-escort"));
	}
}
