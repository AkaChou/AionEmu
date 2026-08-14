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
	 * @param mask 类别掩码（模板 ID / 100000） / category mask (template id / 100000)
	 * @param playerClass 限制的玩家职业 / restricted player class
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
