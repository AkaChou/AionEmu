package com.aionemu.gameserver.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import com.aionemu.gameserver.model.rift.RiftLocation;
import org.junit.jupiter.api.Test;

class RiftServiceTest {

	@Test
	void storesActiveRiftsInJdkMap() throws Exception {
		RiftService service = new RiftService();

		Map<Integer, RiftLocation> activeRifts = activeRifts(service);

		assertEquals(HashMap.class, activeRifts.getClass());
	}

	@Test
	void closeRiftsToleratesActiveRiftsChangingDuringClose() throws Exception {
		RiftService service = new RiftService();
		Map<Integer, RiftLocation> activeRifts = new LinkedHashMap<>();
		activeRifts.put(1, new MutatingRiftLocation(activeRifts));
		activeRifts.put(2, new RiftLocation());
		setActiveRifts(service, activeRifts);

		assertDoesNotThrow(() -> service.closeRifts());
	}

	@SuppressWarnings("unchecked")
	private Map<Integer, RiftLocation> activeRifts(RiftService service) throws Exception {
		Field field = RiftService.class.getDeclaredField("activeRifts");
		field.setAccessible(true);
		return (Map<Integer, RiftLocation>) field.get(service);
	}

	private void setActiveRifts(RiftService service, Map<Integer, RiftLocation> activeRifts) throws Exception {
		Field field = RiftService.class.getDeclaredField("activeRifts");
		field.setAccessible(true);
		field.set(service, activeRifts);
	}

	private static final class MutatingRiftLocation extends RiftLocation {
		private final Map<Integer, RiftLocation> activeRifts;
		private boolean mutated;

		private MutatingRiftLocation(Map<Integer, RiftLocation> activeRifts) {
			this.activeRifts = activeRifts;
		}

		@Override
		public void setOpened(boolean state) {
			super.setOpened(state);
			if (!mutated) {
				mutated = true;
				activeRifts.put(99, new RiftLocation());
			}
		}
	}
}
