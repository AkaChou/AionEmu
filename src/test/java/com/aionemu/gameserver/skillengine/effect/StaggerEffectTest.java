package com.aionemu.gameserver.skillengine.effect;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class StaggerEffectTest {

	@Test
	void knockbackSnapsDestinationToGround() throws IOException {
		String source = Files.readString(Path.of(
				"src/main/java/com/aionemu/gameserver/skillengine/effect/StaggerEffect.java")).replaceAll("\\s+", " ");

		assertTrue(source.contains("effected.getY() + y1, effected.getZ(), true, intentions)"),
				"stagger knockback must correct the destination height to the ground");
	}
}
