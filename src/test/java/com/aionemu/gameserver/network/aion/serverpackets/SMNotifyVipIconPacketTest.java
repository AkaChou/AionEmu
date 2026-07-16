package com.aionemu.gameserver.network.aion.serverpackets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.ByteBuffer;

import org.junit.jupiter.api.Test;

class SMNotifyVipIconPacketTest {

	@Test
	void writesRetailVipIconPayload() {
		SM_NOTIFY_VIP_ICON packet = new SM_NOTIFY_VIP_ICON(0x12345678, true);

		assertEquals(0x163, packet.getOpcode());
		assertArrayEquals(new byte[] { 0x78, 0x56, 0x34, 0x12, 3, 0 }, payload(packet));
	}

	@Test
	void writesZeroToClearInactiveVipIcon() {
		assertArrayEquals(new byte[] { 4, 3, 2, 1, 0, 0 },
				payload(new SM_NOTIFY_VIP_ICON(0x01020304, false)));
	}

	private static byte[] payload(SM_NOTIFY_VIP_ICON packet) {
		ByteBuffer buffer = ByteBuffer.allocate(6);
		packet.setBuf(buffer);
		packet.writeImpl(null);
		return buffer.array();
	}
}
