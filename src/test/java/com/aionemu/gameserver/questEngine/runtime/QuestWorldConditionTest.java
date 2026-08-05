package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.ProgressLayout;
import com.aionemu.gameserver.questEngine.definition.QuestCondition;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestWorldConditionTest {
	@Test
	void matchesCapturedWorldAndFailsClosedWhenPositionIsUnknown() {
		QuestCondition inWorld = new QuestCondition.WorldIs(220020000, true);
		QuestCondition outsideWorld = new QuestCondition.WorldIs(220020000, false);
		QuestSnapshot inside = snapshot(220020000);
		QuestSnapshot outside = snapshot(220030000);
		QuestSnapshot unknown = new QuestSnapshot(7, 1001, QuestStatus.START, 0, Map.of());

		assertTrue(matches(inside, inWorld));
		assertFalse(matches(outside, inWorld));
		assertTrue(matches(outside, outsideWorld));
		assertFalse(matches(unknown, inWorld));
		assertFalse(matches(unknown, outsideWorld));
	}

	private static boolean matches(QuestSnapshot snapshot, QuestCondition condition) {
		return QuestConditionEvaluator.matches(ProgressLayout.empty(), snapshot, List.of(condition));
	}

	private static QuestSnapshot snapshot(int worldId) {
		return new QuestSnapshot(7, 1001, QuestStatus.START, 0, Map.of(), Map.of(),
			true, true, 0, 0, worldId, 1, 0f, 0f, 0f, (byte) 0, null, null);
	}
}
