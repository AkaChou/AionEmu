package com.aionemu.gameserver.model.items.storage;

import java.util.ArrayList;
import java.util.List;

import com.aionemu.gameserver.model.gameobjects.Item;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 物品仓库模型。
 * Item Storage model.
 */

public class ItemStorage {
	public static final long FIRST_AVAILABLE_SLOT = 65535L;

	private Map<Integer, Item> items;
	private int limit;
	private int specialLimit;
	public ItemStorage(StorageType storageType) {
		this.limit = storageType.getLimit();
		this.specialLimit = storageType.getSpecialLimit();
		this.items = new LinkedHashMap<>();
	}

	/** 获取物品。 / Returns the items. */
	public List<Item> getItems() {
		List<Item> temp = new ArrayList<>();
		temp.addAll(items.values());
		return temp;
	}

	/** 获取限制。 / Returns the limit. */
	public int getLimit() {
		return this.limit;
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
		for (Item item : items.values()) {
			if (item.getItemTemplate().getTemplateId() == itemId) {
				return item;
			}
		}
		return null;
	}

	/** 返回按 ID 的物品 / Returns the items by id */
	public List<Item> getItemsById(int itemId) {
		List<Item> temp = new ArrayList<>();
		for (Item item : items.values()) {
			if (item.getItemTemplate().getTemplateId() == itemId) {
				temp.add(item);
			}
		}
		return temp;
	}

	/** 返回按对象 ID 的物品 / Returns the item by obj id */
	public Item getItemByObjId(int itemObjId) {
		return this.items.get(itemObjId);
	}

	/** 按物品 ID 返回槽位 ID / Returns the slot id by item id */
	public long getSlotIdByItemId(int itemId) {
		for (Item item : this.items.values()) {
			if (item.getItemTemplate().getTemplateId() == itemId) {
				return item.getEquipmentSlot();
			}
		}
		return -1;
	}

	/** 按槽位 ID 返回物品 / Returns the item by slot id */
	public Item getItemBySlotId(short slotId) {
		for (Item item : getCubeItems()) {
			if (item.getEquipmentSlot() == slotId) {
				return item;
			}
		}
		return null;
	}

	/** 按槽位 ID 返回特殊物品 / Returns the special item by slot id */
	public Item getSpecialItemBySlotId(short slotId) {
		for (Item item : getSpecialCubeItems()) {
			if (item.getEquipmentSlot() == slotId) {
				return item;
			}
		}
		return null;
	}

	/** 按对象 ID 返回槽位 ID / Returns the slot id by obj id */
	public long getSlotIdByObjId(int objId) {
		Item item = this.getItemByObjId(objId);
		if (item != null) {
			return item.getEquipmentSlot();
		} else {
			return -1;
		}
	}

	/** 返回下一个可用槽位 / Returns the next available slot */
	public long getNextAvailableSlot() {
		return FIRST_AVAILABLE_SLOT;
	}

	/** 放入物品。 / Put item. */
	public boolean putItem(Item item) {
		if (this.items.containsKey(item.getObjectId())) {
			return false;
		}
		this.items.put(item.getObjectId(), item);
		return true;
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
		List<Item> result = new ArrayList<Item>();
		for (Item item : items.values()) {
			if (item.getItemTemplate().getExtraInventoryId() > 0) {
				result.add(item);
			}
		}
		return result;
	}

	/** 获取魔立方物品。 / Returns the cube items. */
	public List<Item> getCubeItems() {
		List<Item> result = new ArrayList<Item>();
		for (Item item : items.values()) {
			if (item.getItemTemplate().getExtraInventoryId() < 1) {
				result.add(item);
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
}
