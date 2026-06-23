package com.aionemu.gameserver.network;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.aionemu.gameserver.configs.main.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PacketFloodFilterTest {

	@TempDir
	Path configDir;

	@Test
	void loadsRulesFromConfiguredGameConfigDirectory() throws IOException {
		String oldConfigDir = System.getProperty("aion.game.config.dir");
		boolean oldPffEnable = SecurityConfig.PFF_ENABLE;
		try {
			Path administrationDir = configDir.resolve("administration");
			Files.createDirectories(administrationDir);
			Files.writeString(administrationDir.resolve("pff.properties"), "0x01 = 7\n");
			System.setProperty("aion.game.config.dir", configDir.toString());
			SecurityConfig.PFF_ENABLE = true;

			PacketFloodFilter packetFloodFilter = new PacketFloodFilter();

			assertEquals(7, packetFloodFilter.getPackets()[1]);
		} finally {
			SecurityConfig.PFF_ENABLE = oldPffEnable;
			if (oldConfigDir == null) {
				System.clearProperty("aion.game.config.dir");
			} else {
				System.setProperty("aion.game.config.dir", oldConfigDir);
			}
		}
	}
}
