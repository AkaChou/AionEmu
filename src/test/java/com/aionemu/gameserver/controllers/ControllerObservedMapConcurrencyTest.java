package com.aionemu.gameserver.controllers;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.ConcurrentMap;
import org.junit.jupiter.api.Test;

class ControllerObservedMapConcurrencyTest {

	@Test
	void observedMapsUseConcurrentMapsForAsyncVisibilityCallbacks() {
		assertAll(
			() -> assertTrue(new ShieldController().observed instanceof ConcurrentMap),
			() -> assertTrue(new PlaceableObjectController().observed instanceof ConcurrentMap),
			() -> assertTrue(new FlyRingController().observed instanceof ConcurrentMap),
			() -> assertTrue(new RoadController().observed instanceof ConcurrentMap)
		);
	}
}
