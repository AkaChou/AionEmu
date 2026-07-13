package com.aionemu.gameserver.network.aion.serverpackets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.nio.ByteBuffer;

import org.junit.jupiter.api.Test;

class SMUseObjectAndWindstreamPacketTest {

	@Test
	void writesGaugePayloadInRetailOrder() {
		SM_USE_OBJECT packet = new SM_USE_OBJECT(0x12345678, 0x23456789, 5000, 1);
		ByteBuffer buffer = ByteBuffer.allocate(13);
		packet.setBuf(buffer);
		packet.writeImpl(null);

		assertArrayEquals(new byte[] { 0x78, 0x56, 0x34, 0x12, (byte) 0x89, 0x67, 0x45, 0x23, (byte) 0x88, 0x13, 0, 0, 1 },
			buffer.array());
	}

	@Test
	void writesMovingCollisionPayloadInRetailOrder() {
		SM_WINDSTREAM_ANNOUNCE packet = new SM_WINDSTREAM_ANNOUNCE(2, 300250000, 7, 1);
		ByteBuffer buffer = ByteBuffer.allocate(13);
		packet.setBuf(buffer);
		packet.writeImpl(null);

		assertArrayEquals(new byte[] { 2, 0, 0, 0, (byte) 0x90, 0x73, (byte) 0xE5, 0x11, 7, 0, 0, 0, 1 },
			buffer.array());
	}
}
