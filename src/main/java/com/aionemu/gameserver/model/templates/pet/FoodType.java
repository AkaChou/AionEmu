package com.aionemu.gameserver.model.templates.pet;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlType;

/**
 * Food 类型枚举。
 * Food Type enumeration.
 */

@XmlType(name = "FoodType")
@XmlEnum
public enum FoodType {
	/** Aether Crystal Biscuit / Aether Crystal Biscuit */
	AETHER_CRYSTAL_BISCUIT, AETHER_GEM_BISCUIT, AETHER_POWDER_BISCUIT, ARMOR, BALAUR_SCALES, BONES, EXCLUDES, FLUIDS,
	/** High Craft Step / High Craft Step */
	HIGH_CRAFT_STEP, HEALTHY_FOOD_ALL, HEALTHY_FOOD_SPICY, INFERNAL_DIABOL_AP, INNOCENT_MEREK_XP, MISCELLANEOUS,
	/** New Year Pet Food / New Year Pet Food */
	NEW_YEAR_PET_FOOD, POPPY_SNACK, POPPY_SNACK_TASTY, POPPY_SNACK_NUTRITIOUS, SHUGO_COIN, SOULS, STINKY, THORNS;

	/** 值。 / Value. */
	public String value() {
		return name();
	}

	/** 值 / From Value*/
	public static FoodType fromValue(String value) {
		return valueOf(value);
	}
}
