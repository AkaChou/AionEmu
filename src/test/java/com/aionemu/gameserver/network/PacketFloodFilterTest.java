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
		String oldConfigDir = System.getProperty("aion.config.dir");
		boolean oldPffEnable = SecurityConfig.PFF_ENABLE;
		try {
			Path administrationDir = configDir.resolve("administration");
			Files.createDirectories(administrationDir);
			Files.writeString(administrationDir.resolve("pff.properties"), "0x01 = 7\n");
			System.setProperty("aion.config.dir", configDir.toString());
			SecurityConfig.PFF_ENABLE = true;

			PacketFloodFilter packetFloodFilter = new PacketFloodFilter();

			assertEquals(7, packetFloodFilter.getPackets()[1]);
		} finally {
			SecurityConfig.PFF_ENABLE = oldPffEnable;
			if (oldConfigDir == null) {
				System.clearProperty("aion.config.dir");
			} else {
				System.setProperty("aion.config.dir", oldConfigDir);
			}
		}
	}

	@Test
	void reloadsRulesAndKeepsPacketsAvailableWhenDisabled() throws IOException {
		String oldConfigDir = System.getProperty("aion.config.dir");
		boolean oldPffEnable = SecurityConfig.PFF_ENABLE;
		try {
			Path administrationDir = configDir.resolve("administration");
			Files.createDirectories(administrationDir);
			Path rules = administrationDir.resolve("pff.properties");
			Files.writeString(rules, "0x01 = 7\n");
			System.setProperty("aion.config.dir", configDir.toString());
			SecurityConfig.PFF_ENABLE = true;

			PacketFloodFilter packetFloodFilter = new PacketFloodFilter();
			Files.writeString(rules, "0x01 = 11\n");
			packetFloodFilter.reload();
			assertEquals(11, packetFloodFilter.getPackets()[1]);

			Files.delete(rules);
			packetFloodFilter.reload();
			assertEquals(11, packetFloodFilter.getPackets()[1]);

			SecurityConfig.PFF_ENABLE = false;
			packetFloodFilter.reload();
			assertEquals(0, packetFloodFilter.getPackets()[1]);
		} finally {
			SecurityConfig.PFF_ENABLE = oldPffEnable;
			if (oldConfigDir == null) {
				System.clearProperty("aion.config.dir");
			} else {
				System.setProperty("aion.config.dir", oldConfigDir);
			}
		}
	}
}
