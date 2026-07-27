package com.aionemu.gameserver.skillengine.effect;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class SimpleRootEffectTest {

	@Test
	void knockbackUsesGroundAwareMovementCollision() throws IOException {
		String source = Files.readString(Path.of(
				"src/main/java/com/aionemu/gameserver/skillengine/effect/SimpleRootEffect.java")).replaceAll("\\s+", " ");

		assertTrue(source.contains("PositionUtil.getMoveAwayHeading(effector, effected)"));
		assertTrue(source.contains("findMovementCollision(effected, MathUtil.convertHeadingToDegree(moveAwayHeading), 0.7f)"));
		assertTrue(source.contains("effect.setTargetLoc(closestCollision.x, closestCollision.y, closestCollision.z)"));
		assertFalse(source.contains("getClosestCollision(effected"));
	}
}
