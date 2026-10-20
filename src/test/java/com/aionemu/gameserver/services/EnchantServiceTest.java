package com.aionemu.gameserver.services;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.aionemu.testutil.ConfigSnapshot;
import com.aionemu.commons.configuration.Property;
import com.aionemu.gameserver.configs.main.EnchantsConfig;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.templates.item.ItemCategory;
import com.aionemu.gameserver.model.templates.item.ItemQuality;
import com.aionemu.gameserver.model.templates.item.ItemTemplate;

/**
 * 验证强化费用、独立等级上限与成功率，并在各测试后恢复静态配置。
 * Verifies enchant costs, independent caps and success rates, restoring static settings after each test.
 */
class EnchantServiceTest {

	private final ConfigSnapshot enchantConfigSnapshot = ConfigSnapshot.of(EnchantsConfig.class,
		"ENCHANT_ITEM_KINAH", "MAX_EQUIPMENT_ENCHANT_LEVEL",
		"MAX_AUTHORIZE_ACCESSORY_LEVEL", "MAX_AUTHORIZE_PLUME_LEVEL", "MAX_AUTHORIZE_BRACELET_LEVEL",
		"MAX_STIGMA_ENCHANT_LEVEL");

	@AfterEach
	void restoreConfig() {
		enchantConfigSnapshot.restore();
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
		assertEquals("50", property.defaultValue());
	}

	@Test
	void capsEquipmentEnchantLevelAtConfiguredMaximum() {
		EnchantsConfig.MAX_EQUIPMENT_ENCHANT_LEVEL = 50;

		assertEquals(29, EnchantService.capEquipmentEnchantLevel(29));
		assertEquals(50, EnchantService.capEquipmentEnchantLevel(50));
		assertEquals(50, EnchantService.capEquipmentEnchantLevel(52));
	}

	@Test
	void usesChangedMaximumEquipmentEnchantLevel() {
		EnchantsConfig.MAX_EQUIPMENT_ENCHANT_LEVEL = 25;

		assertEquals(25, EnchantService.capEquipmentEnchantLevel(30));
	}

	@Test
	void capsEquipmentEnchantLevelAtTwoHundredWhenConfiguredHigher() {
		EnchantsConfig.MAX_EQUIPMENT_ENCHANT_LEVEL = 500;

		assertEquals(200, EnchantService.getMaxEquipmentEnchantLevel());
		assertEquals(200, EnchantService.capEquipmentEnchantLevel(250));
	}

	@Test
	void declaresStigmaMaximumWithTenAsBuiltInDefault() throws Exception {
		Property property = EnchantsConfig.class.getDeclaredField("MAX_STIGMA_ENCHANT_LEVEL")
			.getAnnotation(Property.class);

		assertEquals("gameserver.enchant.stigma.max.level", property.key());
		assertEquals("10", property.defaultValue());
	}

	@Test
	void fallsBackToTenAndCapsStigmaMaximumAtOneHundred() {
		EnchantsConfig.MAX_STIGMA_ENCHANT_LEVEL = 0;
		assertEquals(10, EnchantService.getMaxStigmaEnchantLevel());

		EnchantsConfig.MAX_STIGMA_ENCHANT_LEVEL = 15;
		assertEquals(15, EnchantService.getMaxStigmaEnchantLevel());

		EnchantsConfig.MAX_STIGMA_ENCHANT_LEVEL = 100;
		assertEquals(100, EnchantService.getMaxStigmaEnchantLevel());

		EnchantsConfig.MAX_STIGMA_ENCHANT_LEVEL = 500;
		assertEquals(100, EnchantService.getMaxStigmaEnchantLevel());
	}

	@Test
	void declaresStigmaCastTimeWithThreeThousandMillisAsDefault() throws Exception {
		Property property = EnchantsConfig.class.getDeclaredField("STIGMA_ENCHANT_SPEED")
			.getAnnotation(Property.class);

		assertEquals("gameserver.enchant.stigma.cast.time", property.key());
		assertEquals("3000", property.defaultValue());
	}

	@Test
	void declaresIndependentEstimaCastSpeedWithFiveHundredMillisAsDefault() throws Exception {
		Property property = EnchantsConfig.class.getDeclaredField("ESTIMA_ENCHANT_SPEED")
			.getAnnotation(Property.class);

		assertEquals("gameserver.enchant.estima.cast.time", property.key());
		assertEquals("500", property.defaultValue());
	}

	@Test
	void declaresFiveHundredMillisCastTimeForEveryUpgradePath() throws Exception {
		Property enchant = EnchantsConfig.class.getDeclaredField("ENCHANT_SPEED").getAnnotation(Property.class);
		Property authorize = EnchantsConfig.class.getDeclaredField("AUTHORIZE_SPEED").getAnnotation(Property.class);

		assertEquals("gameserver.enchant.cast.time", enchant.key());
		assertEquals("500", enchant.defaultValue());
		assertEquals("gameserver.enchant.authorize.cast.time", authorize.key());
		assertEquals("500", authorize.defaultValue());
	}

	@Test
	void declaresAuthorizeCapsWithThirtyAsDefault() throws Exception {
		Property accessory = EnchantsConfig.class.getDeclaredField("MAX_AUTHORIZE_ACCESSORY_LEVEL")
				.getAnnotation(Property.class);
		Property plume = EnchantsConfig.class.getDeclaredField("MAX_AUTHORIZE_PLUME_LEVEL")
				.getAnnotation(Property.class);
		Property bracelet = EnchantsConfig.class.getDeclaredField("MAX_AUTHORIZE_BRACELET_LEVEL")
				.getAnnotation(Property.class);

		assertEquals("gameserver.enchant.authorize.accessory.max.level", accessory.key());
		assertEquals("30", accessory.defaultValue());
		assertEquals("gameserver.enchant.authorize.plume.max.level", plume.key());
		assertEquals("30", plume.defaultValue());
		assertEquals("gameserver.enchant.authorize.bracelet.max.level", bracelet.key());
		assertEquals("30", bracelet.defaultValue());
	}

	@Test
	void declaresFullSuccessRatesByDefault() throws Exception {
		for (String field : new String[] { "SOCKET_MANASTONE", "ENCHANT_ITEM", "ENCHANT_PLUME", "ENCHANT_BRACELET",
			"ENCHANT_ACCESSORY", "ENCHANT_STIGMA", "ENCHANT_ESTIMA" }) {
			Property property = EnchantsConfig.class.getDeclaredField(field).getAnnotation(Property.class);
			assertEquals("100", property.defaultValue(), field);
		}
	}

	@Test
	void separatesPlumeAndBraceletCaps() throws Exception {
		EnchantsConfig.MAX_AUTHORIZE_ACCESSORY_LEVEL = 20;
		EnchantsConfig.MAX_AUTHORIZE_PLUME_LEVEL = 25;
		EnchantsConfig.MAX_AUTHORIZE_BRACELET_LEVEL = 28;

		assertEquals(20, EnchantService.getMaxAuthorizeLevel(authorizable(ItemCategory.EARRINGS)));
		assertEquals(25, EnchantService.getMaxAuthorizeLevel(authorizable(ItemCategory.PLUME)));
		assertEquals(28, EnchantService.getMaxAuthorizeLevel(authorizable(ItemCategory.BRACELET)));
	}

	@Test
	void capsAuthorizeAtTwoHundredWhenConfiguredHigher() throws Exception {
		EnchantsConfig.MAX_AUTHORIZE_ACCESSORY_LEVEL = 500;

		Item earrings = authorizable(ItemCategory.EARRINGS);
		assertEquals(200, EnchantService.getMaxAuthorizeLevel(earrings));
		assertEquals(200, EnchantService.capAuthorizeLevel(earrings, 250));
	}

	@Test
	void leavesNonAuthorizableItemsUncapped() throws Exception {
		EnchantsConfig.MAX_AUTHORIZE_ACCESSORY_LEVEL = 30;

		ItemTemplate weapon = new ItemTemplate();
		setField(weapon, "category", ItemCategory.SWORD);
		setField(weapon, "maxAuthorize", 0);

		assertEquals(0, EnchantService.getMaxAuthorizeLevel(new Item(1, weapon)));
		assertEquals(50, EnchantService.capAuthorizeLevel(new Item(1, weapon), 50));
	}

	private static Item authorizable(ItemCategory category) throws Exception {
		ItemTemplate template = new ItemTemplate();
		setField(template, "category", category);
		setField(template, "maxAuthorize", 255);
		return new Item(1, template);
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
