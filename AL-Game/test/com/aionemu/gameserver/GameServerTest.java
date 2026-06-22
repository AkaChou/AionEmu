package com.aionemu.gameserver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import com.aionemu.commons.configuration.ConfigurableProcessor;
import com.aionemu.gameserver.configs.main.GeoDataConfig;
import com.aionemu.gameserver.configs.main.PlayerTransferConfig;
import org.junit.jupiter.api.Test;

class GameServerTest {

	@Test
	void resolvesGameModuleConfigWhenStartedFromRepositoryRoot() throws IOException {
		File logbackConfig = GameServer.resolveRuntimeFile("config/slf4j-logback.xml", new File(".."));

		assertEquals(new File("../AL-Game/config/slf4j-logback.xml").getCanonicalFile(), logbackConfig.getCanonicalFile());
		assertTrue(logbackConfig.isFile());
	}

	@Test
	void preservesModuleDirectoryRuntimeLayout() throws IOException {
		File logbackConfig = GameServer.resolveRuntimeFile("config/slf4j-logback.xml", new File("."));

		assertEquals(new File("config/slf4j-logback.xml").getCanonicalFile(), logbackConfig.getCanonicalFile());
		assertTrue(logbackConfig.isFile());
	}

	@Test
	void loadsGeoDataDefaultsWithoutOverrideProperties() {
		assertDoesNotThrow(() -> ConfigurableProcessor.process(GeoDataConfig.class, new Properties()));
		assertFalse(GeoDataConfig.GEO_MONONO2_IN_USE);
	}

	@Test
	void loadsPlayerTransferDefaultsWithoutOverrideProperties() {
		assertDoesNotThrow(() -> ConfigurableProcessor.process(PlayerTransferConfig.class, new Properties()));
		assertEquals(0, PlayerTransferConfig.REUSE_HOURS);
	}
}
