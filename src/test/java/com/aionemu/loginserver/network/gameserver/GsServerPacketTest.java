package com.aionemu.loginserver.network.gameserver;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;

import org.junit.jupiter.api.Test;

class GsServerPacketTest {

	@Test
	void writeSerializesSharedPacketInstances() throws Exception {
		assertTrue(Modifier.isSynchronized(GsServerPacket.class
				.getDeclaredMethod("write", GsConnection.class, ByteBuffer.class)
				.getModifiers()));
	}

	@Test
	void writesFrameAndPayloadAsLittleEndian() {
		ByteBuffer buffer = ByteBuffer.allocate(32);

		new SamplePacket().write(null, buffer);

		byte[] packet = new byte[buffer.limit()];
		buffer.get(packet);
		assertArrayEquals(new byte[] {
			9, 0,
			0x55,
			0x22, 0x11,
			0x66, 0x55, 0x44, 0x33
		}, packet);
	}

	private static final class SamplePacket extends GsServerPacket {
		@Override
		protected void writeImpl(GsConnection con) {
			writeC(0x55);
			writeH(0x1122);
			writeD(0x33445566);
		}
	}
}
