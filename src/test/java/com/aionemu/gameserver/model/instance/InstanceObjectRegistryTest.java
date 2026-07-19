package com.aionemu.gameserver.model.instance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class InstanceObjectRegistryTest {

	@Test
	void createsStrictStableEntityKeys() {
		assertEquals("entity:42", InstanceObjectRegistry.entityKey(42));
		assertThrows(IllegalArgumentException.class, () -> InstanceObjectRegistry.entityKey(0));
	}
}
