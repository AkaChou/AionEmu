package com.aionemu.gameserver.network.aion.serverpackets;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.ByteBuffer;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.templates.item.ItemTemplate;

class SMTuneResultTest {

	@Test
	void writesThe58IdentifyResultLayout() {
		Item item = new Item(123, new ItemTemplate());
		item.setItemColor(-1);
		item.setBonusNumber(7);
		item.setOptionalSocket(2);
		SM_TUNE_RESULT packet = new SM_TUNE_RESULT(null, item, 166200022);
		assertEquals(0x122, packet.getOpcode());
		ByteBuffer buffer = ByteBuffer.allocate(198);
		packet.setBuf(buffer);
		packet.writeImpl(null);
		buffer.flip();

		assertEquals(198, buffer.remaining());
		assertEquals(123, buffer.getInt());
		assertEquals(166200022, buffer.getInt());
		assertEquals(7, Byte.toUnsignedInt(buffer.get()));
		buffer.position(9 + 6);
		assertEquals(2, Byte.toUnsignedInt(buffer.get()));
		buffer.position(196);
		assertEquals(0, Byte.toUnsignedInt(buffer.get()));
		assertEquals(0, Byte.toUnsignedInt(buffer.get()));
	}
}
