package com.aionemu.gameserver.model.templates.spawns;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SpawnTemplateStaticIdTest {

	@Test
	void staticIdUsesEntityId() {
		SpawnTemplate spawn = new SpawnTemplate(new SpawnGroup2(1001, 2001), 1, 2, 3, (byte) 4, 0, null, 123, 0);

		assertEquals(123, spawn.getStaticId());
		spawn.setStaticId(456);
		assertEquals(456, spawn.getEntityId());
	}
}
