package com.aionemu.gameserver.services;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.model.templates.item.EquipType;
import com.aionemu.gameserver.model.templates.item.ItemTemplate;

class ArmsfusionServiceTest {

	@Test
	void treatsWeaponWithoutWeaponTypeAsNotTwoHanded() throws Exception {
		ItemTemplate template = new ItemTemplate();
		setField(template, "equipmentType", EquipType.WEAPON);

		assertFalse(template.isTwoHandWeapon());
	}

	@Test
	void validatesSecondaryWeaponBeforeComparingWeaponTypes() throws Exception {
		String source = Files.readString(Path.of(
				"src/main/java/com/aionemu/gameserver/services/ArmsfusionService.java"));
		int secondaryCheck = source.indexOf("if (!secondItem.getItemTemplate().isTwoHandWeapon())");
		int typeCheck = source.indexOf("firstItem.getItemTemplate().getWeaponType() != secondItem.getItemTemplate().getWeaponType()");

		assertTrue(secondaryCheck >= 0 && secondaryCheck < typeCheck);
	}

	@Test
	void allowsInventoryFusionWithoutNpcTarget() throws Exception {
		String source = Files.readString(Path.of(
				"src/main/java/com/aionemu/gameserver/network/aion/clientpackets/CM_FUSION_WEAPONS.java"));
		String run = source.substring(source.indexOf("protected void runImpl()"));

		assertFalse(run.contains("getTarget()"));
		assertTrue(run.contains("ArmsfusionService.fusionWeapons(player, firstItemId, secondItemId)"));
	}

	private static void setField(Object target, String fieldName, Object value) throws ReflectiveOperationException {
		Field field = target.getClass().getDeclaredField(fieldName);
		field.setAccessible(true);
		field.set(target, value);
	}
}
