package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QuestOwnerSwitchGateTest {
	@Test
	void anyOpenGlobalGateBlocksOwnerSwitch() {
		QuestOwnerSwitchGate.Inputs inputs = new QuestOwnerSwitchGate.Inputs(100, 0, 0, 0,
				true, true, false, 0, 0, 0);
		assertEquals("PRODUCTION_OWNER_SWITCH_BLOCKED", assertThrows(QuestCompilationException.class,
				() -> QuestOwnerSwitchGate.requireReady(inputs)).code());
	}

	@Test
	void onlyFullyClosedCandidateStateIsReadyAndStillHasSwitchZero() {
		QuestOwnerSwitchGate.Inputs inputs = new QuestOwnerSwitchGate.Inputs(100, 0, 0, 0,
				true, true, true, 0, 0, 0);
		assertDoesNotThrow(() -> QuestOwnerSwitchGate.requireReady(inputs));
	}
}
