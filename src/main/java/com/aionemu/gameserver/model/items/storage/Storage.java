package com.aionemu.gameserver.model.items.storage;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

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
	 * 增加基纳但不发送客户端封包；事务调用方须在 JDBC 事务提交后才发布物品更新。
	 * Increases kinah without sending a client packet. Transactional callers must
	 * publish the item update only after their JDBC transaction commits.
	 *
	 * @return 无法装入基纳堆叠的剩余数量 / the amount that could not fit in the kinah stack
	 */
	public long increaseKinahSilently(long amount) {
		if (amount <= 0) {
			return 0;
		}
		if (kinahItem == null) {
			kinahItem = ItemFactory.newItem(ItemId.KINAH.value(), 0);
			kinahItem.setItemLocation(storageType.getId());
		}
		long leftCount = kinahItem.increaseItemCount(amount);
		setPersistentState(PersistentState.UPDATE_REQUIRED);
		return leftCount;
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
	 * 不检查直接减少基纳。 / Just decrease kinah without any checks.
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
	 * 增加物品数量并返回剩余数量。 / Increase item count and return left count.
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
	 * 减少物品数量并返回剩余数量。 / Decrease item count and return left count.
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
	 * 将物品从仓库移除，不改变其状态。 / Remove item from storage without changing its state
	 */
	@Override
	public Item remove(Item item) {
		return itemStorage.removeItem(item.getObjectId());
	}

	/**
	 * 从仓库删除物品并标记 DB 更新，删除类型为 QUEST_REWARD。 / Delete item from storage and mark for DB update. QUEST_REWARD delete type
	 */
	Item delete(Item item, Player actor) {
		return delete(item, ItemDeleteType.QUEST_REWARD, actor);
	}

	/**
	 * 从仓库删除物品并标记 DB 更新。 / Delete item from storage and mark for DB update
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

	/** 按物品 ID 返回第一件物品 / Returns the first item by item id */
	@Override
	public Item getFirstItemByItemId(int itemId) {
		return this.itemStorage.getFirstItemById(itemId);
	}

	/** 返回含基纳的物品列表 / Returns the items with kinah */
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
	 * @return 特殊魔立方是否已满 / whether the special cube is full
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

	/** 返回可用槽位数 / Returns the free slots */
	public int getFreeSlots(int inventory) {
		if (inventory > 0) {
			return getSpecialCubeFreeSlots();
		}
		return getFreeSlots();
	}

	/** 返回特殊魔立方可用槽位数 / Returns the special cube free slots */
	public int getSpecialCubeFreeSlots() {
		return this.itemStorage.getSpecialCubeFreeSlots();
	}

	/** 返回可用槽位数 / Returns the free slots */
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

	/**
	 * 捕捉存储区的精确实时状态，供调用方自有事务回滚。
	 * Captures the exact live storage state for caller-owned transaction rollback.
	 */
	public TransactionSnapshot transactionSnapshot() {
		return new TransactionSnapshot();
	}

	public final class TransactionSnapshot {
		private final Map<Integer, ItemState> items = new LinkedHashMap<>();
		private final ItemState kinah = kinahItem == null ? null : new ItemState(kinahItem);
		private final List<Item> deleted = deletedItems == null ? List.of() : List.copyOf(deletedItems);
		private final PersistentState storageState = persistentState;
		private boolean restored;

		private TransactionSnapshot() {
			for (Item item : itemStorage.getItems()) {
				items.put(item.getObjectId(), new ItemState(item));
			}
		}

		public void restore() {
			restore(item -> { });
		}

		public void restore(Consumer<Item> discardedItem) {
			if (discardedItem == null) {
				throw new IllegalArgumentException("discardedItem must not be null");
			}
			if (restored) {
				return;
			}
			restored = true;
			for (Item current : new ArrayList<>(itemStorage.getItems())) {
				itemStorage.removeItem(current.getObjectId());
				if (!items.containsKey(current.getObjectId()) && current.getPersistentState() == PersistentState.NEW) {
					discardedItem.accept(current);
				}
			}
			for (ItemState state : items.values()) {
				if (!itemStorage.putItem(state.item)) {
					throw new IllegalStateException("cannot restore storage item " + state.item.getObjectId());
				}
				state.restore();
			}
			if (kinah == null && kinahItem != null && kinahItem.getPersistentState() == PersistentState.NEW) {
				discardedItem.accept(kinahItem);
			}
			kinahItem = kinah == null ? null : kinah.item;
			if (kinah != null) {
				kinah.restore();
			}
			if (deletedItems != null) {
				deletedItems.clear();
				deletedItems.addAll(deleted);
			}
			persistentState = storageState;
		}
	}

	private static final class ItemState {
		private final Item item;
		private final long count;
		private final int location;
		private final PersistentState persistentState;

		private ItemState(Item item) {
			this.item = item;
			this.count = item.getItemCount();
			this.location = item.getItemLocation();
			this.persistentState = item.getPersistentState();
		}

		private void restore() {
			item.setItemCount(count);
			item.setItemLocation(location);
			item.setPersistentState(persistentState);
		}
	}

	/** 物品数量。 / Size. */
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
