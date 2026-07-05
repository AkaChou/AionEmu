package com.aionemu.gameserver.controllers;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.ConcurrentMap;
import org.junit.jupiter.api.Test;

class HouseControllerTest {

	@Test
	void observedPlayersUseConcurrentMapForAsyncAppearanceTasks() {
		HouseController controller = new HouseController();

		assertTrue(controller.observed instanceof ConcurrentMap);
	}
}
