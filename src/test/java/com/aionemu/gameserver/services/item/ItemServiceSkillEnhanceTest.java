package com.aionemu.gameserver.services.item;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.Reader;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;

import jakarta.xml.bind.JAXBContext;

import com.aionemu.gameserver.dataholders.ItemSkillEnhanceData;
import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.templates.item.ItemTemplate;
import org.junit.jupiter.api.Test;

class ItemServiceSkillEnhanceTest {

	@Test
	void fillsMissingSkillEnhanceForLoadedItems() throws Exception {
		Item item = new Item(1, new TestItemTemplate(100));
		item.setPersistentState(PersistentState.UPDATED);
		ItemSkillEnhanceData data = unmarshal("""
			<item_skill_enhances>
				<item_skill_enhance id="100" player_class="CLERIC" skill_id="4037"/>
			</item_skill_enhances>
			""");

		assertTrue(ItemService.ensureSkillEnhance(item, data, PlayerClass.CLERIC));

		assertTrue(item.isEnhance());
		assertEquals(4037, item.getEnhanceSkillId());
		assertEquals(1, item.getEnhanceEnchantLevel());
		assertEquals(PersistentState.UPDATE_REQUIRED, item.getPersistentState());
	}

	@Test
	void leavesExistingSkillEnhanceUntouched() throws Exception {
		Item item = new Item(1, new TestItemTemplate(100));
		item.setIsEnhance(true);
		item.setEnhanceSkillId(4037);
		item.setEnhanceEnchantLevel(2);
		item.setPersistentState(PersistentState.UPDATED);
		ItemSkillEnhanceData data = unmarshal("""
			<item_skill_enhances>
				<item_skill_enhance id="100" player_class="CLERIC" skill_id="4037"/>
			</item_skill_enhances>
			""");

		assertFalse(ItemService.ensureSkillEnhance(item, data, PlayerClass.CLERIC));

		assertEquals(4037, item.getEnhanceSkillId());
		assertEquals(2, item.getEnhanceEnchantLevel());
		assertEquals(PersistentState.UPDATED, item.getPersistentState());
	}

	@Test
	void replacesExistingSkillEnhanceWhenSkillIsNotInCurrentTemplate() throws Exception {
		Item item = new Item(1, new TestItemTemplate(100));
		item.setIsEnhance(true);
		item.setEnhanceSkillId(999999);
		item.setEnhanceEnchantLevel(1);
		item.setPersistentState(PersistentState.UPDATED);
		ItemSkillEnhanceData data = unmarshal("""
			<item_skill_enhances>
				<item_skill_enhance id="100" player_class="CLERIC" skill_id="4037"/>
			</item_skill_enhances>
			""");

		assertTrue(ItemService.ensureSkillEnhance(item, data, PlayerClass.CLERIC));

		assertTrue(item.isEnhance());
		assertEquals(4037, item.getEnhanceSkillId());
		assertEquals(1, item.getEnhanceEnchantLevel());
		assertEquals(PersistentState.UPDATE_REQUIRED, item.getPersistentState());
	}

	@Test
	void rbShoesSkillEnhance65IsAvailableForClericLike58AL() throws Exception {
		Item item = new Item(1, new TestItemTemplate(65));
		ItemSkillEnhanceData data = unmarshal(Path.of("src/main/resources/aion/definitions/items/skill_enhance/item_skill_enhances.xml"));

		assertTrue(ItemService.ensureSkillEnhance(item, data, PlayerClass.CLERIC));

		assertTrue(item.isEnhance());
		assertTrue(data.getSkillEnhance(65, PlayerClass.CLERIC).getSkillId().contains(item.getEnhanceSkillId()));
		assertTrue(data.getSkillEnhance(65, PlayerClass.CLERIC).getSkillId().contains(4668));
		assertTrue(data.getSkillEnhance(65, PlayerClass.CLERIC).getSkillId().contains(4552));
	}

	private static ItemSkillEnhanceData unmarshal(String xml) throws Exception {
		return ItemSkillEnhanceData.class.cast(JAXBContext.newInstance(ItemSkillEnhanceData.class)
				.createUnmarshaller().unmarshal(new StringReader(xml)));
	}

	private static ItemSkillEnhanceData unmarshal(Path path) throws Exception {
		try (Reader reader = Files.newBufferedReader(path)) {
			return ItemSkillEnhanceData.class.cast(JAXBContext.newInstance(ItemSkillEnhanceData.class)
					.createUnmarshaller().unmarshal(reader));
		}
	}

	private static final class TestItemTemplate extends ItemTemplate {
		private final int skillEnhance;

		private TestItemTemplate(int skillEnhance) {
			this.skillEnhance = skillEnhance;
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
		public int getSkillEnhance() {
			return skillEnhance;
		}

		@Override
		public int getNameId() {
			return 1;
		}
	}
}
