package com.aionemu.gameserver.model.stats.container;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class NpcGameStatsTest {

	@Test
	void skillDelayUsesOneFixedDeadline() {
		assertFalse(NpcGameStats.isSkillDelayElapsed(6_999, 7_000));
		assertTrue(NpcGameStats.isSkillDelayElapsed(7_000, 7_000));
	}
}
