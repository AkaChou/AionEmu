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
	/** 奥德水晶饼干。 / Aether Crystal Biscuit. */
	AETHER_CRYSTAL_BISCUIT,
	/** 奥德宝石饼干。 / Aether Gem Biscuit. */
	AETHER_GEM_BISCUIT,
	/** 奥德粉末饼干。 / Aether Powder Biscuit. */
	AETHER_POWDER_BISCUIT,
	/** 防具。 / Armor. */
	ARMOR,
	/** 龙族鳞片。 / Balaur Scales. */
	BALAUR_SCALES,
	/** 骨头。 / Bones. */
	BONES,
	/** 排除项。 / Excludes. */
	EXCLUDES,
	/** 液体。 / Fluids. */
	FLUIDS,
	/** 高级工艺阶段。 / High Craft Step. */
	HIGH_CRAFT_STEP,
	/** 全能健康食品。 / Healthy Food All. */
	HEALTHY_FOOD_ALL,
	/** 辛辣健康食品。 / Healthy Food Spicy. */
	HEALTHY_FOOD_SPICY,
	/** 地狱恶魔 AP。 / Infernal Diabol AP. */
	INFERNAL_DIABOL_AP,
	/** 纯真梅雷克 XP。 / Innocent Merek XP. */
	INNOCENT_MEREK_XP,
	/** 杂项。 / Miscellaneous. */
	MISCELLANEOUS,
	/** 新年宠物食品。 / New Year Pet Food. */
	NEW_YEAR_PET_FOOD,
	/** 罂粟零食。 / Poppy Snack. */
	POPPY_SNACK,
	/** 美味罂粟零食。 / Poppy Snack Tasty. */
	POPPY_SNACK_TASTY,
	/** 营养罂粟零食。 / Poppy Snack Nutritious. */
	POPPY_SNACK_NUTRITIOUS,
	/** 术古硬币。 / Shugo Coin. */
	SHUGO_COIN,
	/** 灵魂。 / Souls. */
	SOULS,
	/** 臭味食物。 / Stinky. */
	STINKY,
	/** 荆棘。 / Thorns. */
	THORNS;

	/** 值。 / Value. */
	public String value() {
		return name();
	}

	/** 按字符串值转换 / From value */
	public static FoodType fromValue(String value) {
		return valueOf(value);
	}
}
