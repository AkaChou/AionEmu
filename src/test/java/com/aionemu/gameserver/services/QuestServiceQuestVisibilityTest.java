package com.aionemu.gameserver.services;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class QuestServiceQuestVisibilityTest {

	@Test
	void displayMayIgnoreMaximumLevelWithoutChangingStrictStartDefault() {
		assertFalse(QuestService.isMaxLevelPermitted(50, 51, false));
		assertTrue(QuestService.isMaxLevelPermitted(50, 51, true));
		assertTrue(QuestService.isMaxLevelPermitted(50, 50, false));
		assertTrue(QuestService.isMaxLevelPermitted(0, 100, false));
	}
}
