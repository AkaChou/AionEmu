package com.aionemu.gameserver.services;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

class RetailMatchmakingLegacyCleanupTest {
	private static final Path INSTANCE_SERVICES = Path.of(
			"src/main/java/com/aionemu/gameserver/services/instance");

	@Test
	void retailDefinitionsOwnScheduleAndCooldownState() throws Exception {
		String matchmaking = Files.readString(Path.of(
				"src/main/java/com/aionemu/gameserver/services/RetailMatchmakingService.java"));
		String config = Files.readString(Path.of(
				"src/main/java/com/aionemu/gameserver/configs/main/AutoGroupConfig.java"));

		assertTrue(matchmaking.contains("startScheduleNotifications"));
		assertTrue(matchmaking.contains("MatchDefinition.all()"));
		assertTrue(matchmaking.contains("definition.isOpen()"));
		assertTrue(matchmaking.contains("InstanceLimitService.status"));
		assertFalse(config.contains(".schedule."));
		assertFalse(config.contains("playersWithCooldown"));
	}

	@Test
	void legacyBattlefieldRegistrationServicesAreDeleted() {
		for (String name : List.of("KamarBattlefieldService", "EngulfedOphidanBridgeService",
				"SuspiciousOphidanBridgeService", "IronWallWarfrontService", "IdgelDomeService",
				"IdgelDomeLandmarkService", "GrandArenaTrainingCampService", "IDRunService",
				"AsyunatarService", "DredgionService2")) {
			assertFalse(Files.exists(INSTANCE_SERVICES.resolve(name + ".java")), name);
		}
		assertFalse(Files.exists(Path.of(
				"src/main/java/com/aionemu/gameserver/lifecycle/GameBattlefieldServices.java")));
		assertFalse(Files.exists(Path.of(
				"src/main/java/com/aionemu/gameserver/lifecycle/GameBattlefieldRuntimeBridge.java")));
	}
}
