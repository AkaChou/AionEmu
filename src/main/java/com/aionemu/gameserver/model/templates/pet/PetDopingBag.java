package com.aionemu.gameserver.model.templates.pet;

import java.util.Arrays;

/**
 * 宠物兴奋剂包模板（静态数据/XML）。
 * Pet doping bag template (static data / XML).
 *
 * @author Rolandas
 */
public class PetDopingBag {

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

	/** 设置饮品物品 / Sets the drink item */
	public void setDrinkItem(int itemId) {
		setItem(itemId, 1);
	}

	/** 返回饮品物品 / Returns the drink item */
	public int getDrinkItem() {
		if (itemBag == null || itemBag.length < 2) {
			return 0;
		}
		return itemBag[1];
	}

	/**
	 * 在指定槽位添加或移除物品。 / Adds or removes an item in the given slot.
	 *
	 * @param itemId 物品 ID，0 表示移除 / item Id, or 0 to remove
	 * @param slot   槽位编号；0 为食物，1 为饮品，其余为卷轴 / slot number; 0 for food, 1 for drink, the rest for scrolls
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

	/** 返回已用卷轴 / Returns the scrolls used */
	public int[] getScrollsUsed() {
		if (itemBag == null || itemBag.length < 3) {
			return new int[0];
		}
		return Arrays.copyOfRange(itemBag, 2, itemBag.length);
	}

	/**
	 * @return 是否需要保存 / true if the bag needs saving
	 */
	public boolean isDirty() {
		return isDirty;
	}
}
