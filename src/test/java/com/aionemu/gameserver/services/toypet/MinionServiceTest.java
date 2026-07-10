package com.aionemu.gameserver.services.toypet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class MinionServiceTest {
	@Test
	void rejectsMinionsAtAndAboveTheLimit() {
		assertFalse(MinionService.isMinionLimitReached(199));
		assertTrue(MinionService.isMinionLimitReached(200));
		assertTrue(MinionService.isMinionLimitReached(256));
	}

	@Test
	void chargesOnlyForMissingSkillPoints() {
		assertEquals(1_000_000, MinionService.chargePrice(0));
		assertEquals(960, MinionService.chargePrice(49_952));
		assertEquals(0, MinionService.chargePrice(50_000));
	}

	@Test
	void consumesConfiguredEnergyBeforeApplyingSkillActions() throws Exception {
		String service = Files.readString(Path.of("src/main/java/com/aionemu/gameserver/services/toypet/MinionService.java"));
		assertTrue(service.contains("int energyCost = minionSkill.getEnergyCost()"));
		assertTrue(service.contains("player.setMinionSkillPoints(currentSkillPoints - energyCost)"));
		assertTrue(service.contains("setMinionSkillPointsAutoCharge(autoCharge)"));

		String skill = Files.readString(Path.of("src/main/java/com/aionemu/gameserver/skillengine/model/Skill.java"));
		int usageCheck = skill.indexOf("if (!preUsageCheck())");
		int energyUse = skill.indexOf("consumeMinionSkillPoints", usageCheck);
		int actions = skill.indexOf("Actions skillActions", energyUse);
		assertTrue(usageCheck < energyUse && energyUse < actions);
	}
}
