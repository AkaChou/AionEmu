package com.aionemu.gameserver.model.templates.item;

import java.util.List;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlID;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.XmlType;

import org.apache.commons.lang3.StringUtils;

import com.aionemu.gameserver.configs.main.CustomConfig;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.items.ItemId;
import com.aionemu.gameserver.model.items.ItemMask;
import com.aionemu.gameserver.model.stats.calc.functions.StatFunction;
import com.aionemu.gameserver.model.templates.VisibleObjectTemplate;
import com.aionemu.gameserver.model.templates.item.actions.ItemActions;
import com.aionemu.gameserver.model.templates.itemset.ItemSetTemplate;
import com.aionemu.gameserver.model.templates.stats.ModifiersTemplate;
import com.aionemu.gameserver.world.zone.ZoneName;

/**
 * 物品模板（静态数据/XML）。
 * XML template.
 */

@XmlAccessorType(XmlAccessType.NONE)
@XmlType(namespace = "", name = "ItemTemplate")
public class ItemTemplate extends VisibleObjectTemplate {
	@XmlAttribute(name = "id", required = true)
	@XmlID
	private String id;
	private int itemId;

	@XmlElement(name = "modifiers", required = false)
	protected ModifiersTemplate modifiers;

	@XmlAttribute(name = "descr")
	private String descr;

	@XmlElement(name = "actions", required = false)
	protected ItemActions actions;

	@XmlAttribute(name = "name_desc")
	private String namedesc;

	@XmlAttribute(name = "mask")
	private int mask;

	@XmlAttribute(name = "category")
	private ItemCategory category = ItemCategory.NONE;

	@XmlAttribute(name = "slot")
	private int itemSlot;

	@XmlAttribute(name = "equipment_type")
	private EquipType equipmentType = EquipType.NONE;

	@XmlAttribute(name = "weapon_boost")
	private int weaponBoost;

	@XmlAttribute(name = "price")
	private int price;

	@XmlAttribute(name = "luna_price")
	private int lunaPrice;

	@XmlAttribute(name = "robot_id")
	private int robot_id;

	@XmlAttribute(name = "abyss_point")
	private int abyssPoint;

	@XmlAttribute(name = "max_stack_count")
	private int maxStackCount = 1;

	@XmlAttribute(name = "unit_sell_count")
	private int unitSellCount = 1;

	@XmlAttribute(name = "level")
	private int level;

	@XmlAttribute(name = "quality")
	private ItemQuality itemQuality;

	@XmlAttribute(name = "item_type")
	private ItemType itemType;

	@XmlAttribute(name = "weapon_type")
	private WeaponType weaponType;

	@XmlAttribute(name = "armor_type")
	private ArmorType armorType;

	@XmlAttribute(name = "attack_type")
	private ItemAttackType attackType;

	@XmlAttribute(name = "attack_gap")
	private float attackGap;

	@XmlAttribute(name = "desc")
	private String description;

	@XmlAttribute(name = "option_slot_bonus")
	private int optionSlotBonus;

	@XmlAttribute(name = "rnd_bonus")
	private int rnd_bonus = 0;

	@XmlAttribute(name = "rnd_count")
	private int rnd_count = 0;

	@XmlAttribute(name = "wrappable_count")
	private int wrappable_count = 0;

	@XmlAttribute(name = "max_authorize")
	private int maxAuthorize;

	@XmlAttribute(name = "tempering_table_id")
	private int temperingTableId;

	@XmlAttribute(name = "robot_name")
	private int robotName = 0;

	@XmlAttribute(name = "bonus_apply")
	private String bonusApply;

	@XmlAttribute(name = "no_enchant")
	private boolean noEnchant;

	@XmlAttribute(name = "dye")
	private boolean itemDyePermitted;

	@XmlAttribute(name = "race")
	private Race race = Race.PC_ALL;

	@XmlAttribute(name = "return_world")
	private int returnWorldId;

	@XmlAttribute(name = "return_alias")
	private String returnAlias;

	@XmlElement(name = "godstone")
	private GodstoneInfo godstoneInfo;

	@XmlElement(name = "stigma")
	private Stigma stigma;

	@XmlAttribute(name = "name")
	private String name;

	@XmlAttribute(name = "restrict")
	private String restrict;

	@XmlAttribute(name = "restrict_max")
	private String restrictMax;

	@XmlTransient
	private int[] restricts;

	@XmlTransient
	private byte[] restrictsMax;

	@XmlAttribute(name = "m_slots")
	private int manastoneSlots;

	@XmlAttribute(name = "s_slots")
	private int specialSlots;

	@XmlAttribute(name = "max_enchant")
	private int maxEnchant;

	@XmlAttribute(name = "max_enchant_bonus")
	private int max_enchant_bonus;

	@XmlAttribute(name = "temp_exchange_time")
	protected int temExchangeTime;

	@XmlAttribute(name = "expire_time")
	protected int expireTime;

	@XmlElement(name = "weapon_stats")
	protected WeaponStats weaponStats;

	@XmlAttribute(name = "activate_count")
	private int activationCount;

	@XmlAttribute(name = "func_pet_id")
	private int funcPetId;

	@XmlElement(name = "tradein_list")
	protected TradeinList tradeinList;

	@XmlElement(name = "acquisition")
	private Acquisition acquisition;

	@XmlElement(name = "disposition")
	private Disposition disposition;

	@XmlElement(name = "improve")
	private Improvement improvement;

	@XmlElement(name = "uselimits")
	private ItemUseLimits useLimits = new ItemUseLimits();

	@XmlElement(name = "purchable")
	private ItemPurchableLimits purchableLimits = new ItemPurchableLimits();

	@XmlElement(name = "inventory")
	private ExtraInventory extraInventory;

	@XmlElement(name = "idian")
	private Idian idianAction;

	@XmlTransient
	private boolean isQuestUpdateItem;

	@XmlAttribute(name = "skill_group")
	private String skill_group;

	@XmlAttribute(name = "skin_skill")
	private int skin_skill;

	@XmlAttribute(name = "skill_enchant")
	private int skill_enchant;

	@XmlAttribute(name = "enchant_base")
	private int enchant_base = 0;

	@XmlAttribute(name = "item_custom_set")
	private int itemCustomSet = 0;

	@XmlAttribute(name = "minion_ticket")
	private boolean minion_ticket;

	@XmlAttribute(name = "is_cash_contract")
	private boolean is_cash_contract;

	private static final WeaponStats emptyWeaponStats = new WeaponStats();

	/**
	 * @param u
	 * @param parent
	 */
	void afterUnmarshal(Unmarshaller u, Object parent) {
		setItemId(Integer.parseInt(id));
		String[] parts = restrict.split(",");
		restricts = new int[17]; // 4.3/4.5 Restriction.
		for (int i = 0; i < parts.length; i++) {
			restricts[i] = Integer.parseInt(parts[i]);
		}
		if (restrictMax != null) {
			String[] partsMax = restrictMax.split(",");
			restrictsMax = new byte[17]; // 4.3/4.5 Restriction.
			for (int i = 0; i < partsMax.length; i++) {
				restrictsMax[i] = Byte.parseByte(partsMax[i]);
			}
		}
		if (weaponStats == null) {
			weaponStats = emptyWeaponStats;
		}
	}

	public byte getMaxLevelRestrict(Player player) {
		if (restrictMax != null) {
			byte restrictId = player.getPlayerClass().getClassId();
			byte restrictLevel = restrictsMax[restrictId];
			return player.getLevel() <= restrictLevel ? 0 : restrictLevel;
		}
		return 0;
	}

	public String getId() {
		return id;
	}

	public int getMask() {
		return mask;
	}

	public ItemCategory getCategory() {
		return category;
	}

	public int getItemSlot() {
		return itemSlot;
	}

	/**
	 * @param playerClass
	 * @return
	 */
	public boolean isClassSpecific(PlayerClass playerClass) {
		boolean related = restricts[playerClass.ordinal()] > 0;
		if (!related && !playerClass.isStartingClass()) {
			related = restricts[PlayerClass.getStartingClassFor(playerClass).ordinal()] > 0;
		}
		return related;
	}

	/**
	 * @param playerClass
	 * @return
	 */
	public int getRequiredLevel(PlayerClass playerClass) {
		int requiredLevel = restricts[playerClass.ordinal()];
		// 玩家可装备 66–83 物品，但未必应用完整属性。 / A player can equip item between 66-83 but have not full stats apply.
		if (requiredLevel >= 66 && requiredLevel <= 83) {
			return 66;
		}
		if (requiredLevel == 0) {
			return -1;
		} else {
			return requiredLevel;
		}
	}

	public List<StatFunction> getModifiers() {
		if (modifiers != null) {
			return modifiers.getModifiers();
		}
		return null;
	}

	public ItemActions getActions() {
		return actions;
	}

	public EquipType getEquipmentType() {
		return equipmentType;
	}

	public int getPrice() {
		return price;
	}

	public int getLunaPrice() {
		return lunaPrice;
	}

	public int getRobotId() {
		return robot_id;
	}

	public int getAbyssPoint() {
		return abyssPoint;
	}

	public int getLevel() {
		return level;
	}

	public ItemQuality getItemQuality() {
		return itemQuality;
	}

	public ItemType getItemType() {
		return itemType;
	}

	public WeaponType getWeaponType() {
		return weaponType;
	}

	public ArmorType getArmorType() {
		if (isPlume()) {
			return ArmorType.PLUME;
		}
		if (isBracelet()) {
			return ArmorType.BRACELET;
		}
		return armorType;
	}

	@Override
	public int getNameId() {
		try {
			int val = Integer.parseInt(description);
			return val;
		} catch (NumberFormatException nfe) {
			return 0;
		}
	}

	public long getMaxStackCount() {
		if (isKinah()) {
			if (CustomConfig.ENABLE_KINAH_CAP) {
				return CustomConfig.KINAH_CAP_VALUE;
			} else {
				return Long.MAX_VALUE;
			}
		}
		if (isLuna()) {
			if (CustomConfig.ENABLE_LUNA_CAP) {
				return CustomConfig.LUNA_CAP_VALUE;
			} else {
				return Long.MAX_VALUE;
			}
		}
		return maxStackCount;
	}

	public ItemAttackType getAttackType() {
		return attackType;
	}

	public float getAttackGap() {
		return attackGap;
	}

	public int getOptionSlotBonus() {
		return optionSlotBonus;
	}

	public String getBonusApply() {
		return bonusApply;
	}

	/** 是否无强化 / Whether no enchant */
	public boolean isNoEnchant() {
		return (getMask() & ItemMask.NO_ENCHANT) == ItemMask.NO_ENCHANT;
	}

	/** 是否允许物品染色 / Whether item dye permitted */
	public boolean isItemDyePermitted() {
		return (getMask() & ItemMask.DYEABLE) == ItemMask.DYEABLE;
	}

	/** 获取种族。 / Returns the race. */
	public Race getRace() {
		return race;
	}

	/** 返回武器加速 / Returns the weapon boost*/
	public int getWeaponBoost() {
		return weaponBoost;
	}

	/** 是否武器 / Whether weapon*/
	public boolean isWeapon() {
		return equipmentType == EquipType.WEAPON;
	}

	/** 是否防具 / Whether armor*/
	public boolean isArmor() {
		return equipmentType == EquipType.ARMOR;
	}

	/** 是否为基纳。 / Whether kinah. */
	public boolean isKinah() {
		return itemId == ItemId.KINAH.value();
	}

	/** 是否为月华。 / Whether luna. */
	public boolean isLuna() {
		return itemId == ItemId.LUNA.value();
	}

	/** 是否烙印之石 / Whether stigma*/
	public boolean isStigma() {
		return itemId >= 140000001 && itemId <= 140001493; // Last Stigma Stone in 4.8
	}

	/**
	 * @return Whether cp stones
	 */
	public boolean isCpStones() {
		return itemId >= 187300002 && itemId <= 187300005; // 5.6
	}

	/**
	 * @return 是否为羽饰。 / Whether plume
	  */
	public boolean isPlume() {
		return category == ItemCategory.PLUME;
	}

	/**
	 * @return Whether bracelet
	 */
	public boolean isBracelet() {
		return category == ItemCategory.BRACELET;
	}

	/**
	 * @return Whether mana stone
	 */
	public boolean isManaStone() {
		return category == ItemCategory.MANASTONE || category == ItemCategory.SPECIAL_MANASTONE
				|| category == ItemCategory.PRIMARY_MANASTONE;
	}

	/**
	 * @return Whether estima
	 */
	public boolean isEstima() {
		return category == ItemCategory.ESTIMA;
	}

	/**
	 * @return Whether tempering solution
	 */
	public boolean isTemperingSolution() {
		return category == ItemCategory.TEMPERING;
	}

	/** Whetherinert 烙印之石 / Whether inert stigma */
	public boolean isInertStigma() {
		return name.endsWith("(Inert)");
	}

	/** 设置物品 ID / Sets the item id */
	public void setItemId(int itemId) {
		this.itemId = itemId;
	}

	/**
	 * @return id of the associated ItemSetTemplate or null if none
	 */
	public ItemSetTemplate getItemSet() {
		return DataManager.ITEM_SET_DATA.getItemSetTemplateByItemId(itemId);
	}

	/**
	 * 检查是否 ItemTemplatebelongs 到物品设置。 / Checks if the ItemTemplate belongs to an item set
	 */
	public boolean isItemSet() {
		return getItemSet() != null;
	}

	/** 返回神石信息 / Returns the godstone info*/
	public GodstoneInfo getGodstoneInfo() {
		return godstoneInfo;
	}

	/** 获取名称。 / Returns the name. */
	@Override
	public String getName() {
		return name != null ? name : StringUtils.EMPTY;
	}

	/** 返回模板 ID / Returns the template id */
	@Override
	public int getTemplateId() {
		return itemId;
	}

	/** 返回返回世界 ID / Returns the return world id */
	public int getReturnWorldId() {
		return returnWorldId;
	}

	/** 返回 return alias / Returns the return alias */
	public String getReturnAlias() {
		return returnAlias;
	}

	/** 返回烙印之石 / Returns the stigma*/
	public Stigma getStigma() {
		return stigma;
	}

	/** 返回魔石槽位 / Returns the manastone slots*/
	public int getManastoneSlots() {
		return manastoneSlots;
	}

	/** 返回特殊槽位 / Returns the special slots*/
	public int getSpecialSlots() {
		return specialSlots;
	}

	/** 返回最大强化等级 / Returns the max enchant level*/
	public int getMaxEnchantLevel() {
		return maxEnchant;
	}

	/** 返回 max enchant bonus / Returns the max enchant bonus */
	public int getMaxEnchantBonus() {
		return max_enchant_bonus;
	}

	/**
	 * @return 是否限制为一个。 / Whether limit one
	  */
	public boolean hasLimitOne() {
		return (getMask() & ItemMask.LIMIT_ONE) == ItemMask.LIMIT_ONE;
	}

	/**
	 * @return Whether tradeable
	 */
	public boolean isTradeable() {
		return (getMask() & ItemMask.TRADEABLE) == ItemMask.TRADEABLE;
	}

	/**
	 * @return 是否可合成。 / Whether can fuse
	  */
	public boolean isCanFuse() {
		return (getMask() & ItemMask.CAN_COMPOSITE_WEAPON) == ItemMask.CAN_COMPOSITE_WEAPON;
	}

	/**
	 * @return Whether extract
	 */
	public boolean canExtract() {
		return (getMask() & ItemMask.CAN_SPLIT) == ItemMask.CAN_SPLIT;
	}

	/**
	 * @return Whether soul bound
	 */
	public boolean isSoulBound() {
		return (getMask() & ItemMask.SOUL_BOUND) == ItemMask.SOUL_BOUND;
	}

	/**
	 * @return Whether breakable
	 */
	public boolean isBreakable() {
		return (getMask() & ItemMask.BREAKABLE) == ItemMask.BREAKABLE;
	}

	/**
	 * @return Whether deletable
	 */
	public boolean isDeletable() {
		return (getMask() & ItemMask.DELETABLE) == ItemMask.DELETABLE;
	}

	/**
	 * @return 是否可镶嵌伊迪安。 / Whether can idian
	  */
	public boolean isCanIdian() {
		return (getMask() & ItemMask.CAN_IDIAN) == ItemMask.CAN_IDIAN;
	}

	/**
	 * @return Whether archdaeva
	 */
	public boolean isArchdaeva() {
		return (getMask() & ItemMask.ITEM_ARCHDAEVA) == ItemMask.ITEM_ARCHDAEVA;
	}

	/** 是否为双手武器 / Whether two hand weapon */
	public boolean isTwoHandWeapon() {
		if (!isWeapon()) {
			return false;
		}
		return weaponType.getRequiredSlots() == 2;
	}

	/** 返回 temp exchange time / Returns the temp exchange time */
	public int getTempExchangeTime() {
		return temExchangeTime;
	}

	/** 获取过期时间。 / Returns the expire time. */
	public int getExpireTime() {
		return expireTime;
	}

	/** 返回 weapon stats / Returns the weapon stats */
	public final WeaponStats getWeaponStats() {
		return weaponStats;
	}

	/** 返回 activation count / Returns the activation count */
	public int getActivationCount() {
		return activationCount;
	}

	/** 返回 func pet id / Returns the func pet id */
	public final int getFuncPetId() {
		return funcPetId;
	}

	/** Modify Mask / Modify Mask */
	public void modifyMask(boolean apply, int filter) {
		if (apply) {
			mask |= filter;
		} else {
			mask &= ~filter;
		}
	}

	/**
	 * @return Whether stackable
	 */
	public boolean isStackable() {
		return this.maxStackCount > 1;
	}

	/**
	 * @return Whether area restriction
	 */
	public boolean hasAreaRestriction() {
		return useLimits.getUseArea() != null;
	}

	/** 返回 use area / Returns the use area */
	public ZoneName getUseArea() {
		return useLimits.getUseArea();
	}

	/** 返回 tradein list / Returns the tradein list */
	public TradeinList getTradeinList() {
		return tradeinList;
	}

	/** 返回 acquisition / Returns the acquisition */
	public Acquisition getAcquisition() {
		return acquisition;
	}

	/** 返回 random bonus id / Returns the random bonus id */
	public int getRandomBonusId() {
		return rnd_bonus;
	}

	/** 返回随机加成数量 / Returns the random bonus count */
	public int getRandomBonusCount() {
		return rnd_count;
	}

	/** 返回 wrappable count / Returns the wrappable count */
	public int getWrappableCount() {
		return wrappable_count;
	}

	/** 返回 max authorize / Returns the max authorize */
	public int getMaxAuthorize() {
		return maxAuthorize;
	}

	/** 返回 tempering table id / Returns the tempering table id */
	public int getTemperingTableId() {
		return temperingTableId;
	}

	/** 返回 robot name / Returns the robot name */
	public int getRobotName() {
		return robotName;
	}

	/** 返回 improvement / Returns the improvement */
	public Improvement getImprovement() {
		return improvement;
	}

	/** 返回 use limits / Returns the use limits */
	public ItemUseLimits getUseLimits() {
		return useLimits;
	}

	/** 返回 purchable limits / Returns the purchable limits */
	public ItemPurchableLimits getPurchableLimits() {
		return purchableLimits;
	}

	/** 返回 disposition / Returns the disposition */
	public Disposition getDisposition() {
		return disposition;
	}

	/** 返回 ownership world / Returns the ownership world */
	public int getOwnershipWorld() {
		return useLimits.getOwnershipWorld();
	}

	/** 获取伊迪安动作。 / Returns the idian action. */
	public Idian getIdianAction() {
		return idianAction;
	}

	/** 是否为组合物品 / Whether combination item */
	public boolean isCombinationItem() {
		return category == ItemCategory.COMBINATION;
	}

	/**
	 * @return Whether enchantment stone
	 */
	public boolean isEnchantmentStone() {
		return category == ItemCategory.ENCHANTMENT;
	}

	/** Whetherenchantment 烙印之石 stone / Whether enchantment stigma stone */
	public boolean isEnchantmentStigmaStone() {
		return category == ItemCategory.ENCHANTMENT_STIGMA;
	}

	/**
	 * @return Whether amplification stone
	 */
	public boolean isAmplificationStone() {
		return category == ItemCategory.ENCHANTMENT_AMPLIFICATION;
	}

	/**
	 * @return 是否为布甲。 / Whether cloth
	  */
	public boolean isCloth() {
		return armorType != null && equipmentType == EquipType.ARMOR;
	}

	/**
	 * @return Whether ancient stone
	 */
	public boolean isAncientStone() {
		// 古代魔石：生命 +105 与【印章】古代魔石：治疗增强 +6。 / Ancient Manastone: HP +105 && //[Stamp] Ancient Manastone: Healing Boost +6
		return itemId >= 167020000 && itemId <= 167020112;
	}

	/**
	 * @return Whether accessory
	 */
	public boolean isAccessory() {
		return category == ItemCategory.EARRINGS || category == ItemCategory.RINGS || category == ItemCategory.NECKLACE
				|| category == ItemCategory.PLUME || category == ItemCategory.BRACELET || category == ItemCategory.BELT
				|| category == ItemCategory.HELMET;
	}

	/** 是否任务更新物品 / Whether quest update item*/
	public boolean isQuestUpdateItem() {
		return isQuestUpdateItem;
	}

	/** 设置任务更新物品 / Sets the quest update item*/
	public void setQuestUpdateItem(boolean value) {
		this.isQuestUpdateItem = value;
	}

	/** 返回 extra inventory id / Returns the extra inventory id */
	public int getExtraInventoryId() {
		if (extraInventory == null) {
			return -1;
		}
		return extraInventory.getId();
	}

	/** 获取技能队伍。 / Returns the skill group. */
	public String getSkillGroup() {
		return skill_group;
	}

	/** 获取外观技能。 / Returns the skin skill. */
	public int getSkinSkill() {
		return skin_skill;
	}

	/** 返回技能强化 / Returns the skill enchant*/
	public int getSkillEnchant() {
		return skill_enchant;
	}

	/** 返回基础强化 / Returns the base enchant*/
	public int getBaseEnchant() {
		return enchant_base;
	}

	/** 返回 item custom set / Returns the item custom set */
	public int getItemCustomSet() {
		return itemCustomSet;
	}

	/** 返回 minion ticket / Returns the minion ticket */
	public boolean getMinionTicket() {
		return this.minion_ticket;
	}

	/**
	 * @return Whether minion cash contract
	 */
	public boolean isMinionCashContract() {
		return this.is_cash_contract;
	}

	/** 返回 skill enhance / Returns the skill enhance */
	public int getSkillEnhance() {
		return skill_enchant;
	}

	/** 返回 descr / Returns the descr */
	public String getDescr() {
		return descr;
	}

	/** 返回 namedesc / Returns the namedesc */
	public String getNamedesc() {
		return namedesc;
	}
}
