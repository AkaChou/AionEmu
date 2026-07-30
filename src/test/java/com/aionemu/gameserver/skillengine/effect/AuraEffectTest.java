package com.aionemu.gameserver.skillengine.effect;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class AuraEffectTest {

	@Test
	void mantraEffectPacketIsSentToEffectorToo() throws IOException {
		String source = Files.readString(Path.of("src/main/java/com/aionemu/gameserver/skillengine/effect/AuraEffect.java"));

		assertTrue(source.contains("PacketSendUtility.broadcastPacket(effector, new SM_MANTRA_EFFECT(effector, skillId), true);"));
	}
}
