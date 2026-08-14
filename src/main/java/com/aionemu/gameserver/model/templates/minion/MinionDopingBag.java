package com.aionemu.gameserver.model.templates.minion;

import java.util.Arrays;

/**
 * 守护灵强化袋模板（静态数据/XML）。
 * Minion doping bag template (static data/XML).
 *
 * @author jacjozs this copy Pet_Bag
 */
public class MinionDopingBag {
	private int[] itemBag = null;
	private boolean isDirty = false;

	/** 设置食物物品 / Sets the food item*/
	public void setFoodItem(int itemId) {
		setItem(itemId, 0);
	}

	/** 返回食物物品 / Returns the food item*/
	public int getFoodItem() {
		if (itemBag == null || itemBag.length < 1) {
			return 0;
		}
		return itemBag[0];
	}

	/** 设置 drink item / Sets the drink item */
	public void setDrinkItem(int itemId) {
		setItem(itemId, 1);
	}

	/** 返回 drink item / Returns the drink item */
	public int getDrinkItem() {
		if (itemBag == null || itemBag.length < 2) {
			return 0;
		}
		return itemBag[1];
	}

	/**
	 * 添加或移除物品到 bag。 / Adds or removes item to the bag
	 *
	 * @param itemId - item Id, or 0 to remove
	 * @param slot   - slot number; 0 for food, 1 for drink, the rest are for
	 */
	public void setItem(int itemId, int slot) {
		if (itemBag == null) {
			itemBag = new int[slot + 1];
			isDirty = true;
		} else if (slot > itemBag.length - 1) {
			itemBag = Arrays.copyOf(itemBag, slot + 1);
			isDirty = true;
		}
		if (itemBag[slot] != itemId) {
			itemBag[slot] = itemId;
			isDirty = true;
		}
	}

	/** 返回指定槽位的物品 / Returns the item in the given slot */
	public int getItem(int slot) {
		return itemBag == null || slot < 0 || slot >= itemBag.length ? 0 : itemBag[slot];
	}

	/** 返回 scrolls used / Returns the scrolls used */
	public int[] getScrollsUsed() {
		if (itemBag == null || itemBag.length < 3) {
			return new int[0];
		}
		return Arrays.copyOfRange(itemBag, 2, itemBag.length);
	}

	/**
	 * @return true if the bag needs saving
	 */
	public boolean isDirty() {
		return isDirty;
	}
}
