package com.aionemu.gameserver.services.teleport;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class FlightTeleportSummonRestoreTest {

	@Test
	void flightTeleportSuspendsSummonAndRestoresItAfterLanding() throws Exception {
		String teleportService = Files.readString(Path.of(
				"src/main/java/com/aionemu/gameserver/services/teleport/TeleportService2.java"));
		String flightBranch = teleportService.substring(
				teleportService.indexOf("if (location.getType() == TeleportType.FLIGHT)"),
				teleportService.indexOf("} else {", teleportService.indexOf("if (location.getType() == TeleportType.FLIGHT)")));
		assertTrue(flightBranch.contains("SummonsService.suspendForTeleport(player);"));

		String playerController = Files.readString(Path.of(
				"src/main/java/com/aionemu/gameserver/controllers/PlayerController.java"));
		String flightEnd = playerController.substring(playerController.indexOf("public void onFlyTeleportEnd()"),
				playerController.indexOf("public boolean addItems", playerController.indexOf("public void onFlyTeleportEnd()")));
		int suspend = flightEnd.indexOf("SummonsService.suspendForTeleport(player);");
		int restore = flightEnd.indexOf("SummonsService.restoreAfterTeleport(player);");
		assertTrue(suspend >= 0);
		assertTrue(suspend < restore);
	}
}
