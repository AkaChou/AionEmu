package com.aionemu.gameserver.skillengine.effect;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class StumbleEffectTest {

	@Test
	void npcKnockdownKeepsCurrentHeight() throws IOException {
		String source = Files.readString(Path.of(
				"src/main/java/com/aionemu/gameserver/skillengine/effect/StumbleEffect.java")).replaceAll("\\s+", " ");

		assertTrue(source.contains("effect.setTargetLoc(x1, y1, effected instanceof Npc ? effected.getZ() : closestCollision.z);"),
				"NPC knockdown must not move the monster to a lower geo layer");
	}
}
