package com.aionemu.gameserver.configs.main;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.aionemu.commons.configuration.ConfigurableProcessor;

class SkillConfigTest {

	@AfterEach
	void resetDefaults() {
		ConfigurableProcessor.process(SkillConfig.class, new Properties());
		SkillConfig.refresh();
	}

	@Test
	void bindsSkillSettingsAndScalesCooldown() {
		Properties properties = new Properties();
		properties.setProperty("gameserver.skill.cooldown.multiplier", "0.01");
		properties.setProperty("gameserver.skill.dp.consume", "false");
		properties.setProperty("gameserver.magicboost.cap", "4000");

		ConfigurableProcessor.process(SkillConfig.class, properties);
		SkillConfig.refresh();

		assertEquals(30, SkillConfig.scaleCooldown(3000));
		assertFalse(SkillConfig.CONSUME_DP);
		assertEquals(4000, SkillConfig.MAGICBOOST_CAP);
	}

	@Test
	void rejectsCooldownMultiplierOutsideSupportedRange() {
		SkillConfig.COOLDOWN_MULTIPLIER = 0.009;
		assertThrows(IllegalArgumentException.class, SkillConfig::refresh);
		SkillConfig.COOLDOWN_MULTIPLIER = 1.01;
		assertThrows(IllegalArgumentException.class, SkillConfig::refresh);
	}

	@Test
	void skillPropertiesDocumentsDefaults() throws Exception {
		Properties properties = new Properties();
		try (InputStream in = Files.newInputStream(Path.of("src/main/resources/aion/config/main/skill.properties"))) {
			properties.load(in);
		}

		assertEquals("1", properties.getProperty("gameserver.skill.cooldown.multiplier"));
		assertEquals("true", properties.getProperty("gameserver.skill.dp.consume"));
		assertEquals("6500", properties.getProperty("gameserver.magicboost.cap"));
	}
}
