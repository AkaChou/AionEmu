package com.aionemu.gameserver.model.broker.filter;

import com.aionemu.gameserver.model.templates.item.ItemTemplate;

/**
 * 经纪行 MinMaxFilter 模型。
 * Broker Min Max Filter model.
 *
 * @author ATracer
 */
public class BrokerMinMaxFilter extends BrokerFilter {

	private int min;
	private int max;

	/**
	 * @param min 最小类别值 / min category value
	 * @param max 最大类别值 / max category value
	 */
	public BrokerMinMaxFilter(int min, int max) {
		this.min = min * 100000;
		this.max = max * 100000;
	}

	/** 接受 / accept. */
	@Override
	public boolean accept(ItemTemplate template) {
		return template.getTemplateId() >= min && template.getTemplateId() < max;
	}
}
