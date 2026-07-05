package com.aionemu.gameserver.services;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.ConcurrentMap;

import org.junit.jupiter.api.Test;

class OutpostServiceTest {

	@Test
	void activeOutpostIndexIsSafeForCronAndWorldThreads() throws Exception {
		assertTrue(ConcurrentMap.class.isAssignableFrom(OutpostService.class.getDeclaredField("active").getType()));
	}
}
