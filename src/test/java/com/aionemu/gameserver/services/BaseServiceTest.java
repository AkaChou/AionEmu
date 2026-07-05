package com.aionemu.gameserver.services;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.ConcurrentMap;

import org.junit.jupiter.api.Test;

class BaseServiceTest {

	@Test
	void activeBaseIndexIsSafeForConcurrentResetAndWorldThreads() throws Exception {
		assertTrue(ConcurrentMap.class.isAssignableFrom(BaseService.class.getDeclaredField("active").getType()));
	}
}
