package com.aionemu.gameserver.network.aion.clientpackets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class CMWindstreamTest {

	@Test
	void synchronizesFlyStateBeforeSendingVisualStats() throws Exception {
		String source = Files.readString(Path.of(
				"src/main/java/com/aionemu/gameserver/network/aion/clientpackets/CM_WINDSTREAM.java"));

		assertTrue(source.contains("player.setState(CreatureState.FLYING);\n\t\t\tplayer.setFlyState(1);"));
		int flyStateSync = source.indexOf("player.setFlyState(player.isInState(CreatureState.GLIDING) ? 2 : 0);");
		assertTrue(flyStateSync >= 0);
		assertTrue(flyStateSync < source.indexOf("player.getGameStats().updateStatsAndSpeedVisually();"));
		assertTrue(source.contains("player.isUsingFlyTeleport() || player.isInPlayerMode(PlayerMode.WINDSTREAM) || !player.isFlying()"));
		assertTrue(source.contains("ENTER_MAX_DISTANCE = 45"));
		assertTrue(source.indexOf("route.contains(distance, player.getX(), player.getY(), player.getZ(), ENTER_MAX_DISTANCE)")
			< source.indexOf("case 1:"));
		assertTrue(source.contains("pendingPath.teleportId != teleportId || pendingPath.distance != distance"));
		assertTrue(source.contains("new SM_WINDSTREAM(state, 0)"));
		assertFalse(source.contains("isStrongWind"));
		assertFalse(Files.readString(Path.of(
				"src/main/java/com/aionemu/gameserver/network/aion/clientpackets/CM_MOVE_IN_AIR.java")).contains(
				"windstreamPath.accepts("));
		assertTrue(Files.readString(Path.of(
				"src/main/java/com/aionemu/gameserver/controllers/PlayerController.java")).contains(
				"if (player.isInPlayerMode(PlayerMode.WINDSTREAM)) {\n\t\t\tplayer.unsetPlayerMode(PlayerMode.WINDSTREAM);"));
	}
}
