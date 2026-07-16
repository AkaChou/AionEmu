package com.aionemu.gameserver.network.aion.serverpackets;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class SMNPAuthTokenPacketTest {

	@Test
	void writesNpTokenAsAsciiZeroTerminatedFields() {
		SM_NP_AUTH_TOKEN packet = new SM_NP_AUTH_TOKEN();
		ByteBuffer buffer = ByteBuffer.allocate(128);
		packet.setBuf(buffer);
		packet.writeImpl(null);
		buffer.flip();

		assertEquals(0x129, packet.getOpcode());
		assertEquals(1, Byte.toUnsignedInt(buffer.get()));
		assertEquals("local-aionemu", readAsciiZ(buffer));
		assertEquals("83C1595C-2932-B33F-DA9B-F26A859BEAB8", readAsciiZ(buffer));
		assertEquals("aion", readAsciiZ(buffer));
		assertEquals(0x1F6, Short.toUnsignedInt(buffer.getShort()));
		assertEquals(2, Byte.toUnsignedInt(buffer.get()));
		assertEquals(0, buffer.remaining());
	}

	private static String readAsciiZ(ByteBuffer buffer) {
		int start = buffer.position();
		while (buffer.get() != 0) {
		}
		int end = buffer.position() - 1;
		byte[] value = new byte[end - start];
		buffer.position(start);
		buffer.get(value);
		buffer.get();
		return new String(value, StandardCharsets.US_ASCII);
	}
}
