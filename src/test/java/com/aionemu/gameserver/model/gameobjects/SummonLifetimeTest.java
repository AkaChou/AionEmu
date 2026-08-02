package com.aionemu.gameserver.model.gameobjects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

class SummonLifetimeTest {

	@Test
	void permanentSummonNeverExpires() {
		Summon summon = summonWithLiveTime(0, 1_000_000_000L);

		assertEquals(0, summon.getLiveTime(Long.MAX_VALUE));
		assertFalse(summon.isExpired(Long.MAX_VALUE));
	}

	@Test
	void timedSummonKeepsOnlyItsRemainingLifetime() {
		long startedAt = 1_000_000_000L;
		Summon summon = summonWithLiveTime(10, startedAt);

		assertEquals(10, summon.getLiveTime(startedAt));
		assertEquals(10, summon.getLiveTime(startedAt + 1));
		assertEquals(1, summon.getLiveTime(startedAt + 9_999_999_999L));
		assertEquals(0, summon.getLiveTime(startedAt + 10_000_000_000L));
		assertTrue(summon.isExpired(startedAt + 10_000_000_000L));
	}

	private static Summon summonWithLiveTime(int liveTime, long currentTimeNanos) {
		Summon summon = new ObjenesisStd().newInstance(Summon.class);
		summon.setLiveTime(liveTime, currentTimeNanos);
		return summon;
	}
}
