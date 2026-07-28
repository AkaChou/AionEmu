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

import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.QuestStatus;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.BooleanValue;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.CleanupLease;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.IntValue;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.Lifecycle;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.PreparedTransition;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.QuestHistory;
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
		PreparedTransition journal = new PreparedTransition(0, "event-9", "kill-advance", 2, new byte[] { 4, 5, 6 });
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
		assertEquals(QuestStatus.START, decoded.getQuestStatus());
		assertEquals(new QuestHistory(3, 2, 1_700_000_000_000L, 1_750_000_000_000L), decoded.getHistory());
		assertArrayEquals(new byte[] { 4, 5, 6 }, decoded.getJournal().getEventPayload());
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
		assertThrows(IllegalArgumentException.class, () -> new PlayerQuestGraphState(1, 1, 0, "start", QuestStatus.NONE, QuestHistory.EMPTY, null,
			Lifecycle.PREPARED, Map.of(), Map.of(), new PreparedTransition(-1, "event", "transition", 1, new byte[0]), Map.of(), null));
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
		oldVersion[3] = 0x32;
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
			output.writeInt(0x51475333);
			output.writeByte(1);
			output.writeInt(0);
			output.writeInt(0);
			output.writeBoolean(false);
			output.writeBoolean(false);
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
}
