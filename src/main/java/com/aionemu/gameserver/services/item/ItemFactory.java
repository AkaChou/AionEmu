package com.aionemu.gameserver.services.item;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.templates.item.ItemTemplate;
import com.aionemu.gameserver.utils.idfactory.IDFactory;

/**
 * 物品工厂，按模板与数量创建物品实例。
 * Item factory creating item instances from templates and counts.
 *
 * @author ATracer
 */

@Slf4j
public class ItemFactory {


	/**
	 * 创建物品实例。
	 * Creates a new item instance.
	 *
	 * itemId
	 * result
	 */
	public static final Item newItem(int itemId) {
		ItemTemplate itemTemplate = DataManager.ITEM_DATA.getItemTemplate(itemId);
		if (itemTemplate == null) {
			log.error(I18n.get("log.c0940ff7ef75", itemId));
			return null;
		}
		return new Item(GameWorldBootstrapServices.idFactory().nextId(), itemTemplate);
	}

	/**
	 * 创建物品实例。
	 * Creates a new item instance.
	 *
	 * itemId
	 * count
	 * result
	 */
	public static Item newItem(int itemId, long count) {
		Item item = newItem(itemId);
		item.setItemCount(calculateCount(item.getItemTemplate(), count));
		return item;
	}

	private static final long calculateCount(ItemTemplate itemTemplate, long count) {
		long maxStackCount = itemTemplate.getMaxStackCount();
		if (count > maxStackCount && !itemTemplate.isKinah()) {
			count = maxStackCount;
		}
		return count;
	}
}