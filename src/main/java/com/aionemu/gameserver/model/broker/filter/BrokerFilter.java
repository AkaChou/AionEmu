package com.aionemu.gameserver.model.broker.filter;

import com.aionemu.gameserver.model.templates.item.ItemTemplate;

/**
 * 经纪行 Filter 模型。
 * Broker Filter model.
 *
 * @author ATracer
 */
public abstract class BrokerFilter {

	/**
	 * 判断物品模板是否通过过滤条件。
	 * Whether the item template passes the filter.
	 *
	 * @param template 物品模板 / item template
	 * @return 是否接受 / whether accepted
	 */
	public abstract boolean accept(ItemTemplate template);
}
