package com.aionemu.gameserver.network.loginserver;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.nio.ByteBuffer;

import org.junit.jupiter.api.Test;

class LsServerPacketTest {

	@Test
	void writesFrameAndPayloadAsLittleEndian() {
		ByteBuffer buffer = ByteBuffer.allocate(32);

		new SamplePacket().write(null, buffer);

		byte[] packet = new byte[buffer.limit()];
		buffer.get(packet);
		assertArrayEquals(new byte[] {
			14, 0,
			0x7A,
			0x55,
			0x22, 0x11,
			0x66, 0x55, 0x44, 0x33,
			0x41, 0, 0, 0
		}, packet);
	}

	private static final class SamplePacket extends LsServerPacket {
		private SamplePacket() {
			super(0x7A);
		}

		@Override
		protected void writeImpl(LoginServerConnection con) {
			writeC(0x55);
			writeH(0x1122);
			writeD(0x33445566);
			writeS("A");
		}
	}
}
