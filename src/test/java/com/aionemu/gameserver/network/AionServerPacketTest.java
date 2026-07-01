package com.aionemu.gameserver.network;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import org.junit.jupiter.api.Test;

class AionServerPacketTest {

	@Test
	void writeSerializesSharedPacketInstances() throws Exception {
		assertTrue(Modifier.isSynchronized(AionServerPacket.class
				.getDeclaredMethod("write", AionConnection.class, ByteBuffer.class)
				.getModifiers()));
	}
}
