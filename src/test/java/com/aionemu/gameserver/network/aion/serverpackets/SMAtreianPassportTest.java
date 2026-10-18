package com.aionemu.gameserver.network.aion.serverpackets;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.ByteBuffer;
import java.util.List;

import org.junit.jupiter.api.Test;

class SMAtreianPassportTest {

	@Test
	void writesSingleLoginEventRecordUsingThe58ProtocolLayout() {
		SM_ATREIAN_PASSPORT packet =
			new SM_ATREIAN_PASSPORT(List.of(new SM_ATREIAN_PASSPORT.Record(8, 12, 1, false)), 11, 7, 2026);
		ByteBuffer buffer = ByteBuffer.allocate(32);
		packet.setBuf(buffer);
		packet.writeImpl(null);
		buffer.flip();

		assertEquals(2026, Short.toUnsignedInt(buffer.getShort()));
		assertEquals(7, Short.toUnsignedInt(buffer.getShort()));
		assertEquals(11, Short.toUnsignedInt(buffer.getShort()));
		assertEquals(1, Short.toUnsignedInt(buffer.getShort()));
		assertEquals(8, buffer.getInt());
		assertEquals(1, buffer.getInt());
		assertEquals(12, buffer.getInt());
		assertEquals(1, Byte.toUnsignedInt(buffer.get()));
		assertEquals(0, buffer.remaining());
	}

	@Test
	void writesEveryActiveAttendanceInOneList() {
		List<SM_ATREIAN_PASSPORT.Record> records = List.of(
			new SM_ATREIAN_PASSPORT.Record(8, 12, 1, false),
			new SM_ATREIAN_PASSPORT.Record(9, 0, 0, true));
		SM_ATREIAN_PASSPORT packet = new SM_ATREIAN_PASSPORT(records, 17, 10, 2026);
		ByteBuffer buffer = ByteBuffer.allocate(64);
		packet.setBuf(buffer);
		packet.writeImpl(null);
		buffer.flip();

		assertEquals(2026, Short.toUnsignedInt(buffer.getShort()));
		assertEquals(10, Short.toUnsignedInt(buffer.getShort()));
		assertEquals(17, Short.toUnsignedInt(buffer.getShort()));
		assertEquals(2, Short.toUnsignedInt(buffer.getShort()));
		assertEquals(8, buffer.getInt());
		assertEquals(1, buffer.getInt());
		assertEquals(12, buffer.getInt());
		assertEquals(1, Byte.toUnsignedInt(buffer.get()));
		assertEquals(9, buffer.getInt());
		assertEquals(0, buffer.getInt());
		assertEquals(0, buffer.getInt());
		assertEquals(0, Byte.toUnsignedInt(buffer.get()));
		assertEquals(0, buffer.remaining());
	}
}
