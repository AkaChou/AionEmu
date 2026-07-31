package com.aionemu.gameserver.questEngine.graph.state;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.QuestStatus;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.ItemUseContinuationPlan;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.Lifecycle;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.PreparedTransition;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.QuestHistory;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.RepeatDeadlineResolution;

class PlayerQuestGraphItemUseContinuationStateTest {

	@Test
	void codecRoundTripsCanonicalItemUseContinuation() {
		ItemUseContinuationPlan plan = new ItemUseContinuationPlan(0, 182206034, 55, 3000,
			1_700_000_003_000L, 1, 3);
		PreparedTransition journal = new PreparedTransition(-1, "item-use-event", "use-item", 0, false,
			RepeatDeadlineResolution.NOT_APPLICABLE, Map.of(), Map.of(), Map.of(), Map.of(0, plan), Map.of(), new byte[0],
			new byte[] { 1, 2, 3 });
		PlayerQuestGraphState state = new PlayerQuestGraphState(1, 1, 0, "offer", QuestStatus.NONE, QuestHistory.EMPTY, null,
			Lifecycle.PREPARED, Map.of(), Map.of(), journal, Map.of(), null);

		byte[] encoded = PlayerQuestGraphStateCodec.encode(state);
		PlayerQuestGraphState decoded = PlayerQuestGraphStateCodec.decode(1, 1, 0, "offer", null, Lifecycle.PREPARED, encoded);

		assertEquals(Map.of(0, plan), decoded.getJournal().getItemUseContinuationPlans());
		assertArrayEquals(encoded, PlayerQuestGraphStateCodec.encode(decoded));
		assertThrows(UnsupportedOperationException.class,
			() -> decoded.getJournal().getItemUseContinuationPlans().clear());
		assertThrows(IllegalArgumentException.class,
			() -> PlayerQuestGraphStateCodec.decode(1, 1, 0, "offer", null, Lifecycle.PREPARED,
				Arrays.copyOf(encoded, encoded.length - 1)));
	}

	@Test
	void continuationPlanRequiresDefinitionBoundNonEmptyTail() {
		assertThrows(IllegalArgumentException.class,
			() -> new ItemUseContinuationPlan(2, 182206034, 55, 3000, 1_700_000_003_000L, 2, 3));
		assertThrows(IllegalArgumentException.class,
			() -> new ItemUseContinuationPlan(2, 182206034, 55, 3000, 1_700_000_003_000L, 3, 2));
	}
}
