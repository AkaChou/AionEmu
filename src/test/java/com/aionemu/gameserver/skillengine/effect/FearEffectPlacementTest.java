package com.aionemu.gameserver.skillengine.effect;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class FearEffectPlacementTest {

	@Test
	void fearOwnsGlidingTransition() throws Exception {
		String effect = Files.readString(Path.of("src/main/java/com/aionemu/gameserver/skillengine/model/Effect.java"));
		String fear = Files.readString(Path.of("src/main/java/com/aionemu/gameserver/skillengine/effect/FearEffect.java"));

		assertFalse(effect.contains("if (isFearEffect())"));
		assertTrue(fear.contains("getFlyController().onStopGliding(true)"));
	}
}
