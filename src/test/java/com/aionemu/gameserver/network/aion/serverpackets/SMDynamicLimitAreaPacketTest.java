package com.aionemu.gameserver.network.aion.serverpackets;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.network.Crypt;

class SMDynamicLimitAreaPacketTest {

	@Test
	void writesSingleAreaUpdate() {
		SM_DYNAMIC_LIMIT_AREA_INFO packet = new SM_DYNAMIC_LIMIT_AREA_INFO("LF3_NoRecall", true);
		ByteBuffer buffer = ByteBuffer.allocate(256);
		packet.setBuf(buffer);
		packet.writeImpl(null);
		buffer.flip();

		assertEquals(0x16B, packet.getOpcode());
		assertEquals(0x295, Crypt.encodeOpcodec(packet.getOpcode()));
		assertEquals("LF3_NoRecall", readS(buffer));
		assertEquals(1, buffer.getInt());
		assertEquals(0, buffer.remaining());
	}

	@Test
	void writesAreaStateSnapshotInMapOrder() {
		Map<String, Boolean> areas = new LinkedHashMap<>();
		areas.put("LF3_First", true);
		areas.put("LF3_Second", false);
		SM_DYNAMIC_LIMIT_AREA_INFO_LIST packet = new SM_DYNAMIC_LIMIT_AREA_INFO_LIST(areas);
		ByteBuffer buffer = ByteBuffer.allocate(256);
		packet.setBuf(buffer);
		packet.writeImpl(null);
		buffer.flip();

		assertEquals(0x16C, packet.getOpcode());
		assertEquals(0x294, Crypt.encodeOpcodec(packet.getOpcode()));
		assertEquals(2, Short.toUnsignedInt(buffer.getShort()));
		assertEquals("LF3_First", readS(buffer));
		assertEquals(1, buffer.getInt());
		assertEquals("LF3_Second", readS(buffer));
		assertEquals(0, buffer.getInt());
		assertEquals(0, buffer.remaining());
	}

	private static String readS(ByteBuffer buffer) {
		StringBuilder value = new StringBuilder();
		for (char character; (character = buffer.getChar()) != 0;) {
			value.append(character);
		}
		return value.toString();
	}
}
