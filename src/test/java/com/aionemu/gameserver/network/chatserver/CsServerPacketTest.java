package com.aionemu.gameserver.network.chatserver;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;

import org.junit.jupiter.api.Test;

class CsServerPacketTest {

	@Test
	void writeSerializesSharedPacketInstances() throws Exception {
		assertTrue(Modifier.isSynchronized(CsServerPacket.class
				.getDeclaredMethod("write", ChatServerConnection.class, ByteBuffer.class)
				.getModifiers()));
	}

	@Test
	void writesFrameAndPayloadAsLittleEndian() {
		ByteBuffer buffer = ByteBuffer.allocate(32);

		new SamplePacket().write(null, buffer);

		byte[] packet = new byte[buffer.limit()];
		buffer.get(packet);
		assertArrayEquals(new byte[] {
			10, 0,
			0x7A,
			0x55,
			0x22, 0x11,
			0x66, 0x55, 0x44, 0x33
		}, packet);
	}

	private static final class SamplePacket extends CsServerPacket {
		private SamplePacket() {
			super(0x7A);
		}

		@Override
		protected void writeImpl(ChatServerConnection con) {
			writeC(0x55);
			writeH(0x1122);
			writeD(0x33445566);
		}
	}
}
