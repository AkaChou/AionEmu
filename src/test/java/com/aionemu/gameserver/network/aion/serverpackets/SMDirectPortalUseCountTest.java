package com.aionemu.gameserver.network.aion.serverpackets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.nio.ByteBuffer;

import org.junit.jupiter.api.Test;

class SMDirectPortalUseCountTest {

	@Test
	void writesRetailUseCountPayload() {
		var packet = new SM_DIRECT_PORTAL_USE_COUNT(0x12345678, 5, 300, true, 0, 2);
		var buffer = ByteBuffer.allocate(19);
		packet.setBuf(buffer);
		packet.writeImpl(null);

		assertArrayEquals(new byte[] { 3, 0x78, 0x56, 0x34, 0x12, 5, 0, 0, 0, 0x2c, 1, 0, 0, 1, 0, 2, 0, 0, 0 },
			buffer.array());
	}
}
