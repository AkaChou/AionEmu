package com.aionemu.gameserver.services;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.aionemu.commons.configuration.Property;
import com.aionemu.gameserver.configs.main.EnchantsConfig;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.templates.item.ItemQuality;
import com.aionemu.gameserver.model.templates.item.ItemTemplate;

class EnchantServiceTest {

	private final int originalUnifiedEnchantKinah = EnchantsConfig.ENCHANT_ITEM_KINAH;
	private final int originalMaxEquipmentEnchantLevel = EnchantsConfig.MAX_EQUIPMENT_ENCHANT_LEVEL;

	@AfterEach
	void restoreConfig() {
		EnchantsConfig.ENCHANT_ITEM_KINAH = originalUnifiedEnchantKinah;
		EnchantsConfig.MAX_EQUIPMENT_ENCHANT_LEVEL = originalMaxEquipmentEnchantLevel;
	}

	@Test
	void defaultsToCurrentQualityAndLevelPriceTableWhenUnifiedPriceIsDisabled() throws Exception {
		EnchantsConfig.ENCHANT_ITEM_KINAH = -1;

		assertEquals(11441, EnchantService.EnchantKinah(item(ItemQuality.EPIC, 14)));
		assertEquals(6356250, EnchantService.EnchantKinah(item(ItemQuality.MYTHIC, 15)));
		assertEquals(0, EnchantService.EnchantKinah(item(ItemQuality.UNIQUE, 20)));
	}

	@Test
	void usesConfiguredUnifiedPriceForEveryQualityAndEnchantLevel() throws Exception {
		EnchantsConfig.ENCHANT_ITEM_KINAH = 123456;

		assertEquals(123456, EnchantService.EnchantKinah(item(ItemQuality.EPIC, 0)));
		assertEquals(123456, EnchantService.EnchantKinah(item(ItemQuality.MYTHIC, 25)));
		assertEquals(123456, EnchantService.EnchantKinah(item(ItemQuality.UNIQUE, 20)));
	}

	@Test
	void allowsZeroAsConfiguredUnifiedPrice() throws Exception {
		EnchantsConfig.ENCHANT_ITEM_KINAH = 0;

		assertEquals(0, EnchantService.EnchantKinah(item(ItemQuality.MYTHIC, 20)));
	}

	@Test
	void declaresDefaultMaximumEquipmentEnchantLevel() throws Exception {
		Property property = EnchantsConfig.class.getDeclaredField("MAX_EQUIPMENT_ENCHANT_LEVEL")
				.getAnnotation(Property.class);

		assertEquals("gameserver.enchant.equipment.max.level", property.key());
		assertEquals("30", property.defaultValue());
	}

	@Test
	void capsEquipmentEnchantLevelAtConfiguredMaximum() {
		EnchantsConfig.MAX_EQUIPMENT_ENCHANT_LEVEL = 30;

		assertEquals(29, EnchantService.capEquipmentEnchantLevel(29));
		assertEquals(30, EnchantService.capEquipmentEnchantLevel(30));
		assertEquals(30, EnchantService.capEquipmentEnchantLevel(32));
	}

	@Test
	void usesChangedMaximumEquipmentEnchantLevel() {
		EnchantsConfig.MAX_EQUIPMENT_ENCHANT_LEVEL = 25;

		assertEquals(25, EnchantService.capEquipmentEnchantLevel(30));
	}

	private static Item item(ItemQuality quality, int enchantLevel) throws Exception {
		ItemTemplate template = new ItemTemplate();
		setField(template, "itemQuality", quality);
		Item item = new Item(1, template);
		item.setEnchantLevel(enchantLevel);
		return item;
	}

	private static void setField(Object target, String fieldName, Object value) throws Exception {
		Field field = target.getClass().getDeclaredField(fieldName);
		field.setAccessible(true);
		field.set(target, value);
	}
}
