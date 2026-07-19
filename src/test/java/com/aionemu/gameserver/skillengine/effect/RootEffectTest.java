package com.aionemu.gameserver.skillengine.effect;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.controllers.observer.ObserverType;

class RootEffectTest {

	@Test
	void directAndPeriodicDamageCanBreakRoot() throws IOException {
		assertTrue(ObserverType.ATTACKED_OR_DOT.matchesObserver(ObserverType.ATTACKED));
		assertTrue(ObserverType.ATTACKED_OR_DOT.matchesObserver(ObserverType.DOT_ATTACKED));
		assertFalse(ObserverType.ATTACKED_OR_DOT.matchesObserver(ObserverType.MOVE));

		String source = Files.readString(Path.of(
			"src/main/java/com/aionemu/gameserver/skillengine/effect/RootEffect.java"));
		assertTrue(source.contains("SkillConfig.ROOT_BREAK_ON_DOT ? ObserverType.ATTACKED_OR_DOT : ObserverType.ATTACKED"));
		assertTrue(source.contains("public void dotattacked(Creature creature, Effect dotEffect)"));
		assertFalse(source.contains("Rnd."));
	}
}
