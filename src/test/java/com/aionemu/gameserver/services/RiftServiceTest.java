package com.aionemu.gameserver.services;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.util.HashMap;
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

	@SuppressWarnings("unchecked")
	private Map<Integer, RiftLocation> activeRifts(RiftService service) throws Exception {
		Field field = RiftService.class.getDeclaredField("activeRifts");
		field.setAccessible(true);
		return (Map<Integer, RiftLocation>) field.get(service);
	}
}
