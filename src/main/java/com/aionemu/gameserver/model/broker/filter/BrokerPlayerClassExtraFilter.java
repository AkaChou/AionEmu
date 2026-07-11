package com.aionemu.gameserver.model.broker.filter;

import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.templates.item.ItemTemplate;

/**
 * 经纪行玩家职业 ExtraFilter 模型。
 * Broker Player Class Extra Filter model.
 *
 * @author ATracer
 */
public class BrokerPlayerClassExtraFilter extends BrokerPlayerClassFilter {

	private int mask;

	/**
	 * @param mask
	 */
	public BrokerPlayerClassExtraFilter(int mask, PlayerClass playerClass) {
		super(playerClass);
		this.mask = mask;
	}

	/** 接受 / accept. */
	@Override
	public boolean accept(ItemTemplate template) {
		return super.accept(template) && mask == template.getTemplateId() / 100000;
	}
}
