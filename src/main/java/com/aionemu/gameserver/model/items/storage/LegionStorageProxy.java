package com.aionemu.gameserver.model.items.storage;

import java.util.List;
import java.util.Queue;

import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.services.item.ItemPacketService.ItemDeleteType;
import com.aionemu.gameserver.services.item.ItemPacketService.ItemUpdateType;

import java.util.ArrayList;

/**
 * 军团仓库 Proxy，用于物品相关逻辑。
 * Legion Storage Proxy for items logic.
 *
 * @author ATracer
 */
public class LegionStorageProxy extends Storage {

	private final Player actor;
	private final Storage storage;

	public LegionStorageProxy(Storage storage, Player actor) {
		super(storage.getStorageType(), false);
		this.actor = actor;
		this.storage = storage;
	}

	/** 增加基纳。 / Increase kinah. */
	@Override
	public void increaseKinah(long amount) {
		storage.increaseKinah(amount, actor);
	}

	/** 增加基纳。 / Increase kinah. */
	@Override
	public void increaseKinah(long amount, ItemUpdateType updateType) {
		storage.increaseKinah(amount, updateType, actor);
	}

	/** 尝试减少基纳。 / Try to decrease kinah. */
	@Override
	public boolean tryDecreaseKinah(long amount) {
		return storage.tryDecreaseKinah(amount, actor);
	}

	/** 减少基纳。 / Decrease kinah. */
	@Override
	public void decreaseKinah(long amount) {
		storage.decreaseKinah(amount, actor);
	}

	/** 减少基纳。 / Decrease kinah. */
	@Override
	public void decreaseKinah(long amount, ItemUpdateType updateType) {
		storage.decreaseKinah(amount, updateType, actor);
	}

	/** 增加物品计数。 / Increase item count. */
	@Override
	public long increaseItemCount(Item item, long count) {
		return storage.increaseItemCount(item, count, actor);
	}

	/** 增加物品计数。 / Increase item count. */
	@Override
	public long increaseItemCount(Item item, long count, ItemUpdateType updateType) {
		return storage.increaseItemCount(item, count, updateType, actor);
	}

	/** 减少物品计数。 / Decrease item count. */
	@Override
	public long decreaseItemCount(Item item, long count) {
		return storage.decreaseItemCount(item, count, actor);
	}

	/** 减少物品计数。 / Decrease item count. */
	@Override
	public long decreaseItemCount(Item item, long count, ItemUpdateType updateType) {
		return storage.decreaseItemCount(item, count, updateType, actor);
	}

	/** 添加。 / Add. */
	@Override
	public Item add(Item item) {
		return storage.add(item, actor);
	}

	/** 放入。 / Put. */
	@Override
	public Item put(Item item) {
		return storage.put(item, actor);
	}

	/** 删除。 / Delete. */
	@Override
	public Item delete(Item item) {
		return storage.delete(item, actor);
	}

	/** 删除。 / Delete. */
	@Override
	public Item delete(Item item, ItemDeleteType deleteType) {
		return storage.delete(item, deleteType, actor);
	}

	/** 按物品 ID 减少 / Decrease by item id */
	@Override
	public boolean decreaseByItemId(int itemId, long count) {
		return storage.decreaseByItemId(itemId, count, actor);
	}

	/** 按对象 ID 减少 / Decrease by object id */
	@Override
	public boolean decreaseByObjectId(int itemObjId, long count) {
		return storage.decreaseByObjectId(itemObjId, count, actor);
	}

	/** 按对象 ID 减少 / Decrease by object id */
	@Override
	public boolean decreaseByObjectId(int itemObjId, long count, ItemUpdateType updateType) {
		return storage.decreaseByObjectId(itemObjId, count, updateType, actor);
	}

	/** 获取基纳。 / Returns the kinah. */
	@Override
	public long getKinah() {
		return storage.getKinah();
	}

	/** 获取基纳物品。 / Returns the kinah item. */
	@Override
	public Item getKinahItem() {
		return storage.getKinahItem();
	}

	/** 获取仓库类型。 / Returns the storage type. */
	@Override
	public StorageType getStorageType() {
		return storage.getStorageType();
	}

	/** 加载回调。 / On load handler. */
	@Override
	public void onLoadHandler(Item item) {
		storage.onLoadHandler(item);
	}

	/** 移除。 / Remove. */
	@Override
	public Item remove(Item item) {
		return storage.remove(item);
	}

	/** 按物品 ID 返回第一件物品 / Returns the first item by item id */
	@Override
	public Item getFirstItemByItemId(int itemId) {
		return storage.getFirstItemByItemId(itemId);
	}

	/** 返回含基纳的物品列表 / Returns the items with kinah */
	@Override
	public List<Item> getItemsWithKinah() {
		return storage.getItemsWithKinah();
	}

	/** 获取物品。 / Returns the items. */
	@Override
	public List<Item> getItems() {
		return storage.getItems();
	}

	/** 返回按物品 ID 的物品 / Returns the items by item id */
	@Override
	public List<Item> getItemsByItemId(int itemId) {
		return storage.getItemsByItemId(itemId);
	}

	/** 返回 deleted items / Returns the deleted items */
	@Override
	public Queue<Item> getDeletedItems() {
		return storage.getDeletedItems();
	}

	/** 返回按对象 ID 的物品 / Returns the item by obj id */
	@Override
	public Item getItemByObjId(int itemObjId) {
		return storage.getItemByObjId(itemObjId);
	}

	/** 是否已满。 / Whether Full. */
	@Override
	public boolean isFull() {
		return storage.isFull();
	}

	/** 返回可用槽位数 / Returns the free slots */
	@Override
	public int getFreeSlots() {
		return storage.getFreeSlots();
	}

	/** 设置限制。 / Sets the limit. */
	@Override
	public boolean setLimit(int limit) {
		return storage.setLimit(limit);
	}

	/** 获取限制。 / Returns the limit. */
	@Override
	public int getLimit() {
		return storage.getLimit();
	}

	/** 物品数量。 / Size. */
	@Override
	public int size() {
		return storage.size();
	}

	/** 设置所有者。 / Sets the owner. */
	@Override
	public void setOwner(Player player) {
		throw new UnsupportedOperationException("LWH doesnt have owner");
	}
}
