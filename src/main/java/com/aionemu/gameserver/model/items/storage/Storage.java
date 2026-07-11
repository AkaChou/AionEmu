package com.aionemu.gameserver.model.items.storage;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.items.ItemId;
import com.aionemu.gameserver.questEngine.QuestEngine;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.services.item.ItemFactory;
import com.aionemu.gameserver.services.item.ItemPacketService;
import com.aionemu.gameserver.services.item.ItemPacketService.ItemDeleteType;
import com.aionemu.gameserver.services.item.ItemPacketService.ItemUpdateType;

import java.util.ArrayList;

/**
 * 仓库，用于物品相关逻辑。
 * Storage for items logic.
 *
 * @author KID, ATracer
 */
public abstract class Storage implements IStorage {

	private ItemStorage itemStorage;
	private Item kinahItem;
	private StorageType storageType;
	private Queue<Item> deletedItems;
	/**
	 * 可为 UPDATED 与 UPDATE_REQUIRED 两种类型。 / Can be of 2 types: UPDATED and UPDATE_REQUIRED
	 */
	private PersistentState persistentState = PersistentState.UPDATED;

	public Storage(StorageType storageType) {
		this(storageType, true);
	}

	public Storage(StorageType storageType, boolean withDeletedItems) {
		itemStorage = new ItemStorage(storageType);
		this.storageType = storageType;
		if (withDeletedItems) {
			this.deletedItems = new ConcurrentLinkedQueue<Item>();
		}
	}

	/** 获取基纳。 / Returns the kinah. */
	@Override
	public long getKinah() {
		return kinahItem == null ? 0 : kinahItem.getItemCount();
	}

	/** 获取基纳物品。 / Returns the kinah item. */
	@Override
	public Item getKinahItem() {
		return kinahItem;
	}

	/** 获取仓库类型。 / Returns the storage type. */
	@Override
	public StorageType getStorageType() {
		return storageType;
	}

	void increaseKinah(long amount, Player actor) {
		increaseKinah(amount, ItemUpdateType.INC_KINAH_COLLECT, actor);
	}

	void increaseKinah(long amount, ItemUpdateType updateType, Player actor) {
		if (kinahItem == null) {
			add(ItemFactory.newItem(ItemId.KINAH.value(), 0), actor);
		}
		if (amount > 0) {
			increaseItemCount(kinahItem, amount, updateType, actor);
		}
	}

	/**
	 * 减少基纳（先检查存量是否足够）。 / Decrease kinah by {@code amount} but check first that its enough in storage.
	 */
	boolean tryDecreaseKinah(long amount, Player actor) {
		if (getKinah() >= amount) {
			decreaseKinah(amount, actor);
			return true;
		}
		return false;
	}

	/**
	 * just decrease kinah without any checks
	 */
	public void decreaseKinah(long amount, Player actor) {
		decreaseKinah(amount, ItemUpdateType.DEC_KINAH_BUY, actor);
	}

	void decreaseKinah(long amount, ItemUpdateType updateType, Player actor) {
		if (amount > 0) {
			decreaseItemCount(kinahItem, amount, updateType, actor);
		}
	}

	long increaseItemCount(Item item, long count, Player actor) {
		return increaseItemCount(item, count, ItemUpdateType.DEC_ITEM_USE, actor);
	}

	/**
	 * 增加物品数量并返回剩余数量 / increase item count and return left count
	 */
	long increaseItemCount(Item item, long count, ItemUpdateType updateType, Player actor) {
		long leftCount = item.increaseItemCount(count);
		ItemPacketService.sendItemPacket(actor, storageType, item, updateType);
		setPersistentState(PersistentState.UPDATE_REQUIRED);
		return leftCount;
	}

	long decreaseItemCount(Item item, long count, Player actor) {
		return this.decreaseItemCount(item, count, ItemUpdateType.DEC_ITEM_USE, actor);
	}

	/**
	 * decrease item count and return left count
	 */
	long decreaseItemCount(Item item, long count, ItemUpdateType updateType, Player actor) {
		if (item == null) {
			return 0;
		}
		long leftCount = item.decreaseItemCount(count);
		if (item.getItemCount() <= 0 && !item.getItemTemplate().isKinah()) {
			delete(item, ItemDeleteType.fromUpdateType(updateType), actor);
		} else {
			ItemPacketService.sendItemPacket(actor, storageType, item, updateType);
		}
		setPersistentState(PersistentState.UPDATE_REQUIRED);
		return leftCount;
	}

	/**
	 * 仅用于新加入背包的物品（从 DB 加载）；已装备则放入装备栏。 / This method should be called only for new items added to inventory (loading from DB) If item is equiped - will be put to equipment if item is unequiped - will be put to default bag for now Kinah is stored separately as it will be used frequently.
	 */
	@Override
	public void onLoadHandler(Item item) {
		if (item.getItemTemplate().isKinah()) {
			kinahItem = item;
		} else {
			itemStorage.putItem(item);
		}
	}

	Item add(Item item, Player actor) {
		if (item.getItemTemplate().isKinah()) {
			this.kinahItem = item;
		} else if (!itemStorage.putItem(item)) {
			return null;
		}
		item.setItemLocation(storageType.getId());
		setPersistentState(PersistentState.UPDATE_REQUIRED);
		ItemPacketService.sendStorageUpdatePacket(actor, storageType, item);
		GameEngineServices.questEngine().onItemGet(new QuestEnv(null, actor, 0, 0), item.getItemTemplate().getTemplateId());
		if (item.getItemTemplate().isQuestUpdateItem()) {
			actor.getController().updateZone();
			actor.getController().updateNearbyQuests();
		}
		return item;
	}

	// 名称略有误导——但似乎仅用于装备 / a bit misleading name - but looks like its used only for equipment
	Item put(Item item, Player actor) {
		if (!itemStorage.putItem(item)) {
			return null;
		}
		item.setItemLocation(storageType.getId());
		setPersistentState(PersistentState.UPDATE_REQUIRED);
		ItemPacketService.sendItemUpdatePacket(actor, storageType, item, ItemUpdateType.EQUIP_UNEQUIP);
		return item;
	}

	/**
	 * 移除物品从 storage 无 changing 其 state。 / Remove item from storage without changing its state
	 */
	@Override
	public Item remove(Item item) {
		return itemStorage.removeItem(item.getObjectId());
	}

	/**
	 * 删除物品从 storage 并 mark 用于 DB 更新 .QUEST_REWARD 删除 type。 / Delete item from storage and mark for DB update. QUEST_REWARD delete type
	 */
	Item delete(Item item, Player actor) {
		return delete(item, ItemDeleteType.QUEST_REWARD, actor);
	}

	/**
	 * 删除物品从 storage 并 mark 用于 DB 更新。 / Delete item from storage and mark for DB update
	 */
	Item delete(Item item, ItemDeleteType deleteType, Player actor) {
		if (remove(item) != null) {
			item.setPersistentState(PersistentState.DELETED);
			deletedItems.add(item);
			setPersistentState(PersistentState.UPDATE_REQUIRED);
			ItemPacketService.sendItemDeletePacket(actor, StorageType.getStorageTypeById(item.getItemLocation()), item,
					deleteType);
			if (item.getItemTemplate().isQuestUpdateItem()) {
				actor.getController().updateZone();
				actor.getController().updateNearbyQuests();
			}
			return item;
		}
		return null;
	}

	boolean decreaseByItemId(int itemId, long count, Player actor) {
		List<Item> items = itemStorage.getItemsById(itemId);
		if (items.size() == 0) {
			return false;
		}
		for (Item item : items) {
			if (count == 0) {
				break;
			}
			count = decreaseItemCount(item, count, actor);
		}

		items.clear();
		return count == 0;
	}

	boolean decreaseByObjectId(int itemObjId, long count, Player actor) {
		return decreaseByObjectId(itemObjId, count, ItemUpdateType.DEC_ITEM_USE, actor);
	}

	boolean decreaseByObjectId(int itemObjId, long count, ItemUpdateType updateType, Player actor) {
		Item item = itemStorage.getItemByObjId(itemObjId);
		if (item == null || item.getItemCount() < count) {
			return false;
		}
		return decreaseItemCount(item, count, updateType, actor) == 0;
	}

	/** 按物品 ID 返回 first item / Returns the first item by item id */
	@Override
	public Item getFirstItemByItemId(int itemId) {
		return this.itemStorage.getFirstItemById(itemId);
	}

	/** 返回物品基纳 / Returns the items with kinah*/
	@Override
	public List<Item> getItemsWithKinah() {
		List<Item> items = this.itemStorage.getItems();
		if (this.kinahItem != null) {
			items.add(this.kinahItem);
		}
		return items;
	}

	/** 获取物品。 / Returns the items. */
	@Override
	public List<Item> getItems() {
		return this.itemStorage.getItems();
	}

	/** 返回按物品 ID 的物品 / Returns the items by item id */
	@Override
	public List<Item> getItemsByItemId(int itemId) {
		return this.itemStorage.getItemsById(itemId);
	}

	/** 返回 deleted items / Returns the deleted items */
	@Override
	public Queue<Item> getDeletedItems() {
		return deletedItems;
	}

	/** 返回按对象 ID 的物品 / Returns the item by obj id */
	@Override
	public Item getItemByObjId(int itemObjId) {
		return this.itemStorage.getItemByObjId(itemObjId);
	}

	/** 返回按物品 ID 的物品数量 / Returns the item count by item id */
	@Override
	public long getItemCountByItemId(int itemId) {
		List<Item> temp = this.itemStorage.getItemsById(itemId);
		if (temp.size() == 0) {
			return 0;
		}
		long cnt = 0;
		for (Item item : temp) {
			cnt += item.getItemCount();
		}
		return cnt;
	}

	/** 是否已满。 / Whether Full. */
	@Override
	public boolean isFull() {
		return this.itemStorage.isFull();
	}

	/**
	 * @return Whether full special cube / Whether full special cube
	 */
	public boolean isFullSpecialCube() {
		return this.itemStorage.isFullSpecialCube();
	}

	/** 是否已满。 / Whether Full. */
	public boolean isFull(int inventory) {
		if (inventory > 0) {
			return isFullSpecialCube();
		}
		return isFull();
	}

	/** 返回 free slots / Returns the free slots */
	public int getFreeSlots(int inventory) {
		if (inventory > 0) {
			return getSpecialCubeFreeSlots();
		}
		return getFreeSlots();
	}

	/** 返回 special cube free slots / Returns the special cube free slots */
	public int getSpecialCubeFreeSlots() {
		return this.itemStorage.getSpecialCubeFreeSlots();
	}

	/** 返回 free slots / Returns the free slots */
	@Override
	public int getFreeSlots() {
		return this.itemStorage.getFreeSlots();
	}

	/** 设置限制。 / Sets the limit. */
	public boolean setLimit(int limit) {
		return this.itemStorage.setLimit(limit);
	}

	/** 获取限制。 / Returns the limit. */
	@Override
	public int getLimit() {
		return this.itemStorage.getLimit();
	}

	/** 获取持久化状态。 / Returns the persistent state. */
	@Override
	public final PersistentState getPersistentState() {
		return persistentState;
	}

	/** 设置持久化状态。 / Sets the persistent state. */
	@Override
	public final void setPersistentState(PersistentState persistentState) {
		this.persistentState = persistentState;
	}

	/** 大小 / size. */
	@Override
	public int size() {
		return itemStorage.size();
	}

	/** 清空。 / Clear. */
	public void clear() {
		for (Item i : itemStorage.getItems()) {
			remove(i);
		}
	}
}
