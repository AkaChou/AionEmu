package com.aionemu.gameserver.world.zone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

class ZoneNameConcurrencyTest {

	@Test
	void zoneNameRegistryUsesConcurrentStorageForParallelGeoLoading() throws Exception {
		Field zoneNames = ZoneName.class.getDeclaredField("zoneNames");
		zoneNames.setAccessible(true);

		assertInstanceOf(ConcurrentHashMap.class, zoneNames.get(null));
	}

	@Test
	void unknownZoneIdFallsBackToNoneDuringConcurrentZoneCreation() {
		int noneId = ZoneName.getId(ZoneName.NONE);
		AtomicReference<Throwable> failure = new AtomicReference<>();

		IntStream.range(0, 8).parallel().forEach(worker -> {
			for (int i = 0; i < 10_000; i++) {
				try {
					ZoneName.createOrGet("material_zone_" + worker + "_" + i);
					assertEquals(noneId, ZoneName.getId("missing_zone_" + worker + "_" + i));
				} catch (Throwable t) {
					failure.compareAndSet(null, t);
					return;
				}
			}
		});

		if (failure.get() != null) {
			throw new AssertionError("ZoneName registry failed under concurrent access", failure.get());
		}
	}
}
