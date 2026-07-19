package com.aionemu.gameserver.services;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class FindGroupProtocolTest {
	@Test
	void implementsRetailInstanceGroupActions() throws IOException {
		String client = Files.readString(Path.of(
				"src/main/java/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java"));
		String server = Files.readString(Path.of(
				"src/main/java/com/aionemu/gameserver/network/aion/serverpackets/SM_FIND_GROUP.java"));
		String service = Files.readString(Path.of(
				"src/main/java/com/aionemu/gameserver/services/FindGroupService.java"));
		for (String action : new String[] { "0x08", "0x09", "0x0A", "0x0B", "0x0C", "0x0D", "0x0F",
				"0x11", "0x14", "0x19" }) {
			assertTrue(client.contains("case " + action), action);
		}
		for (String action : new String[] { "0x0A", "0x0B", "0x0E", "0x10", "0x12", "0x16", "0x17",
				"0x18", "0x1A" }) {
			assertTrue(server.contains(action), action);
		}
		assertTrue(service.contains("EntryRequestType.GROUP_ENTRY"));
		assertTrue(service.contains("quickApply(Player player)"));
		assertTrue(service.contains("replyInstanceGroupApplication"));
		assertTrue(service.contains("definition.getTime() / 1000L"));
		assertTrue(service.contains("onLogout(Player player)"));
		assertTrue(service.contains("onTeamChanged(TemporaryPlayerTeam<?> team)"));
		assertTrue(!service.contains("new SM_FIND_GROUP(0x16, group)"));
		String matchmaking = Files.readString(Path.of(
				"src/main/java/com/aionemu/gameserver/services/RetailMatchmakingService.java"));
		assertTrue(matchmaking.contains("sendTeamMatchUpdates(match)"));
		assertTrue(matchmaking.contains("closeTeamMatchWindow"));
		assertTrue(matchmaking.contains("instanceGroupEntryId"));
	}
}
