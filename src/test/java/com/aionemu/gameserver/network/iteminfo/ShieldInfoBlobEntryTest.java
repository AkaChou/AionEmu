package com.aionemu.gameserver.network.iteminfo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.items.ItemSlot;
import com.aionemu.gameserver.model.templates.item.ItemTemplate;
import com.aionemu.gameserver.network.aion.iteminfo.ItemBlobEntry;
import com.aionemu.gameserver.network.aion.iteminfo.ItemInfoBlob;
import com.aionemu.gameserver.network.aion.iteminfo.ItemInfoBlob.ItemBlobType;

class ShieldInfoBlobEntryTest {

	@Test
	void writesSingleShieldSlotAndDyeColor() {
		Item item = new TestItem(1, new ShieldTemplate());
		ItemBlobEntry entry = ItemInfoBlob.newBlobEntry(ItemBlobType.SLOTS_SHIELD, null, item);
		ByteBuffer buffer = ByteBuffer.allocate(entry.getSize()).order(ByteOrder.LITTLE_ENDIAN);

		entry.writeThisBlob(buffer);
		buffer.flip();

		assertEquals(ItemSlot.SUB_HAND.getSlotIdMask(), buffer.getLong());
		assertEquals(0, buffer.getLong());
		assertEquals(1, Byte.toUnsignedInt(buffer.get()));
		assertEquals(0x12, Byte.toUnsignedInt(buffer.get()));
		assertEquals(0x34, Byte.toUnsignedInt(buffer.get()));
		assertEquals(0x56, Byte.toUnsignedInt(buffer.get()));
	}

	private static final class ShieldTemplate extends ItemTemplate {
		@Override
		public int getItemSlot() {
			return (int) ItemSlot.SUB_HAND.getSlotIdMask();
		}

		@Override
		public boolean isItemDyePermitted() {
			return true;
		}
	}

	private static final class TestItem extends Item {
		private TestItem(int objectId, ItemTemplate template) {
			super(objectId, template);
		}

		@Override
		public int getItemColor() {
			return 0x123456;
		}
	}
}
