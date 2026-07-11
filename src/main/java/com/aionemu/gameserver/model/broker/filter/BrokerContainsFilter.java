package com.aionemu.gameserver.model.broker.filter;

import org.apache.commons.lang3.ArrayUtils;

import com.aionemu.gameserver.model.templates.item.ItemTemplate;

/**
 * 经纪行 ContainsFilter 模型。
 * Broker Contains Filter model.
 *
 * @author ATracer
 */
public class BrokerContainsFilter extends BrokerFilter {

	private int[] masks;

	/**
	 * @param masks
	 */
	public BrokerContainsFilter(int... masks) {
		this.masks = masks;
	}

	/** 接受 / accept. */
	@Override
	public boolean accept(ItemTemplate template) {
		return ArrayUtils.contains(masks, template.getTemplateId() / 100000);
	}
}
