package com.aionemu.gameserver.model.broker;

import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.broker.filter.BrokerContainsExtraFilter;
import com.aionemu.gameserver.model.broker.filter.BrokerContainsFilter;
import com.aionemu.gameserver.model.broker.filter.BrokerFilter;
import com.aionemu.gameserver.model.broker.filter.BrokerMinMaxFilter;
import com.aionemu.gameserver.model.broker.filter.BrokerPlayerClassExtraFilter;
import com.aionemu.gameserver.model.broker.filter.BrokerRecipeFilter;
import com.aionemu.gameserver.model.gameobjects.Item;

/**
 * 经纪行物品掩码枚举。
 * Broker Item Mask enumeration.
 */

public enum BrokerItemMask {
	/**
	 * 武器分区 + 子分类 / Weapons Section + Sub Categories
	 */
	WEAPON(9010, new BrokerMinMaxFilter(1000, 1023), null, true),
	WEAPON_SWORD(1000, new BrokerContainsFilter(1000), BrokerItemMask.WEAPON, false),
	WEAPON_MACE(1001, new BrokerContainsFilter(1001), BrokerItemMask.WEAPON, false),
	WEAPON_DAGGER(1002, new BrokerContainsFilter(1002), BrokerItemMask.WEAPON, false),
	WEAPON_ORB(1005, new BrokerContainsFilter(1005), BrokerItemMask.WEAPON, false),
	WEAPON_SPELLBOOK(1006, new BrokerContainsFilter(1006), BrokerItemMask.WEAPON, false),
	WEAPON_GREATSWORD(1009, new BrokerContainsFilter(1009), BrokerItemMask.WEAPON, false),
	WEAPON_POLEARM(1013, new BrokerContainsFilter(1013), BrokerItemMask.WEAPON, false),
	WEAPON_STAFF(1015, new BrokerContainsFilter(1015), BrokerItemMask.WEAPON, false),
	WEAPON_BOW(1017, new BrokerContainsFilter(1017), BrokerItemMask.WEAPON, false),
	WEAPON_GUN(1018, new BrokerContainsFilter(1018), BrokerItemMask.WEAPON, false),
	WEAPON_CANNON(1019, new BrokerContainsFilter(1019), BrokerItemMask.WEAPON, false),
	WEAPON_HARP(1020, new BrokerContainsFilter(1020), BrokerItemMask.WEAPON, false),
	WEAPON_KEYBLADE(1021, new BrokerContainsFilter(1021), BrokerItemMask.WEAPON, false),
	WEAPON_KEYHAMMER(1022, new BrokerContainsFilter(1022), BrokerItemMask.WEAPON, false),

	/**
	 * 防具分区 + 子分类 / Armor Section + Sub Categories
	 */
	ARMOR(9020, new BrokerMinMaxFilter(1101, 1160), null, true),
	ARMOR_CLOTHING(8010, new BrokerContainsFilter(1100, 1110, 1120, 1130, 1140), BrokerItemMask.ARMOR, true),
	ARMOR_CLOTHING_JACKET(1100, new BrokerContainsFilter(1100), BrokerItemMask.ARMOR_CLOTHING, false),
	ARMOR_CLOTHING_GLOVES(1110, new BrokerContainsFilter(1110), BrokerItemMask.ARMOR_CLOTHING, false),
	ARMOR_CLOTHING_PAULDRONS(1120, new BrokerContainsFilter(1120), BrokerItemMask.ARMOR_CLOTHING, false),
	ARMOR_CLOTHING_PANTS(1130, new BrokerContainsFilter(1130), BrokerItemMask.ARMOR_CLOTHING, false),
	ARMOR_CLOTHING_SHOES(1140, new BrokerContainsFilter(1140), BrokerItemMask.ARMOR_CLOTHING, false),
	ARMOR_CLOTH(8020, new BrokerContainsFilter(1101, 1111, 1121, 1131, 1141), BrokerItemMask.ARMOR, true),
	ARMOR_CLOTH_JACKET(1101, new BrokerContainsFilter(1101), BrokerItemMask.ARMOR_CLOTH, false),
	ARMOR_CLOTH_GLOVES(1111, new BrokerContainsFilter(1111), BrokerItemMask.ARMOR_CLOTH, false),
	ARMOR_CLOTH_PAULDRONS(1121, new BrokerContainsFilter(1121), BrokerItemMask.ARMOR_CLOTH, false),
	ARMOR_CLOTH_PANTS(1131, new BrokerContainsFilter(1131), BrokerItemMask.ARMOR_CLOTH, false),
	ARMOR_CLOTH_SHOES(1141, new BrokerContainsFilter(1141), BrokerItemMask.ARMOR_CLOTH, false),
	ARMOR_LEATHER(8030, new BrokerContainsFilter(1103, 1113, 1123, 1133, 1143), BrokerItemMask.ARMOR, true),
	ARMOR_LEATHER_JACKET(1103, new BrokerContainsFilter(1103), BrokerItemMask.ARMOR_LEATHER, false),
	ARMOR_LEATHER_GLOVES(1113, new BrokerContainsFilter(1113), BrokerItemMask.ARMOR_LEATHER, false),
	ARMOR_LEATHER_PAULDRONS(1123, new BrokerContainsFilter(1123), BrokerItemMask.ARMOR_LEATHER, false),
	ARMOR_LEATHER_PANTS(1133, new BrokerContainsFilter(1133), BrokerItemMask.ARMOR_LEATHER, false),
	ARMOR_LEATHER_SHOES(1143, new BrokerContainsFilter(1143), BrokerItemMask.ARMOR_LEATHER, false),
	ARMOR_CHAIN(8040, new BrokerContainsFilter(1105, 1115, 1125, 1135, 1145), BrokerItemMask.ARMOR, true),
	ARMOR_CHAIN_JACKET(1105, new BrokerContainsFilter(1105), BrokerItemMask.ARMOR_CHAIN, false),
	ARMOR_CHAIN_GLOVES(1115, new BrokerContainsFilter(1115), BrokerItemMask.ARMOR_CHAIN, false),
	ARMOR_CHAIN_PAULDRONS(1125, new BrokerContainsFilter(1125), BrokerItemMask.ARMOR_CHAIN, false),
	ARMOR_CHAIN_PANTS(1135, new BrokerContainsFilter(1135), BrokerItemMask.ARMOR_CHAIN, false),
	ARMOR_CHAIN_SHOES(1145, new BrokerContainsFilter(1145), BrokerItemMask.ARMOR_CHAIN, false),
	ARMOR_PLATE(8050, new BrokerContainsFilter(1106, 1116, 1126, 1136, 1146), BrokerItemMask.ARMOR, true),
	ARMOR_PLATE_JACKET(1106, new BrokerContainsFilter(1106), BrokerItemMask.ARMOR_PLATE, false),
	ARMOR_PLATE_GLOVES(1116, new BrokerContainsFilter(1116), BrokerItemMask.ARMOR_PLATE, false),
	ARMOR_PLATE_PAULDRONS(1126, new BrokerContainsFilter(1126), BrokerItemMask.ARMOR_PLATE, false),
	ARMOR_PLATE_PANTS(1136, new BrokerContainsFilter(1136), BrokerItemMask.ARMOR_PLATE, false),
	ARMOR_PLATE_SHOES(1146, new BrokerContainsFilter(1146), BrokerItemMask.ARMOR_PLATE, false),
	ARMOR_SHIELD(1150, new BrokerContainsFilter(1150), BrokerItemMask.ARMOR, false),
	ARMOR_WINGS(1870, new BrokerContainsFilter(1870), BrokerItemMask.ARMOR, false),

	/**
	 * 饰品分区 + 子分类 / Accessory Section + Sub Categories
	 */
	ACCESSORY(9030, new BrokerMinMaxFilter(1200, 1872), null, true),
	ACCESSORY_EARRINGS(1200, new BrokerContainsFilter(1200), BrokerItemMask.ACCESSORY, false),
	ACCESSORY_NECKLACE(1210, new BrokerContainsFilter(1210), BrokerItemMask.ACCESSORY, false),
	ACCESSORY_RING(1220, new BrokerContainsFilter(1220), BrokerItemMask.ACCESSORY, false),
	ACCESSORY_BELT(1230, new BrokerContainsFilter(1230), BrokerItemMask.ACCESSORY, false),
	ACCESSORY_HEADGEAR(7030, new BrokerMinMaxFilter(1250, 1270), BrokerItemMask.ACCESSORY, false),
	ACCESSORY_PLUME(1871, new BrokerContainsFilter(1871), BrokerItemMask.ACCESSORY, false),
	ACCESSORY_BRACELET(1872, new BrokerContainsFilter(1872), BrokerItemMask.ACCESSORY, false),

	/**
	 * 技能相关分区 + 子分类 / Skill related Section + Sub Categories
	 */
	SKILL_RELATED(9040, new BrokerContainsFilter(1400, 1695), null, true),
	SKILL_RELATED_STIGMA(1400, new BrokerContainsFilter(1400), BrokerItemMask.SKILL_RELATED, true),
	/** 技能相关烙印之石（剑星） / Skill Related Stigma Gladiator */
	SKILL_RELATED_STIGMA_GLADIATOR(6010, new BrokerPlayerClassExtraFilter(1400, PlayerClass.GLADIATOR),
			BrokerItemMask.SKILL_RELATED_STIGMA, false),
	/** 技能相关烙印之石（守护星） / Skill Related Stigma Templar */
	SKILL_RELATED_STIGMA_TEMPLAR(6011, new BrokerPlayerClassExtraFilter(1400, PlayerClass.TEMPLAR),
			BrokerItemMask.SKILL_RELATED_STIGMA, false),
	/** 技能相关烙印之石（杀星） / Skill Related Stigma Assassin */
	SKILL_RELATED_STIGMA_ASSASSIN(6012, new BrokerPlayerClassExtraFilter(1400, PlayerClass.ASSASSIN),
			BrokerItemMask.SKILL_RELATED_STIGMA, false),
	/** 技能相关烙印之石（弓星） / Skill Related Stigma Ranger */
	SKILL_RELATED_STIGMA_RANGER(6013, new BrokerPlayerClassExtraFilter(1400, PlayerClass.RANGER),
			BrokerItemMask.SKILL_RELATED_STIGMA, false),
	/** 技能相关烙印之石（魔道星） / Skill Related Stigma Sorcerer */
	SKILL_RELATED_STIGMA_SORCERER(6014, new BrokerPlayerClassExtraFilter(1400, PlayerClass.SORCERER),
			BrokerItemMask.SKILL_RELATED_STIGMA, false),
	/** 技能相关烙印之石（精灵星） / Skill Related Stigma Spiritmaster */
	SKILL_RELATED_STIGMA_SPIRITMASTER(6015, new BrokerPlayerClassExtraFilter(1400, PlayerClass.SPIRIT_MASTER),
			BrokerItemMask.SKILL_RELATED_STIGMA, false),
	/** 技能相关烙印之石（治愈星） / Skill Related Stigma Cleric */
	SKILL_RELATED_STIGMA_CLERIC(6016, new BrokerPlayerClassExtraFilter(1400, PlayerClass.CLERIC),
			BrokerItemMask.SKILL_RELATED_STIGMA, false),
	/** 技能相关烙印之石（护法星） / Skill Related Stigma Chanter */
	SKILL_RELATED_STIGMA_CHANTER(6017, new BrokerPlayerClassExtraFilter(1400, PlayerClass.CHANTER),
			BrokerItemMask.SKILL_RELATED_STIGMA, false),
	/** 技能相关烙印之石（枪炮星） / Skill Related Stigma Gunslinger */
	SKILL_RELATED_STIGMA_GUNSLINGER(6018, new BrokerPlayerClassExtraFilter(1400, PlayerClass.GUNSLINGER),
			BrokerItemMask.SKILL_RELATED_STIGMA, false),
	/** 技能相关烙印之石（吟游星） / Skill Related Stigma Songweaver */
	SKILL_RELATED_STIGMA_SONGWEAVER(6019, new BrokerPlayerClassExtraFilter(1400, PlayerClass.SONGWEAVER),
			BrokerItemMask.SKILL_RELATED_STIGMA, false),
	/** 技能相关烙印之石（机甲星） / Skill Related Stigma Aethertech */
	SKILL_RELATED_STIGMA_AETHERTECH(6020, new BrokerPlayerClassExtraFilter(1400, PlayerClass.AETHERTECH),
			BrokerItemMask.SKILL_RELATED_STIGMA, false),

	/** 技能相关技能手册 / Skill Related Skill Manual */
	SKILL_RELATED_SKILL_MANUAL(1695, new BrokerContainsFilter(1695), BrokerItemMask.SKILL_RELATED, true),
	/** 技能相关技能手册（剑星） / Skill Related Skill Manual Gladiator */
	SKILL_RELATED_SKILL_MANUAL_GLADIATOR(6020, new BrokerPlayerClassExtraFilter(1695, PlayerClass.GLADIATOR),
			BrokerItemMask.SKILL_RELATED_SKILL_MANUAL, false),
	/** 技能相关技能手册（守护星） / Skill Related Skill Manual Templar */
	SKILL_RELATED_SKILL_MANUAL_TEMPLAR(6021, new BrokerPlayerClassExtraFilter(1695, PlayerClass.TEMPLAR),
			BrokerItemMask.SKILL_RELATED_SKILL_MANUAL, false),
	/** 技能相关技能手册（杀星） / Skill Related Skill Manual Assassin */
	SKILL_RELATED_SKILL_MANUAL_ASSASSIN(6022, new BrokerPlayerClassExtraFilter(1695, PlayerClass.ASSASSIN),
			BrokerItemMask.SKILL_RELATED_SKILL_MANUAL, false),
	/** 技能相关技能手册（弓星） / Skill Related Skill Manual Ranger */
	SKILL_RELATED_SKILL_MANUAL_RANGER(6023, new BrokerPlayerClassExtraFilter(1695, PlayerClass.RANGER),
			BrokerItemMask.SKILL_RELATED_SKILL_MANUAL, false),
	/** 技能相关技能手册（魔道星） / Skill Related Skill Manual Sorcerer */
	SKILL_RELATED_SKILL_MANUAL_SORCERER(6024, new BrokerPlayerClassExtraFilter(1695, PlayerClass.SORCERER),
			BrokerItemMask.SKILL_RELATED_SKILL_MANUAL, false),
	/**
	 * 技能相关技能手册（精灵星）。
	 * Skill Related Skill Manual Spiritmaster.
	 */
	SKILL_RELATED_SKILL_MANUAL_SPIRITMASTER(6025, new BrokerPlayerClassExtraFilter(1695, PlayerClass.SPIRIT_MASTER),
			BrokerItemMask.SKILL_RELATED_SKILL_MANUAL, false),
	/** 技能相关技能手册（治愈星） / Skill Related Skill Manual Cleric */
	SKILL_RELATED_SKILL_MANUAL_CLERIC(6026, new BrokerPlayerClassExtraFilter(1695, PlayerClass.CLERIC),
			BrokerItemMask.SKILL_RELATED_SKILL_MANUAL, false),
	/** 技能相关技能手册（护法星） / Skill Related Skill Manual Chanter */
	SKILL_RELATED_SKILL_MANUAL_CHANTER(6027, new BrokerPlayerClassExtraFilter(1695, PlayerClass.CHANTER),
			BrokerItemMask.SKILL_RELATED_SKILL_MANUAL, false),
	/** 技能相关技能手册（枪炮星） / Skill Related Skill Manual Gunslinger */
	SKILL_RELATED_SKILL_MANUAL_GUNSLINGER(6028, new BrokerPlayerClassExtraFilter(1695, PlayerClass.GUNSLINGER),
			BrokerItemMask.SKILL_RELATED_SKILL_MANUAL, false),
	/** 技能相关技能手册（吟游星） / Skill Related Skill Manual Songweaver */
	SKILL_RELATED_SKILL_MANUAL_SONGWEAVER(6029, new BrokerPlayerClassExtraFilter(1695, PlayerClass.SONGWEAVER),
			BrokerItemMask.SKILL_RELATED_SKILL_MANUAL, false),
	/** 技能相关技能手册（机甲星） / Skill Related Skill Manual Aethertech */
	SKILL_RELATED_SKILL_MANUAL_AETHERTECH(6030, new BrokerPlayerClassExtraFilter(1695, PlayerClass.AETHERTECH),
			BrokerItemMask.SKILL_RELATED_SKILL_MANUAL, false),

	/**
	 * 房屋 + 子分类 / Housing + Sub Categories
	 */
	HOME_DECOR(9070, new BrokerContainsFilter(1710, 1711), null, true),
	/** 室外装饰 / Home Decor Out Door */
	HOME_DECOR_OUT_DOOR(1710, new BrokerContainsFilter(1710), BrokerItemMask.HOME_DECOR, false),
	/** 室内装饰 / Home Decor In Door */
	HOME_DECOR_IN_DOOR(1711, new BrokerContainsFilter(1711), BrokerItemMask.HOME_DECOR, false),

	/** 家具 / Furniture. */
	FURNITURE(9080, new BrokerContainsFilter(1700, 1701, 1702, 1703, 1704), null, true),
	/** 室外家具 / Furniture Out Door */
	FURNITURE_OUT_DOOR(1703, new BrokerContainsFilter(1703), BrokerItemMask.FURNITURE, false),
	/** 室内家具 / Furniture In Door */
	FURNITURE_IN_DOOR(8070, new BrokerContainsFilter(1700, 1701, 1702), BrokerItemMask.FURNITURE, true),
	/** 壁挂式室内家具 / Furniture In Door Wall Mounted */
	FURNITURE_IN_DOOR_WALL_MOUNTED(1700, new BrokerContainsFilter(1700), BrokerItemMask.FURNITURE_IN_DOOR, false),
	/** 独立式室内家具 / Furniture In Door Free Standing */
	FURNITURE_IN_DOOR_FREE_STANDING(1701, new BrokerContainsFilter(1701), BrokerItemMask.FURNITURE_IN_DOOR, false),
	/** 地毯类室内家具 / Furniture In Door Rugs */
	FURNITURE_IN_DOOR_RUGS(1702, new BrokerContainsFilter(1702), BrokerItemMask.FURNITURE_IN_DOOR, false),
	/** 内外通用家具 / Furniture In Door Out Door */
	FURNITURE_IN_DOOR_OUT_DOOR(1704, new BrokerContainsFilter(1704), BrokerItemMask.FURNITURE, false),

	/**
	 * 制作分区 + 子分类 / Craft Section + Sub Categories
	 */
	CRAFT(9050, new BrokerContainsFilter(1520, 1522), null, true),
	/** 制作材料。 / Craft Materials. */
	CRAFT_MATERIALS(1520, new BrokerContainsFilter(1520), BrokerItemMask.CRAFT, true),
	/** 采集类制作材料 / Craft Materials Collection */
	CRAFT_MATERIALS_COLLECTION(6030, new BrokerContainsExtraFilter(15200), BrokerItemMask.CRAFT_MATERIALS, false),
	/** 获取类制作材料 / Craft Materials Gain */
	CRAFT_MATERIALS_GAIN(6031, new BrokerContainsExtraFilter(15201), BrokerItemMask.CRAFT_MATERIALS, false),
	/** 部件类制作材料 / Craft Materials Parts */
	CRAFT_MATERIALS_PARTS(6032, new BrokerContainsExtraFilter(15202), BrokerItemMask.CRAFT_MATERIALS, false),
	/** 制作图纸 / Craft Design */
	CRAFT_DESIGN(1522, new BrokerContainsFilter(1522), BrokerItemMask.CRAFT, true),
	/** 武器锻造图纸 / Craft Design Weaponsmithing */
	CRAFT_DESIGN_WEAPONSMITHING(6040, new BrokerRecipeFilter(40002, 1522), BrokerItemMask.CRAFT_DESIGN, false),
	/** 防具锻造图纸 / Craft Design Armorsmithing */
	CRAFT_DESIGN_ARMORSMITHING(6041, new BrokerRecipeFilter(40003, 1522), BrokerItemMask.CRAFT_DESIGN, false),
	/** 裁缝图纸 / Craft Design Tailoring */
	CRAFT_DESIGN_TAILORING(6042, new BrokerRecipeFilter(40004, 1522), BrokerItemMask.CRAFT_DESIGN, false),
	/** 手工制作图纸 / Craft Design Handicrafting */
	CRAFT_DESIGN_HANDICRAFTING(6043, new BrokerRecipeFilter(40008, 1522), BrokerItemMask.CRAFT_DESIGN, false),
	/** 炼金图纸 / Craft Design Alchemy */
	CRAFT_DESIGN_ALCHEMY(6044, new BrokerRecipeFilter(40007, 1522), BrokerItemMask.CRAFT_DESIGN, false),
	/** 烹饪图纸 / Craft Design Cooking */
	CRAFT_DESIGN_COOKING(6045, new BrokerRecipeFilter(40001, 1522), BrokerItemMask.CRAFT_DESIGN, false),
	/** 建造图纸 / Craft Design Construction */
	CRAFT_DESIGN_CONSTRUCTION(6046, new BrokerRecipeFilter(40010, 1522), BrokerItemMask.CRAFT_DESIGN, false),

	/**
	 * 消耗品分区 + 子分类 / Consumables Section + Sub Categories
	 */
	CONSUMABLES(9060,
			new BrokerContainsFilter(1410, 1600, 1620, 1640, 1650, 1660, 16603, 1661, 1665, 1670, 1680, 1690, 16912,
					1692, 1693, 1694, 1696, 1873, 1900),
			null, true),
	/** 消耗品·食物 / Consumables Food */
	CONSUMABLES_FOOD(1600, new BrokerContainsFilter(1600), BrokerItemMask.CONSUMABLES, false),
	/** 消耗品·药水 / Consumables Potion */
	CONSUMABLES_POTION(1620, new BrokerContainsFilter(1620), BrokerItemMask.CONSUMABLES, false),
	/** 消耗品·卷轴 / Consumables Scroll */
	CONSUMABLES_SCROLL(7060, new BrokerContainsFilter(1640), BrokerItemMask.CONSUMABLES, false),
	/** 消耗品·强化类 / Consumables Modify */
	CONSUMABLES_MODIFY(8060, new BrokerContainsFilter(1650, 1660, 1665, 1670, 16603, 1680, 1692, 16912, 1873),
			BrokerItemMask.CONSUMABLES, true),
	/**
	 * 消耗品·强化石。 / Consumables Modify Enchantment Stone
	 */
	CONSUMABLES_MODIFY_ENCHANTMENT_STONE(1660, new BrokerContainsFilter(1660), BrokerItemMask.CONSUMABLES_MODIFY,
			false),
	/** 消耗品·魔石 / Consumables Modify Manastone */
	CONSUMABLES_MODIFY_MANASTONE(1670, new BrokerContainsFilter(1670), BrokerItemMask.CONSUMABLES_MODIFY, false),
	/** 消耗品·提炼石 / Consumables Modify Tempering */
	CONSUMABLES_MODIFY_TEMPERING(7064, new BrokerContainsExtraFilter(16603), BrokerItemMask.CONSUMABLES_MODIFY, false),
	/** 消耗品·神石 / Consumables Modify Godstone */
	CONSUMABLES_MODIFY_GODSTONE(1680, new BrokerContainsFilter(1680), BrokerItemMask.CONSUMABLES_MODIFY, false),
	/** 消耗品·染色剂 / Consumables Modify Dye */
	CONSUMABLES_MODIFY_DYE(7061, new BrokerContainsFilter(1692), BrokerItemMask.CONSUMABLES_MODIFY, false),
	/** 消耗品·涂装 / Consumables Modify Paint */
	CONSUMABLES_MODIFY_PAINT(7065, new BrokerContainsExtraFilter(16912), BrokerItemMask.CONSUMABLES_MODIFY, false),
	/**
	 * 消耗品·增幅石。 / Consumables Modify Amplification Stone
	 */
	CONSUMABLES_MODIFY_AMPLIFICATION_STONE(7066, new BrokerContainsFilter(1665), BrokerItemMask.CONSUMABLES_MODIFY,
			false),
	/** 消耗品·还原石 / Consumables Modify Reduction Stone */
	CONSUMABLES_MODIFY_REDUCTION_STONE(1650, new BrokerContainsFilter(1650), BrokerItemMask.CONSUMABLES_MODIFY, false),
	/** 消耗品·精髓核心 / Consumables Modify Essence Core */
	CONSUMABLES_MODIFY_ESSENCE_CORE(1873, new BrokerContainsFilter(1873), BrokerItemMask.CONSUMABLES_MODIFY, false),
	/** 消耗品·其他强化 / Consumables Modify Other */
	CONSUMABLES_MODIFY_OTHER(7063, new BrokerContainsFilter(1661), BrokerItemMask.CONSUMABLES_MODIFY, false),
	/** 消耗品·其他 / Consumables Other */
	CONSUMABLES_OTHER(7062, new BrokerContainsFilter(1410, 1690, 1693, 1694, 1696, 1900), BrokerItemMask.CONSUMABLES,
			false),

	/**
	 * 其他分区。
	 * Other Section.
	 */
	OTHER(7070, new BrokerContainsFilter(1850, 1860, 1880, 1881, 1887), null, false),
	/** 未知 / Unknown. */
	UNKNOWN(1, new BrokerContainsFilter(0), null, false);

	private int typeId;
	private BrokerFilter filter;
	private BrokerItemMask parent;
	private boolean childrenExist;

	private BrokerItemMask(int typeId, BrokerFilter filter, BrokerItemMask parent, boolean childrenExist) {
		this.typeId = typeId;
		this.filter = filter;
		this.parent = parent;
		this.childrenExist = childrenExist;
	}

	/** 返回 ID / Returns the id */
	public int getId() {
		return typeId;
	}

	/**
	 * 判断物品是否匹配该掩码过滤器。
	 * Whether the item matches this mask filter.
	 *
	 * @param item 待匹配物品 / item
	 * @return 是否匹配 / whether matches
	 */
	public boolean isMatches(Item item) {
		return filter.accept(item.getItemTemplate());
	}

	/**
	 * 判断掩码 ID 是否为当前掩码的父级掩码。
	 * Whether the mask id is an ancestor of this mask.
	 *
	 * @param maskId 掩码 ID / mask id
	 * @return 是否为父级掩码 / whether children mask
	 */
	public boolean isChildrenMask(int maskId) {
		for (BrokerItemMask p = parent; p != null; p = p.parent) {
			if (p.typeId == maskId) {
				return true;
			}
		}
		return false;
	}

	/** 按 ID 返回 broker mask / Returns the broker mask by id */
	public static BrokerItemMask getBrokerMaskById(int id) {
		for (BrokerItemMask mt : values()) {
			if (mt.typeId == id) {
				return mt;
			}
		}
		return UNKNOWN;
	}

	/**
	 * 判断是否存在子分类。
	 * Whether this mask has children.
	 *
	 * @return 是否有子分类 / whether children
	 */
	public boolean hasChildren() {
		return childrenExist;
	}
}
