package com.aionemu.loginserver.network.gameserver.clientpackets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.junit.jupiter.api.Test;

class CM_GS_AUTHTest {

	@Test
	void skipsMalformedIpRangeAndConsumesPacket() {
		CM_GS_AUTH packet = new CM_GS_AUTH();
		ByteBuffer buffer = ByteBuffer.allocate(64).order(ByteOrder.LITTLE_ENDIAN);
		buffer.put((byte) 1);
		buffer.put((byte) 4).put(new byte[] { 127, 0, 0, 1 });
		buffer.putInt(1);
		buffer.put((byte) 0);
		buffer.put((byte) 4).put(new byte[] { 127, 0, 0, 1 });
		buffer.put((byte) 4).put(new byte[] { 127, 0, 0, 1 });
		buffer.putShort((short) 7777);
		buffer.putInt(100);
		buffer.putChar((char) 0);
		buffer.flip();
		packet.setBuffer(buffer);

		assertTrue(packet.read());
		assertEquals(0, packet.getRemainingBytes());
	}
}
