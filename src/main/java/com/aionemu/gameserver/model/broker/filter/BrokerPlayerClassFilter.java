package com.aionemu.gameserver.model.broker.filter;

import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.templates.item.ItemTemplate;

/**
 * 经纪行玩家职业 Filter 模型。
 * Broker Player Class Filter model.
 *
 * @author ATracer
 */
public class BrokerPlayerClassFilter extends BrokerFilter {

	private PlayerClass playerClass;

	/**
	 * @param playerClass 限制的玩家职业 / restricted player class
	 */
	public BrokerPlayerClassFilter(PlayerClass playerClass) {
		super();
		this.playerClass = playerClass;
	}

	/** 接受 / accept. */
	@Override
	public boolean accept(ItemTemplate template) {
		return template.isClassSpecific(playerClass);
	}
}
