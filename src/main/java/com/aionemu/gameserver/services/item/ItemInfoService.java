package com.aionemu.gameserver.services.item;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.templates.item.ItemQuality;
import com.aionemu.gameserver.model.templates.item.ItemTemplate;

/**
 * 物品信息查询服务，提供物品详情相关辅助。
 * Item info service providing helpers for item detail queries.
 *
 * @author ATracer
 */

public class ItemInfoService {

	/**
	 * getQuality 方法。
	 * getQuality method.
	 *
	 * itemId
	 * result
	 */
	public static final ItemQuality getQuality(int itemId) {
		return getItemTemplate(itemId).getItemQuality();
	}

	/**
	 * getNameId 方法。
	 * getNameId method.
	 *
	 * itemId
	 * result
	 */
	public static final int getNameId(int itemId) {
		return getItemTemplate(itemId).getNameId();
	}

	/**
	 * getItemTemplate 方法。
	 * getItemTemplate method.
	 *
	 * itemId
	 * result
	 */
	public static final ItemTemplate getItemTemplate(int itemId) {
		return DataManager.ITEM_DATA.getItemTemplate(itemId);
	}
}