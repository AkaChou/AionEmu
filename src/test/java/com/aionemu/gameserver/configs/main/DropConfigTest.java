package com.aionemu.gameserver.configs.main;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.aionemu.commons.configuration.ConfigurableProcessor;

class DropConfigTest {

	@AfterEach
	void resetDefaults() {
		ConfigurableProcessor.process(DropConfig.class, new Properties());
		DropConfig.refresh();
	}

	@Test
	void bindsPositiveKinahRate() {
		Properties properties = new Properties();
		properties.setProperty("gameserver.drop.kinah.rate", "30");

		ConfigurableProcessor.process(DropConfig.class, properties);
		DropConfig.refresh();

		assertEquals(30f, DropConfig.KINAH_RATE);
	}

	@Test
	void rejectsNonPositiveAndNonFiniteKinahRates() {
		DropConfig.KINAH_RATE = 0f;
		assertThrows(IllegalArgumentException.class, DropConfig::refresh);
		DropConfig.KINAH_RATE = -1f;
		assertThrows(IllegalArgumentException.class, DropConfig::refresh);
		DropConfig.KINAH_RATE = Float.NaN;
		assertThrows(IllegalArgumentException.class, DropConfig::refresh);
		DropConfig.KINAH_RATE = Float.POSITIVE_INFINITY;
		assertThrows(IllegalArgumentException.class, DropConfig::refresh);
	}

	@Test
	void rejectsNonNumericKinahRate() {
		Properties properties = new Properties();
		properties.setProperty("gameserver.drop.kinah.rate", "invalid");

		assertThrows(RuntimeException.class, () -> ConfigurableProcessor.process(DropConfig.class, properties));
	}

	@Test
	void dropPropertiesOwnAllDropRates() throws Exception {
		Properties properties = new Properties();
		try (InputStream in = Files.newInputStream(Path.of("src/main/resources/aion/config/main/drop.properties"))) {
			properties.load(in);
		}

		assertTrue(Boolean.parseBoolean(properties.getProperty("gameserver.drop.enable.global.drops")));
		assertTrue(Float.parseFloat(properties.getProperty("gameserver.drop.kinah.rate")) > 0f);
		assertEquals("30", properties.getProperty("gameserver.rate.regular.drop"));
		assertEquals("30", properties.getProperty("gameserver.rate.premium.drop"));
		assertEquals("30", properties.getProperty("gameserver.rate.vip.drop"));

		Properties rates = new Properties();
		try (InputStream in = Files.newInputStream(Path.of("src/main/resources/aion/config/main/rates.properties"))) {
			rates.load(in);
		}
		assertFalse(rates.containsKey("gameserver.rate.regular.drop"));
		assertFalse(rates.containsKey("gameserver.rate.premium.drop"));
		assertFalse(rates.containsKey("gameserver.rate.vip.drop"));
	}
}
