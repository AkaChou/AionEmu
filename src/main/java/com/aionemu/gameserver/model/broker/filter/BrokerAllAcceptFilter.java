package com.aionemu.gameserver.model.broker.filter;

import com.aionemu.gameserver.model.templates.item.ItemTemplate;

/**
 * 经纪行 AllAcceptFilter 模型。
 * Broker All Accept Filter model.
 *
 * @author ATracer
 */
public class BrokerAllAcceptFilter extends BrokerFilter {

	/** 接受 / accept. */
	@Override
	public boolean accept(ItemTemplate template) {
		return true;
	}
}
