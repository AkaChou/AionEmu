package com.aionemu.gameserver.services.instance;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class RetailMatchAdmissionPersistenceTest {

	@Test
	void reservesAndRestoresEntryCountInMemberTransaction() throws IOException {
		String admission = source("services/instance/InstanceAdmissionService.java");
		String manager = source("services/instance/DynamicInstanceManager.java");
		String dao = source("dao/impl/DynamicInstancesDAO.java");
		String matchmaking = source("services/RetailMatchmakingService.java");

		assertTrue(admission.contains("InstanceLimitService.reserveMatch(instance, player"));
		assertTrue(manager.contains("dao().saveMatchReservation("));
		assertTrue(dao.contains("INSERT_MATCH_RESERVATION"));
		assertTrue(dao.contains("SELECT entry_limit_key,entry_consumed"));
		assertTrue(dao.contains("SET used=GREATEST(0,used-1)"));
		assertTrue(matchmaking.contains("cancelMatchReservation(match.instance, member.playerId())"));
	}

	private static String source(String relative) throws IOException {
		return Files.readString(Path.of("src/main/java/com/aionemu/gameserver").resolve(relative));
	}
}
