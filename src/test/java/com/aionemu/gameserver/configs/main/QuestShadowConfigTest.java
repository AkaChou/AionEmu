package com.aionemu.gameserver.configs.main;

import com.aionemu.commons.configuration.ConfigurableProcessor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestShadowConfigTest {
	@AfterEach
	void restoreDefaults() {
		ConfigurableProcessor.process(QuestShadowConfig.class, new Properties());
	}

	@Test
	void defaultsAreDisabledAndProductionPropertiesDocumentTheSameValues() throws Exception {
		ConfigurableProcessor.process(QuestShadowConfig.class, new Properties());

		assertFalse(QuestShadowConfig.ENABLED);
		assertEquals(Path.of("log/quest-shadow/unified-shadow-report.json"), QuestShadowConfig.reportPath());
		assertEquals(300_000L, QuestShadowConfig.persistIntervalMillis());

		Properties documented = new Properties();
		try (InputStream input = Files.newInputStream(
				Path.of("src/main/resources/aion/config/main/quest-shadow.properties"))) {
			documented.load(input);
		}
		assertEquals("false", documented.getProperty("gameserver.quest.shadow.enable"));
		assertEquals("./log/quest-shadow/unified-shadow-report.json",
			documented.getProperty("gameserver.quest.shadow.report.path"));
		assertEquals("300", documented.getProperty("gameserver.quest.shadow.persist.interval.seconds"));
	}

	@Test
	void enabledValuesBindAndInvalidRuntimeValuesFailClosed() {
		Properties properties = new Properties();
		properties.setProperty("gameserver.quest.shadow.enable", "true");
		properties.setProperty("gameserver.quest.shadow.report.path", "target/shadow/report.json");
		properties.setProperty("gameserver.quest.shadow.persist.interval.seconds", "15");
		ConfigurableProcessor.process(QuestShadowConfig.class, properties);

		assertTrue(QuestShadowConfig.ENABLED);
		assertEquals(Path.of("target/shadow/report.json"), QuestShadowConfig.reportPath());
		assertEquals(15_000L, QuestShadowConfig.persistIntervalMillis());

		QuestShadowConfig.REPORT_PATH = " ";
		QuestShadowConfig.PERSIST_INTERVAL_SECONDS = 0;
		assertThrows(IllegalStateException.class, QuestShadowConfig::reportPath);
		assertThrows(IllegalStateException.class, QuestShadowConfig::persistIntervalMillis);
	}
}
