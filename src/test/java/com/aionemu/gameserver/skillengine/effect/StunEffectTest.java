package com.aionemu.gameserver.skillengine.effect;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

class StunEffectTest {

	@Test
	void npcControlEffectsKeepMovementTaskRegistered() throws IOException {
		for (String effect : List.of("StunEffect", "StaggerEffect", "StumbleEffect", "SpinEffect", "RootEffect",
				"OpenAerialEffect", "ParalyzeEffect", "PetrificationEffect", "SleepEffect", "SnareEffect")) {
			String source = Files.readString(Path.of(
					"src/main/java/com/aionemu/gameserver/skillengine/effect/" + effect + ".java")).replaceAll("\\s+", " ");

			assertTrue(source.contains("if (!(effected instanceof Npc)) { effected.getMoveController().abortMove(); }"),
					effect + " must retain the NPC movement task so pursuit resumes when the effect ends");
		}
	}

	@Test
	void moveBehindDoesNotAbortTargetMovement() throws IOException {
		String source = Files.readString(Path.of(
				"src/main/java/com/aionemu/gameserver/skillengine/effect/MoveBehindEffect.java"));

		assertFalse(source.contains("effected.getMoveController().abortMove();"),
				"move-behind relocates the effector and must not abort the target movement");
	}
}
