package com.aionemu.gameserver.model.templates.item.actions;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.aionemu.testutil.ConfigSnapshot;
import com.aionemu.gameserver.configs.main.EnchantsConfig;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.templates.item.ItemCategory;
import com.aionemu.gameserver.model.templates.item.ItemTemplate;

class TemperingActionTest {

	private final ConfigSnapshot enchantConfigSnapshot = ConfigSnapshot.of(EnchantsConfig.class,
		"ENCHANT_PLUME", "ENCHANT_BRACELET", "ENCHANT_ACCESSORY");

	private final TemperingAction action = new TemperingAction();

	@AfterEach
	void restoreConfig() {
		enchantConfigSnapshot.restore();
	}

	@Test
	void usesEachSlotRateForItsOwnCategory() throws Exception {
		EnchantsConfig.ENCHANT_ACCESSORY = 100;
		EnchantsConfig.ENCHANT_PLUME = 100;
		EnchantsConfig.ENCHANT_BRACELET = 0;

		assertTrue(action.isTemperingSuccess(null, item(ItemCategory.EARRINGS)));
		assertTrue(action.isTemperingSuccess(null, item(ItemCategory.PLUME)));
		assertFalse(action.isTemperingSuccess(null, item(ItemCategory.BRACELET)));
	}

	@Test
	void refusesNonAccessoriesInsteadOfUsingTheAccessoryRate() throws Exception {
		EnchantsConfig.ENCHANT_ACCESSORY = 100;

		assertFalse(action.isTemperingSuccess(null, item(ItemCategory.SWORD)));
	}

	private static Item item(ItemCategory category) throws Exception {
		ItemTemplate template = new ItemTemplate();
		setField(template, "category", category);
		setField(template, "maxAuthorize", 255);
		return new Item(1, template);
	}

	private static void setField(Object target, String fieldName, Object value) throws Exception {
		Field field = target.getClass().getDeclaredField(fieldName);
		field.setAccessible(true);
		field.set(target, value);
	}
}
