package com.aionemu.gameserver.model.broker.filter;

import org.apache.commons.lang3.ArrayUtils;

import com.aionemu.gameserver.model.templates.item.ItemTemplate;

/**
 * 经纪行 ContainsExtraFilter 模型。
 * Broker Contains Extra Filter model.
 *
 * @author ATracer
 */
public class BrokerContainsExtraFilter extends BrokerFilter {

	private int[] masks;

	/**
	 * @param masks
	 */
	public BrokerContainsExtraFilter(int... masks) {
		this.masks = masks;
	}

	/** 接受 / accept. */
	@Override
	public boolean accept(ItemTemplate template) {
		return ArrayUtils.contains(masks, template.getTemplateId() / 10000);
	}
}
