package com.aionemu.gameserver.network.aion.serverpackets;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.ByteBuffer;

import org.junit.jupiter.api.Test;

class SMTargetSelectedTest {

	@Test
	void writesZeroTargetDataWhenTheSelectionIsCleared() {
		SM_TARGET_SELECTED packet = new SM_TARGET_SELECTED(null);
		ByteBuffer buffer = ByteBuffer.allocate(22);
		packet.setBuf(buffer);
		packet.writeImpl(null);
		buffer.flip();

		assertEquals(22, buffer.remaining());
		assertEquals(0, buffer.getInt());
		assertEquals(0, buffer.getShort());
		assertEquals(0, buffer.getInt());
		assertEquals(0, buffer.getInt());
		assertEquals(0, buffer.getInt());
		assertEquals(0, buffer.getInt());
	}
}
