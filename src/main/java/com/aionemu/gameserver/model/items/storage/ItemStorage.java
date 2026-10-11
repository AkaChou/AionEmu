package com.aionemu.gameserver.model.items.storage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.aionemu.gameserver.model.gameobjects.Item;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Getter;

/**
 * 物品仓库模型。
 * Item Storage model.
 * <p>
 * 并发语义：背包既会被玩家 IO 线程增删，也会被后台任务（如活动掉落、
 * 定时保存）遍历，因此底层 map 使用 synchronizedMap，所有单次读写自带
 * 同一把监视器互斥；类内的批量迭代与 check-then-put 复合操作必须以
 * {@code synchronized (items)} 包住，禁止在锁外裸迭代 map。
 * </p>
 * Concurrency semantics: the storage is mutated by the player's IO thread
 * and iterated by background tasks (event drops, periodic saves). The
 * backing map is a synchronizedMap so single reads/writes share one
 * monitor; bulk iterations and the check-then-put compound operation must
 * be wrapped in {@code synchronized (items)}. Never iterate the map raw.
 */

@Getter
public class ItemStorage {
	public static final long FIRST_AVAILABLE_SLOT = 65535L;

	private final Map<Integer, Item> items;
	/** 获取限制。 / Returns the limit. */
	private int limit;
	private final int specialLimit;
	public ItemStorage(StorageType storageType) {
		this.limit = storageType.getLimit();
		this.specialLimit = storageType.getSpecialLimit();
		this.items = Collections.synchronizedMap(new LinkedHashMap<>());
	}

	/** 获取物品。 / Returns the items. */
	public List<Item> getItems() {
		synchronized (items) {
			return new ArrayList<>(items.values());
		}
	}

	/** 设置限制。 / Sets the limit. */
	public boolean setLimit(int limit) {
		if (getCubeItems().size() > limit) {
			return false;
		}

		this.limit = limit;
		return true;
	}

	/** 按物品 ID 返回第一件物品 / Returns the first item by id */
	public Item getFirstItemById(int itemId) {
		synchronized (items) {
			for (Item item : items.values()) {
				if (item.getItemTemplate().getTemplateId() == itemId) {
					return item;
				}
			}
		}
		return null;
	}

	/** 返回按 ID 的物品 / Returns the items by id */
	public List<Item> getItemsById(int itemId) {
		List<Item> temp = new ArrayList<>();
		synchronized (items) {
			for (Item item : items.values()) {
				if (item.getItemTemplate().getTemplateId() == itemId) {
					temp.add(item);
				}
			}
		}
		return temp;
	}

	/** 返回按对象 ID 的物品 / Returns the item by obj id */
	public Item getItemByObjId(int itemObjId) {
		return this.items.get(itemObjId);
	}

	/** 放入物品。 / Put item. */
	public boolean putItem(Item item) {
		synchronized (items) {
			if (this.items.containsKey(item.getObjectId())) {
				return false;
			}
			this.items.put(item.getObjectId(), item);
			return true;
		}
	}

	/** 移除物品。 / Removes item. */
	public Item removeItem(int objId) {
		return this.items.remove(objId);
	}

	/** 是否已满。 / Whether Full. */
	public boolean isFull() {
		return getCubeItems().size() >= limit;
	}

	/**
	 * @return 特殊魔立方是否已满 / whether the special cube is full
	 */
	public boolean isFullSpecialCube() {
		return getSpecialCubeItems().size() >= specialLimit;
	}

	/** 返回特殊魔立方物品 / Returns the special cube items */
	public List<Item> getSpecialCubeItems() {
		List<Item> result = new ArrayList<>();
		synchronized (items) {
			for (Item item : items.values()) {
				if (item.getItemTemplate().getExtraInventoryId() > 0) {
					result.add(item);
				}
			}
		}
		return result;
	}

	/** 获取魔立方物品。 / Returns the cube items. */
	public List<Item> getCubeItems() {
		List<Item> result = new ArrayList<>();
		synchronized (items) {
			for (Item item : items.values()) {
				if (item.getItemTemplate().getExtraInventoryId() < 1) {
					result.add(item);
				}
			}
		}
		return result;
	}

	/** 返回可用槽位数 / Returns the free slots */
	public int getFreeSlots() {
		return limit - getCubeItems().size();
	}

	/** 返回特殊魔立方可用槽位数 / Returns the special cube free slots */
	public int getSpecialCubeFreeSlots() {
		return specialLimit - getSpecialCubeItems().size();
	}

	/** 物品数量。 / Size. */
	public int size() {
		return this.items.size();
	}

	/**
	 * 返回是否没有任何物品。
	 * Returns whether there is no item at all.
	 * @return 无物品时为 true / true if empty
	 */
	public boolean isEmpty() {
		return this.items.isEmpty();
	}
}
