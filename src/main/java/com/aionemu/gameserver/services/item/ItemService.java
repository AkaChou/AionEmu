package com.aionemu.gameserver.services.item;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.lifecycle.GameTaskManagerServices;

import com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.configs.main.LoggingConfig;
import com.aionemu.gameserver.dao.ItemStoneListDAO;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.dataholders.ItemSkillEnhanceData;
import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.gameobjects.AionObject;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.player.Equipment;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.items.ChargeInfo;
import com.aionemu.gameserver.model.items.ItemId;
import com.aionemu.gameserver.model.items.ManaStone;
import com.aionemu.gameserver.model.items.storage.Storage;
import com.aionemu.gameserver.model.templates.item.ArmorType;
import com.aionemu.gameserver.model.templates.item.Improvement;
import com.aionemu.gameserver.model.templates.item.ItemCustomSetTeamplate;
import com.aionemu.gameserver.model.templates.item.ItemSkillEnhance;
import com.aionemu.gameserver.model.templates.item.ItemTemplate;
import com.aionemu.gameserver.model.templates.quest.QuestItems;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.EnchantService;
import com.aionemu.gameserver.services.item.ItemPacketService.ItemAddType;
import com.aionemu.gameserver.services.item.ItemPacketService.ItemUpdateType;
import com.aionemu.gameserver.taskmanager.tasks.ExpireTimerTask;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.RndArray;
import com.aionemu.gameserver.utils.idfactory.IDFactory;
import com.aionemu.gameserver.world.World;
import com.google.common.base.Preconditions;
import com.google.common.collect.Collections2;

/**
 * 核心物品服务：发放/加载魔石、任务物品、升级拷贝与 ID 回收。
 * Core item service — grant/load manastones, quest items, upgrade copy, and id release.
 *
 * @author KID
 */
@Slf4j(topic = "ITEM_LOG")
public class ItemService {

	/** 默认入包更新谓词（采集类型）。 / Default inventory-add update predicate (collect type). */
	public static final ItemUpdatePredicate DEFAULT_UPDATE_PREDICATE = new ItemUpdatePredicate(ItemAddType.ITEM_COLLECT,
			ItemUpdateType.INC_ITEM_COLLECT);

	/**
	 * 从数据库批量加载物品镶嵌的魔石/神石。
	 * Loads manastones/godstones for a collection of items from the database.
	 *
	 * item collection
	 */
	public static void loadItemStones(Collection<Item> itemList) {
		if (itemList != null && itemList.size() > 0) {
			DAOManager.getDAO(ItemStoneListDAO.class).load(itemList);
		}
	}

	/**
	 * 确保玩家物品集合均具备技能强化配置。
	 * Ensures skill-enhance data is applied to all items for the player.
	 *
	 * @param player 玩家 / player
	 * @param items 物品集合 / items
	 * @return 是否有任何物品被修改 / true if any item was changed
	 */
	public static boolean ensureSkillEnhance(Player player, Collection<Item> items) {
		if (items == null || items.isEmpty()) {
			return false;
		}
		boolean changed = false;
		for (Item item : items) {
			changed |= ensureSkillEnhance(item, DataManager.ITEM_SKILL_ENHANCE_DATA, player.getPlayerClass());
		}
		return changed;
	}

	/**
	 * 向玩家发放指定数量物品（默认更新谓词）。
	 * Grants items to the player with the default update predicate.
	 *
	 * 玩家 / player
	 * item template id
	 * count
	 *
	 * @return 未能放入的剩余数量 / remaining count that could not be added
	 */
	public static long addItem(Player player, int itemId, long count) {
		return addItem(player, itemId, count, DEFAULT_UPDATE_PREDICATE);
	}

	/**
	 * 判断单种物品能否完整放入玩家背包。
	 * Checks whether one item type can be added to the player's inventory in full.
	 */
	public static boolean canAddItem(Player player, int itemId, long count) {
		return canAddItems(player, Map.of(itemId, count));
	}

	/**
	 * 判断一组物品能否完整放入玩家背包。
	 * Checks whether all requested items can be added to the player's inventory in full.
	 */
	public static boolean canAddItems(Player player, Map<Integer, Long> itemCounts) {
		Map<Integer, ItemTemplate> templates = new HashMap<>();
		for (Integer itemId : itemCounts.keySet()) {
			templates.put(itemId, DataManager.ITEM_DATA.getItemTemplate(itemId));
		}
		return canAddItems(player.getInventory(), itemCounts, templates, 0, 0);
	}

	/**
	 * 判断一组物品能否完整放入指定背包。
	 * Checks whether all requested items can be added to the given storage in full.
	 */
	public static boolean canAddItems(Storage inventory, Map<Integer, Long> itemCounts,
			Map<Integer, ItemTemplate> templates) {
		return canAddItems(inventory, itemCounts, templates, 0, 0);
	}

	/**
	 * 判断交换物品后是否有足够空间；可计入即将释放的普通/特殊背包格。
	 * Checks capacity after an exchange, including regular/special slots that will be released.
	 */
	public static boolean canAddItems(Storage inventory, Map<Integer, Long> itemCounts,
			Map<Integer, ItemTemplate> templates, int releasedRegularSlots, int releasedSpecialSlots) {
		long regularSlots = Math.max(0, releasedRegularSlots) + (long) inventory.getFreeSlots();
		long specialSlots = Math.max(0, releasedSpecialSlots) + (long) inventory.getSpecialCubeFreeSlots();
		for (Map.Entry<Integer, Long> entry : itemCounts.entrySet()) {
			long remainingCount = entry.getValue() == null ? 0 : entry.getValue();
			if (remainingCount < 0) {
				return false;
			}
			if (remainingCount == 0) {
				continue;
			}
			ItemTemplate template = templates.get(entry.getKey());
			if (template == null) {
				return false;
			}
			if (template.isKinah()) {
				continue;
			}
			long maxStackCount = template.getMaxStackCount();
			if (maxStackCount < 1) {
				return false;
			}
			if (template.isStackable()) {
				for (Item item : inventory.getItemsByItemId(entry.getKey())) {
					remainingCount -= Math.min(remainingCount, Math.max(0, maxStackCount - item.getItemCount()));
					if (remainingCount == 0) {
						break;
					}
				}
			}
			long requiredSlots = remainingCount == 0 ? 0 : 1 + (remainingCount - 1) / maxStackCount;
			if (template.getExtraInventoryId() > 0) {
				specialSlots -= requiredSlots;
				if (specialSlots < 0) {
					return false;
				}
			} else {
				regularSlots -= requiredSlots;
				if (regularSlots < 0) {
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * 原子式校验并扣除一组背包物品，避免中途失败造成部分材料损失。
	 * Validates and consumes a group of inventory items atomically to avoid partial loss.
	 */
	public static boolean decreaseItems(Player player, Map<Integer, Long> itemCounts) {
		Storage inventory = player.getInventory();
		synchronized (inventory) {
			for (Map.Entry<Integer, Long> entry : itemCounts.entrySet()) {
				Long count = entry.getValue();
				if (count == null || count < 0 || inventory.getItemCountByItemId(entry.getKey()) < count) {
					return false;
				}
			}
			for (Map.Entry<Integer, Long> entry : itemCounts.entrySet()) {
				if (entry.getValue() > 0 && !inventory.decreaseByItemId(entry.getKey(), entry.getValue())) {
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * 向玩家发放指定数量物品，使用自定义更新谓词。
	 * Grants items to the player with a custom update predicate.
	 *
	 * 玩家 / player
	 * item template id
	 * count
	 * update predicate
	 *
	 * @return 未能放入的剩余数量 / remaining count that could not be added
	 */
	public static long addItem(Player player, int itemId, long count, ItemUpdatePredicate predicate) {
		return addItem(player, itemId, count, null, predicate, 0, false);
	}

	/**
	 * 按源物品全部属性拷贝发放新物品。
	 * Grants a new item by copying all values from the source item.
	 *
	 * 玩家 / player
	 * source item
	 *
	 * @return 未能放入的剩余数量 / remaining count that could not be added
	 */
	public static long addItem(Player player, Item sourceItem) {
		return addItem(player, sourceItem.getItemId(), sourceItem.getItemCount(), sourceItem, DEFAULT_UPDATE_PREDICATE,
				0, false);
	}

	/**
	 * 按源物品属性拷贝发放，并使用自定义更新谓词。
	 * Grants by copying source-item values with a custom update predicate.
	 *
	 * 玩家 / player
	 * source item
	 * update predicate
	 *
	 * @return 未能放入的剩余数量 / remaining count that could not be added
	 */
	public static long addItem(Player player, Item sourceItem, ItemUpdatePredicate predicate) {
		return addItem(player, sourceItem.getItemId(), sourceItem.getItemCount(), sourceItem, predicate, 0, false);
	}

	/**
	 * 发放指定数量物品，可选从源物品拷贝属性。
	 * Grants items, optionally copying attributes from a source item.
	 *
	 * 玩家 / player
	 * item template id
	 * count
	 * @param sourceItem 源物品（可为 null） / source item (nullable)
	 * @return 未能放入的剩余数量 / remaining count that could not be added
	 */
	public static long addItem(Player player, int itemId, long count, Item sourceItem) {
		return addItem(player, itemId, count, sourceItem, DEFAULT_UPDATE_PREDICATE, 0, false);
	}

	/**
	 * 发放物品并设置强化等级。
	 * Grants items and applies the given enchant level.
	 *
	 * 玩家 / player
	 * item template id
	 * count
	 * 强化等级 / enchant level
	 * update predicate
	 *
	 * @return 未能放入的剩余数量 / remaining count that could not be added
	 */
	public static long addItemAndEnchant(Player player, int itemId, long count, int enchantLevel,
			ItemUpdatePredicate predicate) {
		return addItem(player, itemId, count, null, predicate, enchantLevel, false);
	}

	/**
	 * 发放物品并设置强化等级（默认更新谓词）。
	 * Grants items with enchant level using the default update predicate.
	 *
	 * 玩家 / player
	 * item template id
	 * count
	 * 强化等级 / enchant level
	 * @return 未能放入的剩余数量 / remaining count that could not be added
	 */
	public static long addItemAndEnchant(Player player, int itemId, long count, int enchantLevel) {
		return addItem(player, itemId, count, null, DEFAULT_UPDATE_PREDICATE, enchantLevel, false);
	}

	/**
	 * 发放物品并设置强化等级，可选满充能。
	 * Grants items with enchant level, optionally full-charging them.
	 *
	 * 玩家 / player
	 * item template id
	 * count
	 * 强化等级 / enchant level
	 * @param augment 是否满充能 / whether to fully charge
	 * @return 未能放入的剩余数量 / remaining count that could not be added
	 */
	public static long addItemAndEnchant(Player player, int itemId, long count, int enchantLevel, boolean augment) {
		return addItem(player, itemId, count, null, DEFAULT_UPDATE_PREDICATE, enchantLevel, augment);
	}

	/**
	 * 发放物品核心入口：按可堆叠/不可堆叠分流，支持源物品拷贝、强化与充能。
	 * Core grant entry: routes stackable/non-stackable, supports source copy, enchant and charge.
	 *
	 * 玩家 / player
	 * item template id
	 * count
	 * @param sourceItem 源物品（可为 null） / source item (nullable)
	 * update predicate
	 * 强化等级 / enchant level
	 * @param augment 是否满充能 / whether to fully charge
	 * @return 未能放入的剩余数量 / remaining count that could not be added
	 */
	public static long addItem(Player player, int itemId, long count, Item sourceItem, ItemUpdatePredicate predicate,
			int enchantLevel, boolean augment) {
		ItemTemplate itemTemplate = DataManager.ITEM_DATA.getItemTemplate(itemId);
		if (count <= 0 || itemTemplate == null) {
			return 0;
		}
		Preconditions.checkNotNull(itemTemplate, "No item with id " + itemId);
		Preconditions.checkNotNull(predicate, "Predicate is not supplied");
		if (LoggingConfig.LOG_ITEM) {
			log.info(I18n.get("log.4896f907bf6c", (LoggingConfig.ENABLE_ADVANCED_LOGGING
									? "/Item Name - " + itemTemplate.getTemplateId() + "/" + count + "/"
											+ itemTemplate.getName()
									: " - " + itemTemplate.getTemplateId() + "/" + count), player.getName()));
		}
		Storage inventory = player.getInventory();
		if (itemTemplate.isKinah()) {
			inventory.increaseKinah(count);
			return 0;
		}
		if (itemTemplate.isStackable()) {
			count = addStackableItem(player, itemTemplate, count, predicate);
		} else {
			count = addNonStackableItem(player, itemTemplate, count, sourceItem, predicate, enchantLevel, augment);
		}
		if (inventory.isFull(itemTemplate.getExtraInventoryId()) && count > 0) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_DICE_INVEN_ERROR);
		}
		return count;
	}

	/**
	 * 向背包发放不可堆叠物品。
	 * Adds non-stackable items into inventory.
	 */
	private static long addNonStackableItem(Player player, ItemTemplate itemTemplate, long count, Item sourceItem,
			ItemUpdatePredicate predicate, int enchantlevel, boolean augment) {
		Storage inventory = player.getInventory();
		ItemCustomSetTeamplate itemCustomSet = DataManager.ITEM_CUSTOM_SET_DATA
				.getCustomTemplate(itemTemplate.getItemCustomSet());
		while (!inventory.isFull(itemTemplate.getExtraInventoryId()) && count > 0) {
			Item newItem = ItemFactory.newItem(itemTemplate.getTemplateId());
			if (newItem.getExpireTime() != 0) {
				GameTaskManagerServices.expireTimerTask().addTask(newItem, player);
			}
			if (sourceItem != null) {
				copyItemInfo(sourceItem, newItem);
			}
			if (itemTemplate.getMaxEnchantBonus() != 0) {
				newItem.setEnchantBonus(Rnd.get(0, itemTemplate.getMaxEnchantBonus()));
			}
			if (enchantlevel > 0) {
				enchant(player, enchantlevel, newItem);
			}
			if (augment) {
				chargeItem(player, newItem, 2);
			}
			if (itemTemplate.getItemCustomSet() != 0) {
				enchant(player, itemCustomSet.getCustomEnchantValue(), newItem);
			}
			if (itemTemplate.getSkillEnhance() != 0) {
				ensureSkillEnhance(newItem, DataManager.ITEM_SKILL_ENHANCE_DATA, player.getPlayerClass());
			}
			predicate.changeItem(newItem);
			inventory.add(newItem);
			count--;
		}
		return count;
	}

	/**
	 * 确保单件物品具备技能强化（按职业随机技能）。
	 * Ensures a single item has skill-enhance applied (random skill by class).
	 *
	 * item
	 * @param data 技能强化数据 / skill enhance data
	 * player class
	 *
	 * @return 是否修改了物品 / true if the item was changed
	 */
	static boolean ensureSkillEnhance(Item item, ItemSkillEnhanceData data, PlayerClass playerClass) {
		if (item == null || data == null || item.getItemTemplate() == null || item.getItemTemplate().getSkillEnhance() == 0) {
			return false;
		}
		ItemSkillEnhance itemSkillEnhance = data.getSkillEnhance(item.getItemTemplate().getSkillEnhance(), playerClass);
		if (itemSkillEnhance == null || itemSkillEnhance.getSkillId().isEmpty()) {
			return false;
		}
		if (item.isEnhance() && item.getEnhanceEnchantLevel() != 0
				&& itemSkillEnhance.getSkillId().contains(item.getEnhanceSkillId())) {
			return false;
		}
		item.setEnhanceSkillId(RndArray.get(itemSkillEnhance.getSkillId()));
		item.setEnhanceEnchantLevel(1);
		item.setIsEnhance(true);
		if (item.getPersistentState() != PersistentState.NEW) {
			item.setPersistentState(PersistentState.UPDATE_REQUIRED);
		}
		return true;
	}

	/**
	 * 将物品充能至指定等级（1 或 2）。
	 * Charges an item to the given level (1 or 2).
	 *
	 * 玩家 / player
	 * item
	 * @param level 充能等级 / charge level
	 */
	public static void chargeItem(Player player, Item item, int level) {
		Improvement improvement = item.getImprovement();
		if (improvement == null) {
			return;
		}
		int chargeWay = improvement.getChargeWay();
		int currentCharge = item.getChargePoints();
		switch (level) {
		case 1:
			item.getConditioningInfo().updateChargePoints(ChargeInfo.LEVEL1 - currentCharge);
			break;
		case 2:
			item.getConditioningInfo().updateChargePoints(ChargeInfo.LEVEL2 - currentCharge);
			break;
		}
		if (item.isEquipped()) {
			player.getGameStats().updateStatsVisually();
		}
		ItemPacketService.updateItemAfterInfoChange(player, item);
	}

	/**
	 * 从源物品拷贝魔石、神石、强化与外观等属性。
	 * Copies manastones, godstone, enchant and appearance from source to new item.
	 */
	private static void copyItemInfo(Item sourceItem, Item newItem) {
		newItem.setOptionalSocket(sourceItem.getOptionalSocket());
		newItem.setEnchantBonus(sourceItem.getEnchantBonus());
		if (sourceItem.hasManaStones()) {
			for (ManaStone manaStone : sourceItem.getItemStones()) {
				ItemSocketService.addManaStone(newItem, manaStone.getItemId());
			}
		}
		if (sourceItem.getGodStone() != null) {
			newItem.addGodStone(sourceItem.getGodStone().getItemId(), sourceItem.getGodStone().getActivatedCount());
		}
		if (sourceItem.getEnchantLevel() > 0) {
			newItem.setEnchantLevel(sourceItem.getEnchantLevel());
		}
		if (sourceItem.getEnchantBonus() > 0) {
			newItem.setEnchantBonus(sourceItem.getEnchantBonus());
		}
		if (sourceItem.isSoulBound()) {
			newItem.setSoulBound(true);
		}
		newItem.setBonusNumber(sourceItem.getBonusNumber());
		newItem.setRandomStats(sourceItem.getRandomStats());
		newItem.setIdianStone(sourceItem.getIdianStone());
		newItem.setRandomCount(sourceItem.getRandomCount());
		newItem.setItemColor(sourceItem.getItemColor());
		newItem.setItemSkinTemplate(sourceItem.getItemSkinTemplate());
		newItem.setIsEnhance(sourceItem.isEnhance());
		newItem.setEnhanceEnchantLevel(sourceItem.getEnhanceEnchantLevel());
		newItem.setEnhanceSkillId(sourceItem.getEnhanceSkillId());
	}

	/**
	 * 向背包（及装备中的碎片）发放可堆叠物品。
	 * Adds stackable items into inventory (and equipped shards when applicable).
	 */
	private static long addStackableItem(Player player, ItemTemplate itemTemplate, long count,
			ItemUpdatePredicate predicate) {
		Storage inventory = player.getInventory();
		Collection<Item> items = inventory.getItemsByItemId(itemTemplate.getTemplateId());
		for (Item item : items) {
			if (count == 0) {
				break;
			}
			count = inventory.increaseItemCount(item, count, predicate.getUpdateType(item, true));
		}
		if (itemTemplate.getArmorType() == ArmorType.SHARD) {
			Equipment equipement = player.getEquipment();
			items = equipement.getEquippedItemsByItemId(itemTemplate.getTemplateId());
			for (Item item : items) {
				if (count == 0) {
					break;
				}
				count = equipement.increaseEquippedItemCount(item, count);
			}
		}

		while (!inventory.isFull(itemTemplate.getExtraInventoryId()) && count > 0) {
			Item newItem = ItemFactory.newItem(itemTemplate.getTemplateId(), count);
			count -= newItem.getItemCount();
			inventory.add(newItem);
		}
		return count;
	}

	/**
	 * 发放任务物品列表（默认更新谓词）。
	 * Grants a list of quest items with the default update predicate.
	 *
	 * @param player 玩家 / player
	 * @param questItems 任务物品列表 / quest items
	 * @return 是否全部发放成功 / true if all items were granted
	 */
	public static boolean addQuestItems(Player player, List<QuestItems> questItems) {
		return addQuestItems(player, questItems, DEFAULT_UPDATE_PREDICATE);
	}

	/**
	 * 发放任务物品列表：先校验背包/特殊格空位，再逐项发放。
	 * Grants quest items after validating free inventory and special-cube slots.
	 *
	 * 玩家 / player
	 * @param questItems 任务物品列表 / quest items
	 * update predicate
	 *
	 * @return 是否全部发放成功 / true if all items were granted
	 */
	public static boolean addQuestItems(Player player, List<QuestItems> questItems, ItemUpdatePredicate predicate) {
		int slotReq = 0, specialSlot = 0;

		for (QuestItems qi : questItems) {
			if (qi.getItemId() != ItemId.KINAH.value() && qi.getCount() != 0) {
				ItemTemplate template = DataManager.ITEM_DATA.getItemTemplate(qi.getItemId());
				long stackCount = template.getMaxStackCount();
				long count = qi.getCount() / stackCount;
				if (qi.getCount() % stackCount != 0)
					count++;
				if (template.getExtraInventoryId() > 0) {
					specialSlot += count;
				} else {
					slotReq += count;
				}
			}
		}
		Storage inventory = player.getInventory();
		if (slotReq > 0 && inventory.getFreeSlots() < slotReq) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_DECOMPRESS_INVENTORY_IS_FULL);
			return false;
		}
		if (specialSlot > 0 && inventory.getSpecialCubeFreeSlots() < specialSlot) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_DECOMPRESS_INVENTORY_IS_FULL);
			return false;
		}
		for (QuestItems qi : questItems) {
			addItem(player, qi.getItemId(), qi.getCount(), predicate);
		}
		return true;
	}

	/**
	 * 回收单个物品的对象 ID。
	 * Releases a single item object id back to the id factory.
	 *
	 * item
	 */
	public static void releaseItemId(Item item) {
		GameWorldBootstrapServices.idFactory().releaseId(item.getObjectId());
	}

	/**
	 * 批量回收物品对象 ID。
	 * Releases object ids for a collection of items.
	 *
	 * @param items 物品集合 / items
	 */
	public static void releaseItemIds(Collection<Item> items) {
		Collection<Integer> idIterator = Collections2.transform(items, AionObject.OBJECT_TO_ID_TRANSFORMER);
		GameWorldBootstrapServices.idFactory().releaseIds(idIterator);
	}

	/**
	 * 按玩家对象 ID 向其背包投放 1 件物品。
	 * Drops one item into the inventory of the player identified by object id.
	 *
	 * player object id
	 * item template id
	 *
	 * @return 是否投放成功 / true if granted successfully
	 */
	public static boolean dropItemToInventory(int playerObjectId, int itemId) {
		return dropItemToInventory(com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findPlayer(playerObjectId), itemId);
	}

	/**
	 * 向在线玩家背包投放 1 件物品。
	 * Drops one item into an online player's inventory.
	 *
	 * 玩家 / player
	 * item template id
	 *
	 * @return 是否投放成功 / true if granted successfully
	 */
	public static boolean dropItemToInventory(Player player, int itemId) {
		return dropItemToInventory(player, itemId, 1);
	}

	/**
	 * 向在线玩家背包投放指定数量物品；背包满且无堆叠空间时失败。
	 * Drops items into an online player's inventory; fails when full with no free stack.
	 *
	 * 玩家 / player
	 * item template id
	 * count
	 *
	 * @return 是否投放成功 / true if granted successfully
	 */
	public static boolean dropItemToInventory(Player player, int itemId, long count) {
		if (player == null || !player.isOnline()) {
			return false;
		}
		Storage storage = player.getInventory();
		if (storage.getFreeSlots() < 1) {
			List<Item> items = storage.getItemsByItemId(itemId);
			boolean hasFreeStack = false;
			for (Item item : items) {
				if (item.getPersistentState() == PersistentState.DELETED
						|| item.getItemCount() < item.getItemTemplate().getMaxStackCount()) {
					hasFreeStack = true;
					break;
				}
			}
			if (!hasFreeStack) {
				return false;
			}
		}
		return addItem(player, itemId, count) == 0;
	}

	/**
	 * 创建装备升级结果物品，并生成可选孔和随机强化奖励。
	 * Creates an equipment-upgrade result with optional sockets and a random enchant bonus.
	 *
	 * @param itemId 升级结果模板 ID / upgrade-result template id
	 * @return 新建物品，模板不存在时为 null / new item, or null if template missing
	 */
	public static Item newUpgradeItem(int itemId) {
		Item temp = ItemFactory.newItem(itemId);
		if (temp == null) {
			return null;
		}
		ItemTemplate itemTemplate = temp.getItemTemplate();
		if (itemTemplate.isWeapon() || itemTemplate.isArmor()) {
			temp.setOptionalSocket(Rnd.get(0, itemTemplate.getOptionSlotBonus()));
		}
		if (itemTemplate.getMaxEnchantBonus() != 0) {
			temp.setEnchantBonus(Rnd.get(0, itemTemplate.getMaxEnchantBonus()));
		}

		return temp;
	}

	/**
	 * 检查随机物品模板 ID 是否存在。
	 * Checks whether a random item template id exists.
	 *
	 * @param randomItemId 随机物品模板 ID / random item template id
	 * @return 模板是否存在 / true if template exists
	 */
	public static boolean checkRandomTemplate(int randomItemId) {
		ItemTemplate template = DataManager.ITEM_DATA.getItemTemplate(randomItemId);
		return template != null;
	}

	/**
	 * 物品入包/数量变更时的更新谓词，封装添加类型与更新类型。
	 * Predicate for item-add/update packets, wrapping add type and update type.
	 */
	public static class ItemUpdatePredicate {
		/** 数量更新类型。 / Count update type. */
		private final ItemUpdateType itemUpdateType;
		/** 添加来源类型。 / Add-source type. */
		private final ItemAddType itemAddType;

		/**
		 * 使用指定添加/更新类型构造谓词。
		 * Constructs a predicate with the given add/update types.
		 *
		 * add type
		 * update type
		 */
		public ItemUpdatePredicate(ItemAddType itemAddType, ItemUpdateType itemUpdateType) {
			this.itemUpdateType = itemUpdateType;
			this.itemAddType = itemAddType;
		}

		/**
		 * 使用默认采集类型构造谓词。
		 * Constructs a predicate with default collect types.
		 */
		public ItemUpdatePredicate() {
			this(ItemAddType.ITEM_COLLECT, ItemUpdateType.INC_ITEM_COLLECT);
		}

		/**
		 * 解析物品对应的数量更新类型（基纳特殊处理）。
		 * Resolves the count update type for an item (special-cased for kinah).
		 *
		 * item
		 * whether increasing
		 * update type
		 */
		public ItemUpdateType getUpdateType(Item item, boolean isIncrease) {
			if (item.getItemTemplate().isKinah()) {
				return ItemUpdateType.getKinahUpdateTypeFromAddType(itemAddType, isIncrease);
			}
			return itemUpdateType;
		}

		/**
		 * 返回添加来源类型。
		 * Returns the add-source type.
		 *
		 * add type
		 */
		public ItemAddType getAddType() {
			return itemAddType;
		}

		/**
		 * 入包前可修改物品的钩子，默认不做改动。
		 * Hook to mutate the item before it enters inventory; default is no-op.
		 *
		 * item
		 * always true by default
		 */
		public boolean changeItem(Item item) {
			return true;
		}
	}

	/**
	 * 将源装备属性迁移到升级后的新物品（魔石/神石/强化/授权等会按规则衰减）。
	 * Migrates source gear attributes onto an upgraded item (manastones/godstone/enchant/authorize decay by rule).
	 *
	 * source item
	 * @param newItem 升级后物品 / upgraded item
	 */
	public static void makeUpgradeItem(Item sourceItem, Item newItem) {
		if (sourceItem.hasManaStones()) {
			for (ManaStone manaStone : sourceItem.getItemStones()) {
				ItemSocketService.addManaStone(newItem, manaStone.getItemId());
			}
		}
		if (sourceItem.getGodStone() != null) {
			newItem.addGodStone(sourceItem.getGodStone().getItemId(), sourceItem.getGodStone().getActivatedCount());
		}
		if (sourceItem.getEnchantLevel() > 0) {
			newItem.setEnchantLevel(sourceItem.getEnchantLevel() - 5);
		}
		if (sourceItem.getAuthorize() > 0 && sourceItem.getItemTemplate().isWeapon()) {
			newItem.setAuthorize(sourceItem.getAuthorize() - 5);
		}
		if (sourceItem.getAuthorize() > 0 && sourceItem.getItemTemplate().isPlume()) {
			newItem.setAuthorize(0);
		}
		if (sourceItem.isSoulBound()) {
			newItem.setSoulBound(true);
		}
		if (sourceItem.isAmplified()) {
			newItem.setEnchantLevel(sourceItem.getItemTemplate().getMaxEnchantLevel()
					+ sourceItem.getItemTemplate().getMaxEnchantBonus());
			newItem.setAmplification(false);
		}
		newItem.setPersistentState(PersistentState.UPDATE_REQUIRED);
	}

	/**
	 * 将物品强化/授权设为指定等级并同步外观数值。
	 * Sets item enchant/authorize to the given level and refreshes visual stats.
	 */
	private static void enchant(Player player, int enchant, Item item) {
		if (item.getEnchantLevel() == enchant) {
			return;
		}
		if (enchant > 255) {
			enchant = 255;
		}
		if (enchant < 0) {
			enchant = 0;
		}
		if (item.getItemTemplate().getMaxAuthorize()!=0)
			item.setAuthorize(enchant);
		else if (item.getItemTemplate().getMaxEnchantLevel()!=0)
			item.setEnchantLevel(EnchantService.capEquipmentEnchantLevel(enchant));
		if (item.isEquipped()) {
			player.getGameStats().updateStatsVisually();
		}
		ItemPacketService.updateItemAfterInfoChange(player, item);
	}

	/**
	 * 判断物品是否可升级（武器/印记/指定槽位防具，且非禁止强化）。
	 * Returns whether the item is upgradable (weapon/stigma/selected armor slots, not no-enchant).
	 *
	 * item
	 *
	 * @param item
	 * @return 是否可升级 / true if upgradable
	 */
	public static boolean isUpgradable(Item item) {
		if (item.getItemTemplate().isNoEnchant() && !item.getItemTemplate().isStigma()) {
			return false;
		}
		if (item.getItemTemplate().isWeapon()) {
			return true;
		}
		if (item.getItemTemplate().isStigma()) {
			return true;
		}
		if (item.getItemTemplate().isArmor()) {
			int at = item.getItemTemplate().getItemSlot();
			if (at == 1 || /* Main Hand */
					at == 2 || /* Sub Hand */
					at == 8 || /* Jacket */
					at == 16 || /* Gloves */
					at == 32 || /* Boots */
					at == 2048 || /* Shoulder */
					at == 4096 || /* Pants */
					at == 32768 || /* Wing */
					at == 131072 || /* Main Off Hand */
					at == 262144) { /* Sub Off Hand */
				return true;
			}
		}
		return false;
	}
}
