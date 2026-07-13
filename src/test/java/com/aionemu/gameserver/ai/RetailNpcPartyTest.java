package com.aionemu.gameserver.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RetailNpcPartyTest {

	@Test
	void matchesOnlyOtherNpcInSameInstanceAndExplicitParty() {
		assertTrue(RetailNpcParty.matches(1, 10, "party-1", 2, 10, "party-1"));
		assertFalse(RetailNpcParty.matches(1, 10, "party-1", 1, 10, "party-1"));
		assertFalse(RetailNpcParty.matches(1, 10, "party-1", 2, 11, "party-1"));
		assertFalse(RetailNpcParty.matches(1, 10, "party-1", 2, 10, "party-2"));
		assertFalse(RetailNpcParty.matches(1, 10, null, 2, 10, null));
	}
}
