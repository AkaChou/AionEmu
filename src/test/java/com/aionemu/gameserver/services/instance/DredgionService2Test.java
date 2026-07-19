package com.aionemu.gameserver.services.instance;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class DredgionService2Test {

	@Test
	void entryIconSyncClosesInactiveLevelBrackets() throws IOException {
		String service = Files.readString(Path.of(
				"src/main/java/com/aionemu/gameserver/services/instance/DredgionService2.java"));
		String login = Files.readString(Path.of(
				"src/main/java/com/aionemu/gameserver/services/AutoGroupService.java"));

		assertTrue(service.contains("minLevel = 46"));
		assertTrue(service.contains("if (level < 51)"));
		assertTrue(service.contains("if (maskId != activeMaskId)"));
		assertTrue(service.contains("PacketSendUtility.sendPacket(player, this.autoGroupReg[activeMaskId]);"));
		assertTrue(login.contains("GameFeatureServices.dredgionService().updateEntryIcon(player);"));
		assertFalse(login.contains("player.getLevel() > DredgionService2.minLevel"));
	}
}
