package com.aionemu.gameserver.controllers;

import java.util.concurrent.ConcurrentMap;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ControllerObservedMapConcurrencyTest {

	@Test
	void observedMapsUseConcurrentMapsForAsyncVisibilityCallbacks() {
		assertAll(
			() -> assertInstanceOf(ConcurrentMap.class, new ShieldController().observed),
			() -> assertInstanceOf(ConcurrentMap.class, new PlaceableObjectController().observed),
			() -> assertInstanceOf(ConcurrentMap.class, new FlyRingController().observed),
			() -> assertInstanceOf(ConcurrentMap.class, new RoadController().observed)
		);
	}
}
