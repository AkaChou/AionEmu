package com.aionemu.gameserver.network.aion.serverpackets;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.ByteBuffer;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.services.item.ItemPacketService.ItemAddType;

class SMInventoryAddItemTest {

	@Test
	void writesThe58HeaderAsTwoBytesAndAnEntryCount() {
		SM_INVENTORY_ADD_ITEM packet = new SM_INVENTORY_ADD_ITEM(List.of(), null, ItemAddType.BUY);
		ByteBuffer buffer = ByteBuffer.allocate(4);
		packet.setBuf(buffer);
		packet.writeImpl(null);
		buffer.flip();

		assertEquals(ItemAddType.BUY.getMask(), Byte.toUnsignedInt(buffer.get()));
		assertEquals(0, Byte.toUnsignedInt(buffer.get()));
		assertEquals(0, Short.toUnsignedInt(buffer.getShort()));
		assertEquals(0, buffer.remaining());
	}
}
