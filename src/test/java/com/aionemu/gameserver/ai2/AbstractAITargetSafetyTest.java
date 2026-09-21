package com.aionemu.gameserver.ai2;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class AbstractAITargetSafetyTest {

	@Test
	void creatureNeedsSupportUsesCapturedTargetReference() throws IOException {
		String source = Files.readString(Path.of("src/main/java/com/aionemu/gameserver/ai2/AbstractAI.java"));

		assertFalse(source.contains("handleGuardAgainstAttacker((Creature) creature.getTarget())"),
				"AbstractAI must not re-read creature.getTarget() after pattern-matching validation");
		assertTrue(source.contains("creature.getTarget() instanceof Creature targetCreature"),
				"AbstractAI should pattern-match creature.getTarget() to avoid race conditions");
	}
}
