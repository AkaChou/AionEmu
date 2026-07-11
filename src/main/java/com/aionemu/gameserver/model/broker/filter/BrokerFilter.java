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
	 * @param template
	 * @return
	 */
	public abstract boolean accept(ItemTemplate template);
}
