package com.aionemu.gameserver.network.iteminfo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.templates.item.ItemTemplate;
import com.aionemu.gameserver.network.aion.iteminfo.ItemBlobEntry;
import com.aionemu.gameserver.network.aion.iteminfo.ItemInfoBlob;
import com.aionemu.gameserver.network.aion.iteminfo.ItemInfoBlob.ItemBlobType;
import org.junit.jupiter.api.Test;

class ManaStoneInfoBlobEntryTest {

	private static final int AMPLIFICATION_FLAG_OFFSET = 157;
	private static final int AMPLIFICATION_SKILL_ID_OFFSET = 158;
	private static final int SKILL_BOOST_FLAG_OFFSET = 173;
	private static final int SKILL_BOOST_SKILL_ID_OFFSET = 174;
	private static final int SKILL_BOOST_LEVEL_OFFSET = 178;

	@Test
	void writesSkillBoostDisplayFlagAndFieldsAtClientOffsets() {
		Item item = new TestItem(1, new TestItemTemplate(114101846));
		item.setIsEnhance(true);
		item.setEnhanceSkillId(4037);
		item.setEnhanceEnchantLevel(1);
		ItemBlobEntry entry = ItemInfoBlob.newBlobEntry(ItemBlobType.MANA_SOCKETS, null, item);
		ByteBuffer buffer = ByteBuffer.allocate(entry.getSize()).order(ByteOrder.LITTLE_ENDIAN);

		entry.writeThisBlob(buffer);

		assertEquals(entry.getSize(), buffer.position());
		for (int i = 162; i < SKILL_BOOST_FLAG_OFFSET; i++) {
			assertEquals(0, buffer.get(i));
		}
		assertEquals(1, buffer.get(SKILL_BOOST_FLAG_OFFSET));
		assertEquals(4037, buffer.getInt(SKILL_BOOST_SKILL_ID_OFFSET));
		assertEquals(1, buffer.getInt(SKILL_BOOST_LEVEL_OFFSET));
	}

	@Test
	void suppressesSkillBoostWhenStoredEnhanceTupleIsIncomplete() {
		Item item = new TestItem(1, new TestItemTemplate(114101846));
		item.setIsEnhance(true);
		item.setEnhanceSkillId(0);
		item.setEnhanceEnchantLevel(0);
		ItemBlobEntry entry = ItemInfoBlob.newBlobEntry(ItemBlobType.MANA_SOCKETS, null, item);
		ByteBuffer buffer = ByteBuffer.allocate(entry.getSize()).order(ByteOrder.LITTLE_ENDIAN);

		entry.writeThisBlob(buffer);

		assertEquals(entry.getSize(), buffer.position());
		assertEquals(0, buffer.get(SKILL_BOOST_FLAG_OFFSET));
		assertEquals(0, buffer.getInt(SKILL_BOOST_SKILL_ID_OFFSET));
		assertEquals(0, buffer.getInt(SKILL_BOOST_LEVEL_OFFSET));
	}

	@Test
	void suppressesInherentSkillDisplayEvenWhenAmplificationSkillIsPresent() {
		Item item = new TestItem(1, new TestItemTemplate(114101846));
		item.setAmplification(true);
		item.setAmplificationSkill(12345);
		ItemBlobEntry entry = ItemInfoBlob.newBlobEntry(ItemBlobType.MANA_SOCKETS, null, item);
		ByteBuffer buffer = ByteBuffer.allocate(entry.getSize()).order(ByteOrder.LITTLE_ENDIAN);

		entry.writeThisBlob(buffer);

		assertEquals(entry.getSize(), buffer.position());
		assertEquals(0, buffer.get(AMPLIFICATION_FLAG_OFFSET));
		assertEquals(0, buffer.getInt(AMPLIFICATION_SKILL_ID_OFFSET));
	}

	private static final class TestItemTemplate extends ItemTemplate {
		private final int templateId;

		private TestItemTemplate(int templateId) {
			this.templateId = templateId;
		}

		@Override
		public int getActivationCount() {
			return 0;
		}

		@Override
		public int getExpireTime() {
			return 0;
		}

		@Override
		public int getOptionSlotBonus() {
			return 0;
		}

		@Override
		public int getSkinSkill() {
			return 0;
		}

		@Override
		public int getRandomBonusId() {
			return 0;
		}

		@Override
		public int getTemplateId() {
			return templateId;
		}

		@Override
		public String getName() {
			return "test item";
		}

		@Override
		public int getNameId() {
			return templateId;
		}
	}

	private static final class TestItem extends Item {
		private TestItem(int objId, ItemTemplate itemTemplate) {
			super(objId, itemTemplate);
		}

		@Override
		public int getItemColor() {
			return 0;
		}
	}
}
