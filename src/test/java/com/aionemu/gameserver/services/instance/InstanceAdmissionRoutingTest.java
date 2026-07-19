package com.aionemu.gameserver.services.instance;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

class InstanceAdmissionRoutingTest {
	private static final Path SOURCES = Path.of("src/main/java/com/aionemu/gameserver");

	@Test
	void playerFacingEntrancesCannotCreateInstancesDirectly() throws IOException {
		for (Path root : List.of(SOURCES.resolve("quest"), SOURCES.resolve("ai/event"))) {
			try (var files = Files.walk(root)) {
				for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
					assertFalse(Files.readString(file).contains("getNextAvailableInstance("), file.toString());
				}
			}
		}
		for (Path file : List.of(
				SOURCES.resolve("services/teleport/TeleportService2.java"),
				SOURCES.resolve("commands/player/cmd_pvp.java"),
				SOURCES.resolve("commands/player/cmd_siege.java"))) {
			assertFalse(Files.readString(file).contains("getNextAvailableInstance("), file.toString());
		}
	}

	@Test
	void personalTransferOwnsAdmissionAndRollback() throws IOException {
		String admission = Files.readString(SOURCES.resolve("services/instance/InstanceAdmissionService.java"));
		String teleport = Files.readString(SOURCES.resolve("services/teleport/TeleportService2.java"));
		assertTrue(admission.contains("Admission admitPersonal(Player player, int worldId)"));
		assertTrue(teleport.contains("InstanceAdmissionService.admitPersonal(player, worldId)"));
		assertTrue(teleport.contains("admission.rollback()"));
	}
}
