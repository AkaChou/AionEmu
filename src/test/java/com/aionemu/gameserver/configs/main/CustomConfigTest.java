package com.aionemu.gameserver.configs.main;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import com.aionemu.commons.configuration.ConfigurableProcessor;
import org.junit.jupiter.api.Test;

class CustomConfigTest {

	@Test
	void loadsMagicBoostCapDefault() {
		ConfigurableProcessor.process(CustomConfig.class, new Properties());

		assertEquals(6500, CustomConfig.MAGICBOOST_CAP);
	}

	@Test
	void loadsMagicBoostCapOverride() {
		Properties properties = new Properties();
		properties.setProperty("gameserver.magicboost.cap", "4000");

		ConfigurableProcessor.process(CustomConfig.class, properties);

		assertEquals(4000, CustomConfig.MAGICBOOST_CAP);
	}

	@Test
	void customPropertiesDocumentsMagicBoostCap() throws IOException {
		Properties properties = new Properties();
		try (InputStream in = Files.newInputStream(Path.of("src/main/resources/aion/config/main/custom.properties"))) {
			properties.load(in);
		}

		assertEquals("6500", properties.getProperty("gameserver.magicboost.cap"));
	}

	@Test
	void loadsExpressMailCooldown() {
		ConfigurableProcessor.process(CustomConfig.class, new Properties());
		assertEquals(60, CustomConfig.EXPRESS_MAIL_COOLDOWN_SECONDS);

		Properties properties = new Properties();
		properties.setProperty("gameserver.express.mail.cooldown_seconds", "30");
		ConfigurableProcessor.process(CustomConfig.class, properties);

		assertEquals(30, CustomConfig.EXPRESS_MAIL_COOLDOWN_SECONDS);
	}

	@Test
	void customPropertiesDocumentsExpressMailCooldown() throws IOException {
		Properties properties = new Properties();
		try (InputStream in = Files.newInputStream(Path.of("src/main/resources/aion/config/main/custom.properties"))) {
			properties.load(in);
		}

		assertEquals("60", properties.getProperty("gameserver.express.mail.cooldown_seconds"));
	}
}
