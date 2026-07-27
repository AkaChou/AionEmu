package com.aionemu.gameserver.network.aion.clientpackets;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class CMMoveControlEffectTest {

	@Test
	void ignoresClientMovementWhileFearOrConfuseControlsThePlayer() throws Exception {
		String source = Files.readString(Path.of(
				"src/main/java/com/aionemu/gameserver/network/aion/clientpackets/CM_MOVE.java"));

		assertTrue(source.contains(
				"player.getEffectController().isUnderFear() || player.getEffectController().isConfused()"));
	}
}
