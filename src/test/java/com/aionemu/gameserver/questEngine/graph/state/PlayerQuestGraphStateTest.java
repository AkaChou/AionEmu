package com.aionemu.gameserver.questEngine.graph.state;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.EscortCoordinatesDestination;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.EscortSource;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.QuestStatus;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.StartEscortAction;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.BooleanValue;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.CleanupLease;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.EscortResourceIdentity;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.IntValue;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.InstanceSpawnResourceIdentity;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.ItemMutationKind;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.ItemMutationPlan;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.Lifecycle;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.PreparedTransition;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.QuestHistory;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.RepeatDeadlineDisposition;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.RepeatDeadlineResolution;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.SpawnPlacementKind;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.VariableValue;

class PlayerQuestGraphStateTest {

	@Test
	void codecRoundTripsPreparedStateDeterministically() {
		Map<String, VariableValue> firstVariables = new LinkedHashMap<>();
		firstVariables.put("score", new IntValue(7));
		firstVariables.put("ready", new BooleanValue(true));
		Map<String, VariableValue> reversedVariables = new LinkedHashMap<>();
		reversedVariables.put("ready", new BooleanValue(true));
		reversedVariables.put("score", new IntValue(7));
		ItemMutationPlan itemPlan = new ItemMutationPlan(1, ItemMutationKind.REMOVE_EXACT, 182200001, 2, 5, 3);
		PreparedTransition journal = new PreparedTransition(0, "event-9", "kill-advance", 2, RepeatDeadlineResolution.deadline(1_760_000_000_000L),
			Map.of(1, itemPlan), new byte[] { 4, 5, 6 });
		PlayerQuestGraphState first = state(firstVariables, journal);
		PlayerQuestGraphState reversed = state(reversedVariables, journal);

		byte[] encoded = PlayerQuestGraphStateCodec.encode(first);
		assertArrayEquals(encoded, PlayerQuestGraphStateCodec.encode(reversed));
		PlayerQuestGraphState decoded = PlayerQuestGraphStateCodec.decode(1230, 2, 3, "hunt", 77L, Lifecycle.PREPARED, encoded);

		assertEquals(List.of("ready", "score"), new ArrayList<>(decoded.getVariables().keySet()));
		assertEquals(Map.of("soft", 1_900_000_000_000L, "timeout", 1_800_000_000_000L), decoded.getDeadlines());
		assertEquals(1_750_000_000_000L, decoded.nextDeadlineAt());
		assertEquals("SPAWN", decoded.getCleanupLeases().get("escort").capability());
		assertEquals("event-9", decoded.getJournal().getEventId());
		assertEquals(RepeatDeadlineResolution.deadline(1_760_000_000_000L), decoded.getJournal().getRepeatDeadlineResolution());
		assertEquals(Map.of(1, itemPlan), decoded.getJournal().getItemMutationPlans());
		assertFalse(decoded.getJournal().isTargetCommitted());
		assertEquals(QuestStatus.START, decoded.getQuestStatus());
		assertEquals(new QuestHistory(3, 2, 1_700_000_000_000L, 1_750_000_000_000L), decoded.getHistory());
		assertArrayEquals(new byte[] { 4, 5, 6 }, decoded.getJournal().getEventPayload());
	}

	@Test
	void codecRoundTripsTypedResourcePlansAndMaterializedLeases() {
		InstanceSpawnResourceIdentity spawn = new InstanceSpawnResourceIdentity(7, 1230, 990001, 216608,
			SpawnPlacementKind.DIALOG_TARGET, 700759, 880759, 210040000, 3, 10.5f, 20.5f, 30.5f, (byte) 64, "spawn-key");
		StartEscortAction action = new StartEscortAction(EscortSource.PLAYER_POSITION_SPAWN, 204416, (byte) 8, "4212",
			true, true, true, false, new EscortCoordinatesDestination(1.5f, 2.5f, 3.5f));
		EscortResourceIdentity escort = new EscortResourceIdentity(7, 1230, 990002, 204416, 210040000, 3,
			11.5f, 12.5f, 13.5f, 0, 0, true, "old-walker", action, "escort-key");
		PlayerQuestGraphState state = new PlayerQuestGraphState(1230, 2, 0, "hunt", QuestStatus.START, QuestHistory.EMPTY, null,
			Lifecycle.ACTIVE, Map.of(), Map.of(), null,
			Map.of("escort-key", CleanupLease.escort(escort), "spawn-key", CleanupLease.instanceSpawn(spawn)), null);

		PlayerQuestGraphState decoded = PlayerQuestGraphStateCodec.decode(1230, 2, 0, "hunt", null, Lifecycle.ACTIVE,
			PlayerQuestGraphStateCodec.encode(state));

		assertEquals(spawn, decoded.getCleanupLeases().get("spawn-key").identity());
		assertEquals(escort, decoded.getCleanupLeases().get("escort-key").identity());
		assertTrue(decoded.getCleanupLeases().values().stream().allMatch(CleanupLease::resolved));
		assertThrows(IllegalArgumentException.class,
			() -> PlayerQuestGraphStateCodec.decode(1231, 2, 0, "hunt", null, Lifecycle.ACTIVE,
				PlayerQuestGraphStateCodec.encode(state)));
	}

	@Test
	void typedCleanupLeasesRequireCanonicalLedgerKeysAndQuestOwners() {
		InstanceSpawnResourceIdentity spawn = new InstanceSpawnResourceIdentity(7, 1230, 0, 216608,
			SpawnPlacementKind.STATIC_SPAWN, 700759, 0, 210040000, 3, 10.5f, 20.5f, 30.5f, (byte) 64, "spawn-key");
		CleanupLease lease = CleanupLease.instanceSpawn(spawn);

		assertThrows(IllegalArgumentException.class,
			() -> new PlayerQuestGraphState(1230, 2, 0, "hunt", QuestStatus.START, QuestHistory.EMPTY, null,
				Lifecycle.ACTIVE, Map.of(), Map.of(), null, Map.of("wrong-key", lease), null));
		assertThrows(IllegalArgumentException.class,
			() -> new PlayerQuestGraphState(1231, 2, 0, "hunt", QuestStatus.START, QuestHistory.EMPTY, null,
				Lifecycle.ACTIVE, Map.of(), Map.of(), null, Map.of("spawn-key", lease), null));
	}

	@Test
	void codecReadsLegacyQgs5CleanupLeaseAsUnresolved() throws Exception {
		PlayerQuestGraphState decoded = PlayerQuestGraphStateCodec.decode(1, 1, 0, "active", null, Lifecycle.ACTIVE,
			legacyQgs5ActivePayloadWithLease());

		CleanupLease lease = decoded.getCleanupLeases().get("legacy");
		assertEquals("INSTANCE_SCOPED_SPAWN", lease.capability());
		assertEquals("spawner:700759:npc:216608", lease.resourceKey());
		assertFalse(lease.resolved());
	}

	/** 验证高权限绕过在 history 与 PREPARED journal 中都可确定性往返。 / Verifies deterministic privileged-bypass round trips in history and journal. */
	@Test
	void codecRoundTripsPrivilegedRepeatBypass() {
		QuestHistory history = new QuestHistory(1, 0, 1_700_000_000_000L, null, RepeatDeadlineDisposition.PRIVILEGED_BYPASS);
		PreparedTransition journal = new PreparedTransition(3, "event", "finish", 0,
			RepeatDeadlineResolution.PRIVILEGED_BYPASS, new byte[] { 1 });
		PlayerQuestGraphState state = new PlayerQuestGraphState(1, 1, 4, "reward", QuestStatus.COMPLETE, history, null,
			Lifecycle.PREPARED, Map.of(), Map.of(), journal, Map.of(), null);

		PlayerQuestGraphState decoded = PlayerQuestGraphStateCodec.decode(1, 1, 4, "reward", null, Lifecycle.PREPARED,
			PlayerQuestGraphStateCodec.encode(state));

		assertEquals(RepeatDeadlineDisposition.PRIVILEGED_BYPASS, decoded.getHistory().repeatDeadlineDisposition());
		assertEquals(RepeatDeadlineResolution.PRIVILEGED_BYPASS, decoded.getJournal().getRepeatDeadlineResolution());
	}

	@Test
	void codecRoundTripsCommittedProtocolOutbox() {
		PreparedTransition journal = new PreparedTransition(0, "event", "finish", 2, true,
			RepeatDeadlineResolution.NOT_APPLICABLE, Map.of(), new byte[] { 1, 2 });
		PlayerQuestGraphState state = new PlayerQuestGraphState(1, 1, 4, "done", QuestStatus.START, QuestHistory.EMPTY, null,
			Lifecycle.PREPARED, Map.of(), Map.of(), journal, Map.of(), null);

		PlayerQuestGraphState decoded = PlayerQuestGraphStateCodec.decode(1, 1, 4, "done", null, Lifecycle.PREPARED,
			PlayerQuestGraphStateCodec.encode(state));

		assertTrue(decoded.getJournal().isTargetCommitted());
		assertEquals(2, decoded.getJournal().getNextActionIndex());
		assertEquals("done", decoded.getNodeId());
	}

	/**
	 * 验证追加发放、可选精确扣除和全部扣除计划均可确定性往返，且非法端点被拒绝。
	 * Verifies deterministic round trips for additive, optional-exact, and remove-all plans and rejects invalid endpoints.
	 */
	@Test
	void codecRoundTripsOptionalExactItemPlans() {
		ItemMutationPlan sufficient = new ItemMutationPlan(1, ItemMutationKind.REMOVE_OPTIONAL_EXACT, 182200001, 2, 5, 3);
		ItemMutationPlan insufficient = new ItemMutationPlan(2, ItemMutationKind.REMOVE_OPTIONAL_EXACT, 182200002, 2, 1, 1);
		ItemMutationPlan additive = new ItemMutationPlan(3, ItemMutationKind.GIVE_ADD_EXACT, 182200003, 4, 2, 6);
		ItemMutationPlan all = new ItemMutationPlan(4, ItemMutationKind.REMOVE_ALL, 182200004, 1, 7, 0);
		PreparedTransition journal = new PreparedTransition(0, "event", "remove", 0, RepeatDeadlineResolution.NOT_APPLICABLE,
			Map.of(1, sufficient, 2, insufficient, 3, additive, 4, all), new byte[] { 1 });
		PlayerQuestGraphState state = new PlayerQuestGraphState(1, 1, 1, "active", QuestStatus.START, QuestHistory.EMPTY, null,
			Lifecycle.PREPARED, Map.of(), Map.of(), journal, Map.of(), null);

		PlayerQuestGraphState decoded = PlayerQuestGraphStateCodec.decode(1, 1, 1, "active", null, Lifecycle.PREPARED,
			PlayerQuestGraphStateCodec.encode(state));

		assertEquals(Map.of(1, sufficient, 2, insufficient, 3, additive, 4, all), decoded.getJournal().getItemMutationPlans());
		assertThrows(IllegalArgumentException.class,
			() -> new ItemMutationPlan(0, ItemMutationKind.REMOVE_OPTIONAL_EXACT, 182200001, 2, 1, 0));
	}

	@Test
	void stateAndJournalDefensivelyCopyInputs() {
		Map<String, VariableValue> variables = new HashMap<>();
		variables.put("score", new IntValue(1));
		byte[] eventPayload = { 1, 2 };
		PreparedTransition journal = new PreparedTransition(2, "event", "transition", 0, eventPayload);
		PlayerQuestGraphState state = state(variables, journal);

		variables.clear();
		eventPayload[0] = 9;
		byte[] returnedPayload = journal.getEventPayload();
		returnedPayload[1] = 9;

		assertTrue(state.getVariables().containsKey("score"));
		assertArrayEquals(new byte[] { 1, 2 }, journal.getEventPayload());
		assertThrows(UnsupportedOperationException.class, () -> state.getDeadlines().clear());
	}

	@Test
	void invalidLifecycleAndCorruptPayloadAreRejected() throws Exception {
		assertThrows(IllegalArgumentException.class, () -> new PlayerQuestGraphState(1, 1, 0, "start", QuestStatus.NONE, QuestHistory.EMPTY, null,
			Lifecycle.PREPARED, Map.of(), Map.of(), null, Map.of(), null));
		assertThrows(IllegalArgumentException.class, () -> new PlayerQuestGraphState(1, 1, 0, "start", QuestStatus.START, QuestHistory.EMPTY, null,
			Lifecycle.QUARANTINED, Map.of(), Map.of(), null, Map.of(), null));
		assertThrows(IllegalArgumentException.class, () -> new PlayerQuestGraphState(1, 1, 1, "start", QuestStatus.START, QuestHistory.EMPTY, null,
			Lifecycle.QUARANTINED, Map.of(), Map.of(), new PreparedTransition(-1, "event", "transition", 0, new byte[0]), Map.of(), "reason"));
		assertThrows(IllegalArgumentException.class, () -> new PlayerQuestGraphState(1, 1, 0, "start", QuestStatus.NONE, QuestHistory.EMPTY, null,
			Lifecycle.PREPARED, Map.of(), Map.of(), new PreparedTransition(-1, "event", "transition", 1, new byte[0]), Map.of(), null));
		assertThrows(IllegalArgumentException.class,
			() -> new RepeatDeadlineResolution(RepeatDeadlineDisposition.DEADLINE, 0L));
		assertThrows(IllegalArgumentException.class,
			() -> new ItemMutationPlan(0, ItemMutationKind.REMOVE_EXACT, 182200001, 3, 2, 0));
		assertThrows(IllegalArgumentException.class,
			() -> new QuestHistory(1, 0, 1L, null, RepeatDeadlineDisposition.DEADLINE));
		assertThrows(IllegalArgumentException.class,
			() -> new QuestHistory(1, 0, 1L, 2L, RepeatDeadlineDisposition.PRIVILEGED_BYPASS));
		assertThrows(IllegalArgumentException.class, () -> new QuestHistory(0, 1, null, null));
		assertThrows(IllegalArgumentException.class, () -> new QuestHistory(1, 0, null, null));
		assertThrows(IllegalArgumentException.class, () -> new PlayerQuestGraphState(1, 1, 0, "start", QuestStatus.COMPLETE,
			QuestHistory.EMPTY, null, Lifecycle.ACTIVE, Map.of(), Map.of(), null, Map.of(), null));
		assertThrows(IllegalArgumentException.class, () -> new PlayerQuestGraphState(1, 1, 0, "start", QuestStatus.LOCKED,
			new QuestHistory(1, 0, 1L, null), null, Lifecycle.ACTIVE, Map.of(), Map.of(), null, Map.of(), null));
		PlayerQuestGraphState inactiveHistory = new PlayerQuestGraphState(1, 1, 0, "start", QuestStatus.NONE,
			new QuestHistory(1, 0, 1L, null), null, Lifecycle.ACTIVE, Map.of(), Map.of(), null, Map.of(), null);
		assertEquals(QuestStatus.NONE, inactiveHistory.getQuestStatus());

		byte[] valid = PlayerQuestGraphStateCodec.encode(new PlayerQuestGraphState(1, 1, 0, "start", QuestStatus.START, QuestHistory.EMPTY, null,
			Lifecycle.ACTIVE, Map.of(), Map.of(), null, Map.of(), null));
		byte[] trailing = java.util.Arrays.copyOf(valid, valid.length + 1);
		byte[] oldVersion = java.util.Arrays.copyOf(valid, valid.length);
		oldVersion[3] = 0x33;
		byte[] unknownStatus = java.util.Arrays.copyOf(valid, valid.length);
		unknownStatus[4] = 99;
		assertThrows(IllegalArgumentException.class,
			() -> PlayerQuestGraphStateCodec.decode(1, 1, 0, "start", null, Lifecycle.ACTIVE, oldVersion));
		assertThrows(IllegalArgumentException.class,
			() -> PlayerQuestGraphStateCodec.decode(1, 1, 0, "start", null, Lifecycle.ACTIVE, unknownStatus));
		assertThrows(IllegalArgumentException.class,
			() -> PlayerQuestGraphStateCodec.decode(1, 1, 0, "start", null, Lifecycle.ACTIVE, trailing));
		assertThrows(IllegalArgumentException.class,
			() -> PlayerQuestGraphStateCodec.decode(1, 1, 0, "start", null, Lifecycle.ACTIVE,
				java.util.Arrays.copyOf(valid, valid.length - 1)));
		assertThrows(IllegalArgumentException.class,
			() -> PlayerQuestGraphStateCodec.decode(1, 1, 0, "start", null, Lifecycle.ACTIVE, duplicateVariablePayload()));
	}

	/** 验证旧 QGS4 payload 可读且 journal 物品计划默认为空。 / Verifies legacy QGS4 payloads remain readable with an empty item-plan journal. */
	@Test
	void codecReadsLegacyQgs4Journal() throws Exception {
		byte[] payload = legacyQgs4PreparedPayload();
		PlayerQuestGraphState decoded = PlayerQuestGraphStateCodec.decode(1, 1, 0, "offer", null, Lifecycle.PREPARED, payload);

		assertTrue(decoded.getJournal().getItemMutationPlans().isEmpty());
		assertEquals("legacy", decoded.getJournal().getEventId());
	}

	@Test
	void stateListKeepsStableOrderAndDeletionLedger() {
		PlayerQuestGraphStateList states = new PlayerQuestGraphStateList();
		states.addLoaded(activeState(20));
		states.addLoaded(activeState(10));

		assertEquals(List.of(10, 20), states.snapshot().stream().map(PlayerQuestGraphState::getQuestId).toList());
		assertThrows(IllegalArgumentException.class, () -> states.put(activeState(20)));
		states.put(new PlayerQuestGraphState(20, 1, 1, "next", QuestStatus.START, QuestHistory.EMPTY, null, Lifecycle.ACTIVE, Map.of(), Map.of(), null,
			Map.of(), null));
		assertEquals(1, states.get(20).getRevision());
		assertTrue(states.remove(10));
		assertFalse(states.remove(10));
		assertEquals(Set.of(10), states.deletedQuestIds());
		states.acknowledgeDeleted(Set.of(10));
		assertTrue(states.deletedQuestIds().isEmpty());
	}

	private static PlayerQuestGraphState state(Map<String, VariableValue> variables, PreparedTransition journal) {
		return new PlayerQuestGraphState(1230, 2, 3, "hunt", QuestStatus.START,
			new QuestHistory(3, 2, 1_700_000_000_000L, 1_750_000_000_000L), 77L, Lifecycle.PREPARED, variables,
			Map.of("soft", 1_900_000_000_000L, "timeout", 1_800_000_000_000L), journal,
			Map.of("escort", new CleanupLease("SPAWN", "npc:9001")), null);
	}

	private static PlayerQuestGraphState activeState(int questId) {
		return new PlayerQuestGraphState(questId, 1, 0, "start", QuestStatus.START, QuestHistory.EMPTY, null, Lifecycle.ACTIVE, Map.of(), Map.of(), null,
			Map.of(), null);
	}

	private static byte[] duplicateVariablePayload() throws Exception {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try (DataOutputStream output = new DataOutputStream(bytes)) {
			output.writeInt(0x51475334);
			output.writeByte(1);
			output.writeInt(0);
			output.writeInt(0);
			output.writeBoolean(false);
			output.writeBoolean(false);
			output.writeByte(0);
			output.writeInt(2);
			output.writeUTF("score");
			output.writeByte(1);
			output.writeInt(1);
			output.writeUTF("score");
			output.writeByte(1);
			output.writeInt(2);
			output.writeInt(0);
			output.writeBoolean(false);
			output.writeInt(0);
			output.writeBoolean(false);
		}
		return bytes.toByteArray();
	}

	private static byte[] legacyQgs5ActivePayloadWithLease() throws Exception {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try (DataOutputStream output = new DataOutputStream(bytes)) {
			output.writeInt(0x51475335);
			output.writeByte(1);
			output.writeInt(0);
			output.writeInt(0);
			output.writeBoolean(false);
			output.writeBoolean(false);
			output.writeByte(0);
			output.writeInt(0);
			output.writeInt(0);
			output.writeBoolean(false);
			output.writeInt(1);
			output.writeUTF("legacy");
			output.writeUTF("INSTANCE_SCOPED_SPAWN");
			output.writeUTF("spawner:700759:npc:216608");
			output.writeBoolean(false);
		}
		return bytes.toByteArray();
	}

	/** 创建最小旧 QGS4 PREPARED payload。 / Creates a minimal legacy QGS4 PREPARED payload. */
	private static byte[] legacyQgs4PreparedPayload() throws Exception {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try (DataOutputStream output = new DataOutputStream(bytes)) {
			output.writeInt(0x51475334);
			output.writeByte(0);
			output.writeInt(0);
			output.writeInt(0);
			output.writeBoolean(false);
			output.writeBoolean(false);
			output.writeByte(0);
			output.writeInt(0);
			output.writeInt(0);
			output.writeBoolean(true);
			output.writeLong(-1);
			output.writeUTF("legacy");
			output.writeUTF("accept");
			output.writeInt(0);
			output.writeByte(0);
			output.writeInt(0);
			output.writeInt(0);
			output.writeBoolean(false);
		}
		return bytes.toByteArray();
	}
}
