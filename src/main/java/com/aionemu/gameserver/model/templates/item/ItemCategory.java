package com.aionemu.gameserver.model.templates.item;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 物品分类枚举。
 * Item Category enumeration.
 */

@XmlType(name = "item_category")
@XmlEnum
public enum ItemCategory {
	/** 魔石 / Manastone. */
	MANASTONE, SPECIAL_MANASTONE, PRIMARY_MANASTONE, GODSTONE, ENCHANTMENT, ENCHANTMENT_STIGMA,
	/** 强化增幅 / Enchantment Amplification */
	ENCHANTMENT_AMPLIFICATION, FLUX, BALIC_EMOTION, BALIC_MATERIAL, RAWHIDE, SOULSTONE, RECIPE, GATHERABLE,
	/** 可采集物加成。 / Gatherable Bonus. */
	GATHERABLE_BONUS, SWORD, DAGGER, MACE, ORB, SPELLBOOK, GREATSWORD, POLEARM, STAFF, BOW, SHIELD, JACKET, PANTS,
	/** 碎片 / Shard. */
	SHARD, SHOES, GLOVES, SHOULDERS, NECKLACE, EARRINGS, RINGS, HELMET, BELT, SKILLBOOK, STIGMA, COINS, MEDALS, QUEST,
	/** 钥匙 / Key. */
	KEY, TEMPERING, CRAFT_BOOST, COMBINATION,

	// 4.0
	/** 枪 / Gun. */
	GUN, CANNON, HARP, KEYBLADE, KEYHAMMER, PLUME, NONE,

	// 5.1
	/** 艾斯提玛 / Estima. */
	ESTIMA, BRACELET
}
