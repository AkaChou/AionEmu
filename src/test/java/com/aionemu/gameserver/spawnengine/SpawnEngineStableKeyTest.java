package com.aionemu.gameserver.spawnengine;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;

class SpawnEngineStableKeyTest {

	@Test
	void assignsEntityKeyBeforeDeterministicStaticFallback() {
		SpawnTemplate entity = SpawnEngine.createSpawnTemplate(300540000, 231130, 1, 2, 3, (byte) 4);
		entity.setEntityId(77);
		SpawnEngine.assignStableKey(entity, 5, 6);

		SpawnTemplate fallback = SpawnEngine.createSpawnTemplate(300540000, 231130, 1, 2, 3, (byte) 4);
		SpawnEngine.assignStableKey(fallback, 5, 6);

		assertEquals("entity:77", entity.getStableKey());
		assertEquals("static:5:6", fallback.getStableKey());
	}
}
