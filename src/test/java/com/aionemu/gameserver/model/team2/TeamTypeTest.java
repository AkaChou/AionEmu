package com.aionemu.gameserver.model.team2;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TeamTypeTest {

	@Test
	void keepsLastInAreaMemberButStillDisbandsEmptyTeam() {
		for (TeamType type : TeamType.values()) {
			assertTrue(type.shouldDisband(0));
			if (type.isInArea()) {
				assertFalse(type.shouldDisband(1), type.name());
			}
		}
		assertTrue(TeamType.GROUP.shouldDisband(1));
		assertTrue(TeamType.ALLIANCE.shouldDisband(1));
		assertFalse(TeamType.GROUP.shouldDisband(2));
	}
}
