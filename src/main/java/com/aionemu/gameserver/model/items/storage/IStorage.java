package com.aionemu.gameserver.model.items.storage;

import java.util.List;
import java.util.Queue;

import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.services.item.ItemPacketService.ItemDeleteType;
import com.aionemu.gameserver.services.item.ItemPacketService.ItemUpdateType;


/**
 * 仓库接口。
 * Storage interface.
 * @author ATracer
 */
public interface IStorage {

	void setOwner(Player player);

	/**
	 * @return current kinah count
	 */
	long getKinah();

	/**
	 * @return kinah item or null if storage never had kinah
	 */
	Item getKinahItem();

	StorageType getStorageType();

	void increaseKinah(long amount);

	void increaseKinah(long amount, ItemUpdateType updateType);

	boolean tryDecreaseKinah(long amount);

	void decreaseKinah(long amount);

	void decreaseKinah(long amount, ItemUpdateType updateType);

	long increaseItemCount(Item item, long count);

	long increaseItemCount(Item item, long count, ItemUpdateType updateType);

	long decreaseItemCount(Item item, long count);

	long decreaseItemCount(Item item, long count, ItemUpdateType updateType);

	/**
	 * 添加操作应仅用于从外部进入仓库的新物品。 / Add operation should be used for new items incoming into storage from outside
	 */
	Item add(Item item);

	/**
	 * 放置操作，用于卸下装备等场景。 / Put operation is used in some operations like unequip
	 */
	Item put(Item item);

	Item remove(Item item);

	Item delete(Item item);

	Item delete(Item item, ItemDeleteType deleteType);

	boolean decreaseByItemId(int itemId, long count);

	boolean decreaseByObjectId(int itemObjId, long count);

	boolean decreaseByObjectId(int itemObjId, long count, ItemUpdateType updateType);

	Item getFirstItemByItemId(int itemId);

	List<Item> getItemsWithKinah();

	List<Item> getItems();

	List<Item> getItemsByItemId(int itemId);

	Item getItemByObjId(int itemObjId);

	long getItemCountByItemId(int itemId);

	boolean isFull();

	int getFreeSlots();

	int getLimit();

	int size();

	PersistentState getPersistentState();

	void setPersistentState(PersistentState persistentState);

	Queue<Item> getDeletedItems();

	void onLoadHandler(Item item);
}
