package com.aionemu.gameserver.ai.worlds.reshanta.abyssLanding;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmpyreanBlessingAI2Test {
	private static final Path SOURCE = Path.of(
		"src/main/java/com/aionemu/gameserver/ai/worlds/reshanta/abyssLanding/Empyrean_BlessingAI2.java");

	@Test
	void mapsEachAbyssLandingSpringToItsBlessing() {
		assertEquals(22742, Empyrean_BlessingAI2.getBlessingSkillId(883956));
		assertEquals(22741, Empyrean_BlessingAI2.getBlessingSkillId(883957));
		assertEquals(22740, Empyrean_BlessingAI2.getBlessingSkillId(883958));
		assertEquals(22739, Empyrean_BlessingAI2.getBlessingSkillId(883959));
		assertEquals(22742, Empyrean_BlessingAI2.getBlessingSkillId(883960));
		assertEquals(22741, Empyrean_BlessingAI2.getBlessingSkillId(883961));
		assertEquals(22740, Empyrean_BlessingAI2.getBlessingSkillId(883962));
		assertEquals(22739, Empyrean_BlessingAI2.getBlessingSkillId(883963));
		assertEquals(0, Empyrean_BlessingAI2.getBlessingSkillId(0));
	}

	@Test
	void appliesBlessingDirectlyInsteadOfMakingThePlayerCastIt() throws IOException {
		String source = Files.readString(SOURCE);

		assertTrue(source.contains("applyEffectDirectly(skillId, getOwner(), player, 0)"));
		assertFalse(source.contains("getSkill(player, skillId"));
	}
}
