package com.aionemu.gameserver.ai;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RetailGroupControlEngineTest {

	@Test
	void confirmsLeavingOnlyAfterTwoRefreshesAndCancelsWhenPlayerReturns() {
		var first = RetailGroupControlEngine.categorize(Set.of(4), Set.of(1, 4), Set.of(1, 2, 3), Set.of(3));
		assertEquals(Set.of(4), first.entrants());
		assertEquals(Set.of(3), first.confirmedLeaves());
		assertEquals(Set.of(2), first.pendingLeaves());

		var returned = RetailGroupControlEngine.categorize(Set.of(), Set.of(2), Set.of(2), first.pendingLeaves());
		assertEquals(Set.of(), returned.confirmedLeaves());
		assertEquals(Set.of(), returned.pendingLeaves());
	}
}
