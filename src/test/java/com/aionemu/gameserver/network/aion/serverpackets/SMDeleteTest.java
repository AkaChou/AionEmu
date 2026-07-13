package com.aionemu.gameserver.network.aion.serverpackets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.nio.ByteBuffer;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.model.gameobjects.AionObject;

class SMDeleteTest {

	@Test
	void writesNormalNpcDeparturePayload() {
		SM_DELETE packet = new SM_DELETE(new TestObject(0x12345678), 0);
		ByteBuffer buffer = ByteBuffer.allocate(6);
		packet.setBuf(buffer);
		packet.writeImpl(null);

		assertArrayEquals(new byte[] { 0x78, 0x56, 0x34, 0x12, 0, (byte) 0xFF }, buffer.array());
	}

	private static final class TestObject extends AionObject {
		private TestObject(int objectId) {
			super(objectId);
		}

		@Override
		public String getName() {
			return "test";
		}
	}
}
