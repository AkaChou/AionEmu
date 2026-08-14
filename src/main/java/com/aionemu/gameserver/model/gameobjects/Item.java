package com.aionemu.gameserver.model.gameobjects;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import com.aionemu.gameserver.configs.main.MembershipConfig;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.DescriptionId;
import com.aionemu.gameserver.model.IExpirable;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.items.ChargeInfo;
import com.aionemu.gameserver.model.items.GodStone;
import com.aionemu.gameserver.model.items.IdianStone;
import com.aionemu.gameserver.model.items.ItemMask;
import com.aionemu.gameserver.model.items.ManaStone;
import com.aionemu.gameserver.model.items.RandomBonusResult;
import com.aionemu.gameserver.model.items.RandomStats;
import com.aionemu.gameserver.model.items.storage.IStorage;
import com.aionemu.gameserver.model.items.storage.ItemStorage;
import com.aionemu.gameserver.model.items.storage.StorageType;
import com.aionemu.gameserver.model.stats.calc.StatOwner;
import com.aionemu.gameserver.model.stats.calc.functions.StatFunction;
import com.aionemu.gameserver.model.templates.item.EquipType;
import com.aionemu.gameserver.model.templates.item.Improvement;
import com.aionemu.gameserver.model.templates.item.ItemTemplate;
import com.aionemu.gameserver.model.templates.item.actions.DyeAction;
import com.aionemu.gameserver.model.templates.item.actions.ItemActions;
import com.aionemu.gameserver.model.templates.item.bonuses.StatBonusType;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 物品游戏对象。
 * Item game object.
 *
 * @author ATracer, Wakizashi, xTz
 */
@Slf4j
public class Item extends AionObject implements IExpirable, StatOwner {

	private long itemCount = 1;
	private int itemColor = 0;
	private int colorExpireTime = 0;
	private String itemCreator;
	private ItemTemplate itemTemplate;
	private ItemTemplate itemSkinTemplate;
	private ItemTemplate fusionedItemTemplate;
	private boolean isEquipped = false;
	private long equipmentSlot = ItemStorage.FIRST_AVAILABLE_SLOT;
	private PersistentState persistentState;
	private Set<ManaStone> manaStones;
	private Set<ManaStone> fusionStones;
	private int optionalSocket;
	private int optionalFusionSocket;
	private int enchant_bonus;
	private GodStone godStone;
	private IdianStone idianStone;
	private boolean isSoulBound = false;
	private int itemLocation;
	private int enchantLevel;
	private int expireTime = 0;
	private int temporaryExchangeTime = 0;
	private long repurchasePrice;
	private int activationCount = 0;
	private ChargeInfo conditioningInfo;
	private int bonusNumber = 0;
	private List<StatFunction> currentModifiers;
	private RandomStats randomStats;
	private int rndCount;
	public static int MAX_BASIC_STONES = 6;
	private int wrappableCount;
	private boolean isPacked = false;
	private int authorize;
	private boolean amplification = false;
	private int amplificationSkill = 0;
	private int SkinSkill = 0;
	private boolean luna_reskin = false;
	private int ReductionLevel = 0;
	private int unSeal = 0;
	private boolean canEnhance;
	private int enhanceSkillId;
	private int enhanceEnchantLevel;

	public Item(int objId, ItemTemplate itemTemplate) {
		super(objId);
		this.itemTemplate = itemTemplate;
		this.activationCount = itemTemplate.getActivationCount();
		if (itemTemplate.getExpireTime() != 0) {
			expireTime = ((int) (System.currentTimeMillis() / 1000) + itemTemplate.getExpireTime() * 60) - 1;
		}
		int optionSlotBonus = itemTemplate.getOptionSlotBonus();
		if (optionSlotBonus != 0) {
			optionalSocket = -1;
		}
		if (this.itemTemplate.getSkinSkill() != 0) {
			SkinSkill = itemTemplate.getSkinSkill();
		}
		this.persistentState = PersistentState.NEW;
		updateChargeInfo(0);
	}

	public Item(int objId, ItemTemplate itemTemplate, long itemCount, boolean isEquipped, long equipmentSlot) {
		this(objId, itemTemplate);
		this.itemCount = itemCount;
		this.isEquipped = isEquipped;
		this.equipmentSlot = equipmentSlot;
	}

	public Item(int objId, int itemId, long itemCount, int itemColor, int colorExpires, String itemCreator,
			int expireTime, int activationCount, boolean isEquipped, boolean isSoulBound, long equipmentSlot,
			int itemLocation, int enchant, int enchantBonus, int itemSkin, int fusionedItem, int optionalSocket,
			int optionalFusionSocket, int charge, int randomBonus, int rndCount, int wrappableCount, boolean isPacked,
			int authorize, boolean amplification, int amplificationSkill, int SkinSkill, boolean lunaReskin,
			int reuctionLevel, int unSeal, boolean isEnhance, int enhanceSkillId, int enhanceEnchantLevel) {
		super(objId);
		this.itemTemplate = DataManager.ITEM_DATA.getItemTemplate(itemId);
		this.itemCount = itemCount;
		this.itemColor = itemColor;
		this.colorExpireTime = colorExpires;
		this.itemCreator = itemCreator;
		this.expireTime = expireTime;
		this.activationCount = activationCount;
		this.isEquipped = isEquipped;
		this.isSoulBound = isSoulBound;
		this.equipmentSlot = equipmentSlot;
		this.itemLocation = itemLocation;
		this.enchantLevel = enchant;
		this.enchant_bonus = enchantBonus;
		this.fusionedItemTemplate = DataManager.ITEM_DATA.getItemTemplate(fusionedItem);
		this.itemSkinTemplate = DataManager.ITEM_DATA.getItemTemplate(itemSkin);
		this.optionalSocket = optionalSocket;
		this.optionalFusionSocket = optionalFusionSocket;
		this.bonusNumber = randomBonus;
		this.rndCount = rndCount;
		this.wrappableCount = wrappableCount;
		this.authorize = authorize;
		this.isPacked = isPacked;
		this.amplification = amplification;
		this.amplificationSkill = amplificationSkill;
		this.SkinSkill = SkinSkill;
		this.luna_reskin = lunaReskin;
		if (itemTemplate.getRandomBonusId() != 0 && bonusNumber > 0) {
			randomStats = new RandomStats(itemTemplate.getRandomBonusId(), bonusNumber);
		}
		if (fusionedItemTemplate != null) {
			if (!itemTemplate.isCanFuse() || !itemTemplate.isTwoHandWeapon() || !fusionedItemTemplate.isCanFuse()
					|| !fusionedItemTemplate.isTwoHandWeapon()) {
				this.fusionedItemTemplate = null;
				this.optionalFusionSocket = 0;
			}
		}
		this.ReductionLevel = reuctionLevel;
		this.canEnhance = isEnhance;
		this.enhanceSkillId = enhanceSkillId;
		this.enhanceEnchantLevel = enhanceEnchantLevel;
		updateChargeInfo(charge);
	}

	public final boolean setRndBonus() {
		int setId = itemTemplate.getRandomBonusId();
		if (setId > 0) {
			RandomBonusResult bonus = DataManager.ITEM_RANDOM_BONUSES.getRandomModifiers(StatBonusType.INVENTORY,
					setId);
			if (bonus != null) {
				bonusNumber = bonus.getTemplateNumber();
				randomStats = new RandomStats(itemTemplate.getRandomBonusId(), bonusNumber);
				return true;
			}
		}
		return false;
	}

	private void updateChargeInfo(int charge) {
		int chargeLevel = getChargeLevelMax();
		if (conditioningInfo == null && chargeLevel > 0) {
			this.conditioningInfo = new ChargeInfo(charge, this);
		}
		// 拆解融合物品且第二件有调谐信息时——设为 null。 / when break fusioned item and second item has conditioned info - set to null
		if (conditioningInfo != null && chargeLevel == 0) {
			this.conditioningInfo = null;
		}
	}

	public boolean hasRetuning() {
		if (getOptionalSocket() == -1) {
			return true;
		}
		return false;
	}

	@Override
	public String getName() {
		return getItemName();
	}

	/**
	 * 返回物品制作者。
	 * Returns the item creator.
	 *
	 * @return 物品制作者 / item creator
	 */
	public String getItemCreator() {
		if (itemCreator == null) {
			return StringUtils.EMPTY;
		}
		return itemCreator;
	}

	/**
	 * 设置物品制作者。
	 * Sets the item creator.
	 *
	 * @param itemCreator 要设置的物品制作者 / the itemCreator to set
	 */
	public void setItemCreator(String itemCreator) {
		this.itemCreator = itemCreator;
	}

	public String getItemName() {
		return itemTemplate.getName();
	}

	public String getSkillGroup() {
		return itemTemplate.getSkillGroup();
	}

	public int getOptionalSocket() {
		return optionalSocket;
	}

	public void setOptionalSocket(int optionalSocket) {
		this.optionalSocket = optionalSocket;
	}

	public boolean hasOptionalSocket() {
		return optionalSocket != 0;
	}

	public int getOptionalFusionSocket() {
		return optionalFusionSocket;
	}

	public boolean hasOptionalFusionSocket() {
		return optionalFusionSocket != 0;
	}

	public void setOptionalFusionSocket(int optionalFusionSocket) {
		this.optionalFusionSocket = optionalFusionSocket;
	}

	public int getEnchantBonus() {
		return enchant_bonus;
	}

	public void setEnchantBonus(int enchantBonus) {
		this.enchant_bonus = enchantBonus;
	}

	public boolean hasEnchantBonus() {
		return enchant_bonus != 0;
	}

	/**
	 * 返回物品模板。
	 * Returns the item template.
	 *
	 * @return 物品模板 / the itemTemplate
	 */
	public ItemTemplate getItemTemplate() {
		return itemTemplate;
	}

	/**
	 * 返回物品外观模板。
	 * Returns the item appearance template.
	 *
	 * @return 外观模板 / the itemAppearanceTemplate
	 */
	public ItemTemplate getItemSkinTemplate() {
		if (this.itemSkinTemplate == null) {
			return this.itemTemplate;
		}
		return this.itemSkinTemplate;
	}

	public void setItemSkinTemplate(ItemTemplate newTemplate) {
		this.itemSkinTemplate = newTemplate;
		setPersistentState(PersistentState.UPDATE_REQUIRED);
	}

	public boolean isSkinnedItem() {
		return getItemSkinTemplate() != this.itemTemplate;
	}

	public boolean isCanIdian() {
		return itemTemplate.isCanIdian();
	}

	/**
	 * 返回物品颜色。
	 * Returns the item color.
	 *
	 * @return 物品颜色 / the itemColor
	 */
	public int getItemColor() {
		DyeAction dyeAction = getDyeAction();
		return dyeAction != null ? dyeAction.getColor() : itemColor;
	}

	private DyeAction getDyeAction() {
		if (itemColor < 0) {
			return null;
		}
		ItemTemplate dyeTemplate = DataManager.ITEM_DATA.getItemTemplate(itemColor);
		if (dyeTemplate == null) {
			return null;
		}
		ItemActions actions = dyeTemplate.getActions();
		if (actions == null) {
			return null;
		}
		return actions.getDyeAction();
	}

	/**
	 * 设置物品颜色。
	 * Sets the item color.
	 *
	 * @param itemColor 要设置的物品颜色 / the itemColor to set
	 */
	public void setItemColor(int itemColor) {
		this.itemColor = itemColor;
		setPersistentState(PersistentState.UPDATE_REQUIRED);
	}

	public int getColorTimeLeft() {
		if (colorExpireTime == 0) {
			return 0;
		}
		return (int) (colorExpireTime - System.currentTimeMillis() / 1000);
	}

	public int getColorExpireTime() {
		return colorExpireTime;
	}

	public void setColorExpireTime(int dyeRemainsUntil) {
		this.colorExpireTime = dyeRemainsUntil;
		setPersistentState(PersistentState.UPDATE_REQUIRED);
	}

	/**
	 * 返回堆叠中的物品数量（不应超过模板最大堆叠数）。
	 * Returns the item count in this stack, should not exceed the template max stack count.
	 *
	 * @return 堆叠数量 / the itemCount
	 */
	public long getItemCount() {
		return itemCount;
	}

	/** 返回 free count / Returns the free count */
	public long getFreeCount() {
		return itemTemplate.getMaxStackCount() - itemCount;
	}

	/**
	 * 设置物品数量。
	 * Sets the item count.
	 *
	 * @param itemCount 要设置的物品数量 / the itemCount to set
	 */
	public void setItemCount(long itemCount) {
		this.itemCount = itemCount;
		setPersistentState(PersistentState.UPDATE_REQUIRED);
	}

	/**
	 * 此方法仅应由 Storage 类调用。 / This method should be called ONLY from Storage class In all other ways it is not guaranteed to be udpated in a regular update service It is allowed to use this method for newly created items which are not yet in any storage.
	 */
	public long increaseItemCount(long count) {
		if (count <= 0) {
			return 0;
		}
		long cap = itemTemplate.getMaxStackCount();
		long addCount = this.itemCount + count > cap ? cap - this.itemCount : count;
		if (addCount != 0) {
			this.itemCount += addCount;
			setPersistentState(PersistentState.UPDATE_REQUIRED);
		}
		return count - addCount;
	}

	/**
	 * 此方法仅应由 Storage 类调用。 / This method should be called ONLY from Storage class In all other ways it is not guaranteed to be udpated in a regular update service It is allowed to use this method for newly created items which are not yet in any storage.
	 */
	public long decreaseItemCount(long count) {
		if (count <= 0) {
			return 0;
		}
		long removeCount = count >= itemCount ? itemCount : count;
		this.itemCount -= removeCount;
		if (itemCount == 0 && !this.itemTemplate.isKinah()) {
			setPersistentState(PersistentState.DELETED);
		} else {
			setPersistentState(PersistentState.UPDATE_REQUIRED);
		}
		return count - removeCount;
	}

	/**
	 * 返回是否已装备。
	 * Returns whether the item is equipped.
	 *
	 * @return 是否已装备 / whether equipped
	 */
	public boolean isEquipped() {
		return isEquipped;
	}

	/**
	 * 设置是否已装备。
	 * Sets whether the item is equipped.
	 *
	 * @param isEquipped 要设置的装备状态 / the isEquipped to set
	 */
	public void setEquipped(boolean isEquipped) {
		this.isEquipped = isEquipped;
		setPersistentState(PersistentState.UPDATE_REQUIRED);
	}

	/**
	 * 返回装备槽位，可为 ItemSlot 枚举（已装备）或背包位置。
	 * Returns the equipment slot, either an ItemSlot enum (if equipped) or a cube position.
	 *
	 * @return 装备槽位 / the equipmentSlot
	 */
	public long getEquipmentSlot() {
		return equipmentSlot;
	}

	/** 返回 equipment slot integer / Returns the equipment slot integer */
	public int getEquipmentSlotInteger() {
		return (int) equipmentSlot;
	}

	/**
	 * 设置装备槽位。
	 * Sets the equipment slot.
	 *
	 * @param equipmentSlot 要设置的装备槽位 / the equipmentSlot to set
	 */
	public void setEquipmentSlot(long equipmentSlot) {
		this.equipmentSlot = equipmentSlot;
		setPersistentState(PersistentState.UPDATE_REQUIRED);
	}

	/**
	 * 用于惰性初始化空魔石列表。
	 * This method should be used to lazy initialize empty manastone list @return the itemStones.
	 */
	public Set<ManaStone> getItemStones() {
		if (manaStones == null) {
			this.manaStones = itemStonesCollection();
		}
		return manaStones;
	}

	/**
	 * 用于惰性初始化空魔石列表。
	 * This method should be used to lazy initialize empty manastone list @return the itemStones.
	 */
	public Set<ManaStone> getFusionStones() {
		if (fusionStones == null) {
			this.fusionStones = itemStonesCollection();
		}
		return fusionStones;
	}

	/** 返回融合魔石数量 / Returns the fusion stones size */
	public int getFusionStonesSize() {
		if (fusionStones == null) {
			return 0;
		}
		return fusionStones.size();
	}

	/** 返回物品魔石数量 / Returns the item stones size */
	public int getItemStonesSize() {
		if (manaStones == null) {
			return 0;
		}
		return manaStones.size();
	}

	private Set<ManaStone> itemStonesCollection() {
		return new TreeSet<ManaStone>(new Comparator<ManaStone>() {
			/** 比较 / compare. */
			@Override
			public int compare(ManaStone o1, ManaStone o2) {
				if (o1.getSlot() == o2.getSlot()) {
					return 0;
				}
				return o1.getSlot() > o2.getSlot() ? 1 : -1;
			}
		});
	}

	/**
	 * 检查魔石（不初始化列表）。
	 * Checks the manastones without initialization.
	 *
	 * @return 魔石数量 / manastone count
	 */
	public boolean hasManaStones() {
		return manaStones != null && manaStones.size() > 0;
	}

	/**
	 * 检查融合魔石（不初始化列表）。
	 * Checks the fusion stones without initialization.
	 *
	 * @return 融合魔石数量 / fusion stone count
	 */
	public boolean hasFusionStones() {
		return fusionStones != null && fusionStones.size() > 0;
	}

	/**
	 * 是否拥有伊迪安石。
	 * Whether the item has an Idian stone.
	 *
	 * @return 是否拥有伊迪安石 / whether Idian stone
	 */
	public boolean hasIdianStone() {
		return idianStone != null;
	}

	/**
	 * 是否拥有神石。
	 * Whether the item has a godstone.
	 *
	 * @return 是否拥有神石 / whether godstone
	 */
	public boolean hasGodStone() {
		return godStone != null;
	}

	/**
	 * 返回神石。
	 * Returns the godstone.
	 *
	 * @return 神石 / the godStone
	 */
	public GodStone getGodStone() {
		return godStone;
	}

	/**
	 * 检查物品是否安装了指定神石。
	 * Checks whether the given godstone is installed.
	 *
	 * @param itemId 物品 ID / item id
	 * @return 是否已安装 / whether installed
	 */
	public GodStone addGodStone(int itemId) {
		return addGodStone(itemId, 0);
	}

	public GodStone addGodStone(int itemId, int activatedCount) {
		PersistentState state = godStone != null ? PersistentState.UPDATE_REQUIRED : PersistentState.NEW;
		godStone = new GodStone(getObjectId(), itemId, activatedCount, state);
		return godStone;
	}

	/**
	 * 设置神石。
	 * Sets the godstone.
	 *
	 * @param godStone 要设置的神石 / the godStone to set
	 */
	public void setGodStone(GodStone godStone) {
		this.godStone = godStone;
	}

	/**
	 * 返回强化等级。
	 * Returns the enchant level.
	 *
	 * @return 强化等级 / the enchantLevel
	 */
	public int getEnchantLevel() {
		return enchantLevel;
	}

	/**
	 * 设置强化等级。
	 * Sets the enchant level.
	 *
	 * @param enchantLevel 要设置的强化等级 / the enchantLevel to set
	 */
	public void setEnchantLevel(int enchantLevel) {
		this.enchantLevel = enchantLevel;
		setPersistentState(PersistentState.UPDATE_REQUIRED);
	}

	/**
	 * 返回持久化状态。
	 * Returns the persistent state.
	 *
	 * @return 持久化状态 / the persistentState
	 */
	public PersistentState getPersistentState() {
		return persistentState;
	}

	/**
	 * 可能状态变更：NEW→UPDATED/UPDATE_REQUIRED；UPDATE_REQUIRED→DELETED/UPDATED；UPDATED→DELETED/UPDATE_REQUIRED。 / Possible changes: NEW -> UPDATED NEW -> UPDATE_REQURIED UPDATE_REQUIRED -> DELETED UPDATE_REQUIRED -> UPDATED UPDATED -> DELETED UPDATED -> UPDATE_REQUIRED.
	 */
	public void setPersistentState(PersistentState persistentState) {
		switch (persistentState) {
		case DELETED:
			if (this.persistentState == PersistentState.NEW) {
				this.persistentState = PersistentState.NOACTION;
			} else {
				this.persistentState = PersistentState.DELETED;
			}
			break;
		case UPDATE_REQUIRED:
			if (this.persistentState == PersistentState.NEW) {
				break;
			}
		default:
			this.persistentState = persistentState;
		}
	}

	/** 设置物品位置。 / Sets the item location. */
	public void setItemLocation(int storageType) {
		this.itemLocation = storageType;
		setPersistentState(PersistentState.UPDATE_REQUIRED);
	}

	/** 获取物品位置。 / Returns the item location. */
	public int getItemLocation() {
		return itemLocation;
	}

	/** 获取物品掩码。 / Returns the item mask. */
	public int getItemMask() {
		return itemTemplate.getMask();
	}

	/**
	 * 是否已灵魂绑定。
	 * Whether the item is soul bound.
	 *
	 * @return 是否已绑定 / whether soul bound
	 */
	public boolean isSoulBound() {
		return isSoulBound;
	}

	private boolean isSoulBound(Player player) {
		if (player.havePermission(MembershipConfig.DISABLE_SOULBIND)) {
			return false;
		} else {
			return isSoulBound;
		}
	}

	/** 设置灵魂绑定 / Sets the soul bound */
	public void setSoulBound(boolean isSoulBound) {
		this.isSoulBound = isSoulBound;
		setPersistentState(PersistentState.UPDATE_REQUIRED);
	}

	/** 获取装备类型。 / Returns the equipment type. */
	public EquipType getEquipmentType() {
		if (itemTemplate.isStigma()) {
			return EquipType.STIGMA;
		}
		return itemTemplate.getEquipmentType();
	}

	/** 返回字符串表示。 / Returns string representation. */
	@Override
	public String toString() {
		return "Item [itemId=" + itemTemplate.getTemplateId() + " equipmentSlot=" + equipmentSlot + ", godStone="
				+ godStone + ", isEquipped=" + isEquipped + ", itemColor=" + itemColor + ", itemCount=" + itemCount
				+ ", itemLocation=" + itemLocation + ", itemTemplate=" + itemTemplate + ", manaStones=" + manaStones
				+ ", persistentState=" + persistentState + "]";
	}

	/** 返回物品 ID / Returns the item id */
	public int getItemId() {
		return itemTemplate.getTemplateId();
	}

	/** 返回名称 ID / Returns the name id */
	public int getNameId() {
		return itemTemplate.getNameId();
	}

	/** 是否为合成物品 / Whether fusioned item */
	public boolean hasFusionedItem() {
		return fusionedItemTemplate != null;
	}

	/** 返回融合物品模板 / Returns the fusioned item template */
	public ItemTemplate getFusionedItemTemplate() {
		return this.fusionedItemTemplate;
	}

	/** 返回融合物品 ID / Returns the fusioned item id */
	public int getFusionedItemId() {
		return fusionedItemTemplate != null ? fusionedItemTemplate.getTemplateId() : 0;
	}

	/** 设置融合物品 / Sets the fusioned item */
	public void setFusionedItem(ItemTemplate itemTemplate) {
		fusionedItemTemplate = itemTemplate;
		updateChargeInfo(0);
	}

	/** 返回插槽数 / Returns the sockets */
	public int getSockets(boolean isFusionItem) {
		int numSockets;
		if (itemTemplate.isWeapon() || itemTemplate.isArmor()) {
			if (isFusionItem) {
				ItemTemplate fusedTemp = getFusionedItemTemplate();
				if (fusedTemp == null) {
					log.error(I18n.get("log.7ff276137738", getObjectId(), getItemId()));
					return 0;
				}
				numSockets = fusedTemp.getManastoneSlots();
				numSockets += hasOptionalFusionSocket() ? getOptionalFusionSocket() : 0;
			} else {
				numSockets = getItemTemplate().getManastoneSlots();
				numSockets += hasOptionalSocket() ? getOptionalSocket() : 0;
			}
			if (numSockets < 6) {
				return numSockets;
			}
			return 6;
		}
		return 0;
	}

	/**
	 * 返回掩码。
	 * Returns the mask.
	 *
	 * @return 掩码 / the mask
	 */
	public int getItemMask(Player player) {
		int finalMask = checkConfig(player, itemTemplate.getMask());
		return finalMask;
	}

	/**
	 * @param player
	 * @return
	 */
	private int checkConfig(Player player, int mask) {
		int newMask = mask;
		if (player.havePermission(MembershipConfig.STORE_WH_ALL)) {
			newMask = newMask | ItemMask.STORABLE_IN_WH;
		}
		if (player.havePermission(MembershipConfig.STORE_AWH_ALL)) {
			newMask = newMask | ItemMask.STORABLE_IN_AWH;
		}
		if (player.havePermission(MembershipConfig.STORE_LWH_ALL)) {
			newMask = newMask | ItemMask.STORABLE_IN_LWH;
		}
		if (player.havePermission(MembershipConfig.TRADE_ALL)) {
			newMask = newMask | ItemMask.TRADEABLE;
		}
		if (player.havePermission(MembershipConfig.REMODEL_ALL)) {
			newMask = newMask | ItemMask.REMODELABLE;
		}
		return newMask;
	}

	/**
	 * 按对象 ID 与物品 ID 比较两个物品是否相等。 / Compares two items on their object and item ids.
	 */
	public boolean isSameItem(Item i) {
		return this.getObjectId().equals(i.getObjectId()) && this.getItemId() == i.getItemId();
	}

	/**
	 * 是否可存入个人仓库。
	 * Whether the item is storable in the warehouse.
	 *
	 * @param player 玩家 / player
	 * @return 是否可存入 / whether storable
	 */
	public boolean isStorableinWarehouse(Player player) {
		return (getItemMask(player) & ItemMask.STORABLE_IN_WH) == ItemMask.STORABLE_IN_WH && !isSoulBound(player);
	}

	/**
	 * 是否可存入账号仓库。
	 * Whether the item is storable in the account warehouse.
	 *
	 * @param player 玩家 / player
	 * @return 是否可存入 / whether storable
	 */
	public boolean isStorableinAccWarehouse(Player player) {
		return (getItemMask(player) & ItemMask.STORABLE_IN_AWH) == ItemMask.STORABLE_IN_AWH && !isSoulBound(player);
	}

	/**
	 * 是否可存入军团仓库。
	 * Whether the item is storable in the legion warehouse.
	 *
	 * @param player 玩家 / player
	 * @return 是否可存入 / whether storable
	 */
	public boolean isStorableinLegWarehouse(Player player) {
		return (getItemMask(player) & ItemMask.STORABLE_IN_LWH) == ItemMask.STORABLE_IN_LWH && !isSoulBound(player);
	}

	/**
	 * 是否可交易。
	 * Whether the item is tradeable.
	 *
	 * @param player 玩家 / player
	 * @return 是否可交易 / whether tradeable
	 */
	public boolean isTradeable(Player player) {
		return (getItemMask(player) & ItemMask.TRADEABLE) == ItemMask.TRADEABLE && !isSoulBound(player);
	}

	/**
	 * 是否可改造外观。
	 * Whether the item is remodelable.
	 *
	 * @param player 玩家 / player
	 * @return 是否可改造 / whether remodelable
	 */
	public boolean isRemodelable(Player player) {
		return (getItemMask(player) & ItemMask.REMODELABLE) == ItemMask.REMODELABLE;
	}

	/**
	 * 是否可出售。
	 * Whether the item is sellable.
	 *
	 * @return 是否可出售 / whether sellable
	 */
	public boolean isSellable() {
		return (getItemMask() & ItemMask.SELLABLE) == ItemMask.SELLABLE;
	}

	/**
	 * 是否可提取欧比斯点数。
	 * Whether AP can be extracted from the item.
	 *
	 * @return 是否可提取 AP / whether AP extract
	 */
	public boolean canApExtract() {
		return (getItemMask() & ItemMask.CAN_AP_EXTRACT) == ItemMask.CAN_AP_EXTRACT;
	}

	/** 是否可以伊迪安。 / Whether idian. */
	public boolean canIdian() {
		return (getItemMask() & ItemMask.CAN_IDIAN) == ItemMask.CAN_IDIAN;
	}

	/**
	 * 是否可以镶嵌神石。
	 * Whether a godstone can be socketed.
	 *
	 * @return 是否可镶嵌 / whether socket godstone
	 */
	public boolean canSocketGodstone() {
		return (getItemMask() & ItemMask.CAN_PROC_ENCHANT) == ItemMask.CAN_PROC_ENCHANT;
	}

	/**
	 * 是否已注能。
	 * Whether the item is amplified.
	 *
	 * @return 是否已注能 / whether amplified
	 */
	public boolean canAmplification() {
		return (getItemMask() & ItemMask.CAN_AMPLIFICATION) == ItemMask.CAN_AMPLIFICATION;
	}

	/** 是否为高阶守护者物品 / Whether arch daeva item */
	public boolean isArchDaevaItem() {
		return (getItemMask() & ItemMask.ITEM_ARCHDAEVA) == ItemMask.ITEM_ARCHDAEVA;
	}

	/**
	 * 返回过期时间。
	 * Returns the expire time.
	 *
	 * @return 过期时间 / the expire time
	 */
	@Override
	public int getExpireTime() {
		return expireTime;
	}

	/** 设置过期时间。 / Sets the expire time. */
	public void setExpireTime(int expireTime) {
		this.expireTime = expireTime;
	}

	/** 返回剩余过期时间 / Returns the expire time remaining */
	public int getExpireTimeRemaining() {
		if (expireTime == 0) {
			return 0;
		}
		return expireTime - (int) (System.currentTimeMillis() / 1000);
	}

	/**
	 * 返回临时交换时间。
	 * Returns the temporary exchange time.
	 *
	 * @return 临时交换时间 / the temporary exchange time
	 */
	public int getTemporaryExchangeTime() {
		return temporaryExchangeTime;
	}

	/**
	 * 获取临时交换剩余时间。
	 * Returns the temporary exchange time remaining.
	 */
	public int getTemporaryExchangeTimeRemaining() {
		if (temporaryExchangeTime == 0) {
			return 0;
		}
		return temporaryExchangeTime - (int) (System.currentTimeMillis() / 1000);
	}

	/**
	 * 设置临时交换时间。
	 * Sets the temporary exchange time.
	 *
	 * @param temporaryExchangeTime 要设置的临时交换时间 / the temporaryExchangeTime to set
	 */
	public void setTemporaryExchangeTime(int temporaryExchangeTime) {
		this.temporaryExchangeTime = temporaryExchangeTime;
	}

	/** 到期结束 / Expire end. */
	@Override
	public void expireEnd(Player player) {
		if (player == null) {
			return;
		}
		if (isEquipped()) {
			player.getEquipment().unEquipItem(getObjectId(), getEquipmentSlot());
		}

		for (StorageType i : StorageType.values()) {
			if (i == StorageType.LEGION_WAREHOUSE) {
				continue;
			}
			IStorage storage = player.getStorage(i.getId());

			if (storage != null && storage.getItemByObjId(getObjectId()) != null) {
				storage.decreaseByObjectId(getObjectId(), getItemCount());
				switch (i) {
				case CUBE:
					PacketSendUtility.sendPacket(player,
							new SM_SYSTEM_MESSAGE(1400034, new DescriptionId(getNameId())));
					break;
				case ACCOUNT_WAREHOUSE:
				case REGULAR_WAREHOUSE:
					PacketSendUtility.sendPacket(player,
							new SM_SYSTEM_MESSAGE(1400406, new DescriptionId(getNameId())));
					break;
				default:
					break;
				}
			}
		}
	}

	/** 过期消息。 / Expire Message. */
	@Override
	public void expireMessage(Player player, int time) {
		if (player != null) {
			PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1400481, new DescriptionId(getNameId()), time));
		}
	}

	/** 设置回购价格 / Sets the repurchase price */
	public void setRepurchasePrice(long price) {
		repurchasePrice = price;
	}

	/** 返回回购价格 / Returns the repurchase price */
	public long getRepurchasePrice() {
		return repurchasePrice;
	}

	/** 返回激活次数 / Returns the activation count */
	public int getActivationCount() {
		return activationCount;
	}

	/** 设置激活次数 / Sets the activation count */
	public void setActivationCount(int activationCount) {
		this.activationCount = activationCount;
	}

	/** 返回调谐信息 / Returns the conditioning info */
	public ChargeInfo getConditioningInfo() {
		return conditioningInfo;
	}

	/** 返回充能点 / Returns the charge points. */
	public int getChargePoints() {
		return conditioningInfo != null ? conditioningInfo.getChargePoints() : 0;
	}

	/**
	 * 根据主物品与融合物品计算充能等级。
	 * Calculates the charge level based on the main and fusioned items.
	 */
	public int getChargeLevel() {
		if (getChargePoints() == 0) {
			return 0;
		}
		return getChargePoints() > ChargeInfo.LEVEL1 ? 2 : 1;
	}

	/** 返回最大充能等级 / Returns the max charge level. */
	public int getChargeLevelMax() {
		int thisChargeLevel = 0;
		if (getImprovement() != null) {
			thisChargeLevel = getImprovement().getLevel();
		}
		int fusionedChargeLevel = 0;
		if (hasFusionedItem() && getFusionedItemTemplate().getImprovement() != null) {
			fusionedChargeLevel = getFusionedItemTemplate().getImprovement().getLevel();
		}
		return Math.max(thisChargeLevel, fusionedChargeLevel);
	}

	/** 是否立即过期 / Whether expire now */
	public boolean canExpireNow() {
		return true;
	}

	/** 返回强化信息 / Returns the improvement */
	public Improvement getImprovement() {
		if (getItemTemplate().getImprovement() != null) {
			return getItemTemplate().getImprovement();
		}
		if (hasFusionedItem() && getFusionedItemTemplate().getImprovement() != null) {
			return getFusionedItemTemplate().getImprovement();
		}
		return null;
	}

	/** 返回伊迪安石 / Returns the idian stone */
	public IdianStone getIdianStone() {
		return idianStone;
	}

	/** 设置伊迪安石 / Sets the idian stone */
	public void setIdianStone(IdianStone idianStone) {
		this.idianStone = idianStone;
	}

	/** 返回加成编号 / Returns the bonus number */
	public int getBonusNumber() {
		return bonusNumber;
	}

	/** 设置加成编号 / Sets the bonus number */
	public void setBonusNumber(int number) {
		this.bonusNumber = number;
	}

	/** 返回随机属性 / Returns the random stats */
	public RandomStats getRandomStats() {
		return randomStats;
	}

	/** 设置随机属性 / Sets the random stats */
	public void setRandomStats(RandomStats randomStats) {
		this.randomStats = randomStats;
	}

	/** 是否为鉴定物品 / Whether identify item */
	public boolean isIdentifyItem() {
		return itemTemplate.isCloth();
	}

	/** 返回 current modifiers / Returns the current modifiers */
	public List<StatFunction> getCurrentModifiers() {
		if (currentModifiers == null) {
			currentModifiers = new ArrayList<StatFunction>();
		}
		return currentModifiers;
	}

	/** 设置 current modifiers / Sets the current modifiers */
	public void setCurrentModifiers(List<StatFunction> currentModifiers) {
		getCurrentModifiers().clear();
		getCurrentModifiers().addAll(currentModifiers);
	}

	/** 设置随机属性数量 / Sets the random count */
	public void setRandomCount(int rndCount) {
		this.rndCount = rndCount;
	}

	/** 返回随机数量 / Returns the random count */
	public int getRandomCount() {
		return rndCount;
	}

	/** 设置包装计数 / Sets the wrappable count */
	public void setWrappableCount(int wrappableCount) {
		this.wrappableCount = wrappableCount;
	}

	/** 返回包装计数 / Returns the wrappable count */
	public int getWrappableCount() {
		return wrappableCount;
	}

	/** 设置授权等级 / Sets the authorize */
	public void setAuthorize(int paramInt) {
		authorize = paramInt;
		setPersistentState(PersistentState.UPDATE_REQUIRED);
	}

	/** 返回授权等级 / Returns the authorize */
	public int getAuthorize() {
		return authorize;
	}

	/**
	 * 返回是否已打包。
	 * Returns whether the item is packed.
	 *
	 * @return 是否已打包 / whether packed
	 */
	public boolean isPacked() {
		return isPacked;
	}

	/**
	 * 设置是否已打包。
	 * Sets whether the item is packed.
	 *
	 * @param isPacked 要设置的打包状态 / the isPacked to set
	 */
	public void setPacked(boolean isPacked) {
		this.isPacked = isPacked;
		setPersistentState(PersistentState.UPDATE_REQUIRED);
	}

	/** 设置注能 / Sets the amplification */
	public void setAmplification(boolean amplification) {
		this.amplification = amplification;
	}

	/**
	 * 是否已注能。
	 * Whether the item is amplified.
	 *
	 * @return 是否已注能 / whether amplified
	 */
	public boolean isAmplified() {
		return amplification;
	}

	/** 设置注能技能 / Sets the amplification skill */
	public void setAmplificationSkill(int skill) {
		this.amplificationSkill = skill;
	}

	/** 返回注能技能 / Returns the amplification skill */
	public int getAmplificationSkill() {
		return amplificationSkill;
	}

	/** 设置物品外观技能。 / Sets the item skin skill. */
	public void setItemSkinSkill(int skill) {
		this.SkinSkill = skill;
	}

	/** 获取物品外观技能。 / Returns the item skin skill. */
	public int getItemSkinSkill() {
		return SkinSkill;
	}

	/** 设置 Luna 换肤 / Sets the luna reskin */
	public void setLunaReskin(boolean luna_reskin) {
		this.luna_reskin = luna_reskin;
	}

	/**
	 * 是否为 Luna 换肤。
	 * Whether the item is a Luna reskin.
	 *
	 * @return 是否为 Luna 换肤 / whether luna reskin
	 */
	public boolean isLunaReskin() {
		return luna_reskin;
	}

	/** 设置减免等级 / Sets the reduction level */
	public void setReductionLevel(int paramInt) {
		ReductionLevel = paramInt;
		setPersistentState(PersistentState.UPDATE_REQUIRED);
	}

	/** 返回减免等级 / Returns the reduction level */
	public int getReductionLevel() {
		return ReductionLevel;
	}

	/**
	 * 是否已封印。
	 * Whether the item is sealed.
	 *
	 * @return 是否已封印 / whether sealed
	  */
	public boolean isSeal() {
		if (this.unSeal == 1) {
			return true;
		} else {
			return false;
		}
	}

	/** 返回解除封印标记 / Returns the unseal flag */
	public int getUnSeal() {
		return unSeal;
	}

	/** 设置解除封印标记 / Sets the unseal flag */
	public void setUnSeal(int unSeal) {
		this.unSeal = unSeal;
		setPersistentState(PersistentState.UPDATE_REQUIRED);
	}

	/**
	 * 是否已强化。
	 * Whether the item is enhanced.
	 *
	 * @return 是否已强化 / whether enhanced
	 */
	public boolean isEnhance() {
		return canEnhance;
	}

	/** 设置是否强化 / Sets whether enhanced */
	public void setIsEnhance(boolean canEnhance) {
		this.canEnhance = canEnhance;
	}

	/** 返回强化技能 ID / Returns the enhance skill id */
	public int getEnhanceSkillId() {
		return enhanceSkillId;
	}

	/** 设置强化技能 ID / Sets the enhance skill id */
	public void setEnhanceSkillId(int skillId) {
		this.enhanceSkillId = skillId;
	}

	/** 返回强化等级 / Returns the enhance enchant level */
	public int getEnhanceEnchantLevel() {
		return enhanceEnchantLevel;
	}

	/** 设置强化等级 / Sets the enhance enchant level */
	public void setEnhanceEnchantLevel(int level) {
		this.enhanceEnchantLevel = level;
	}
}
