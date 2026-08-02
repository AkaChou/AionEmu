package com.aionemu.gameserver.quest.handlers.inggison;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class Quest10031TeleportTest {

	private static final Path SCRIPT = Path.of(
			"src/main/java/com/aionemu/gameserver/quest/handlers/inggison/_10031A_Risk_For_The_Obelisk.java");

	@Test
	void sibylleTeleportsToNormalInggisonAndAdvancesOnArrival() throws IOException {
		String source = Files.readString(SCRIPT).replaceAll("\\s+", " ");

		assertTrue(source.contains("player.getWorldId() == 210050000"));
		assertTrue(source.contains(
				"TeleportService2.teleportTo(player, 210050000, 1336f, 277f, 590f, (byte) 77);"));
		assertFalse(source.contains("210130000"));
	}
}
