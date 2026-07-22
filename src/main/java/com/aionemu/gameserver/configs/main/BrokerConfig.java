package com.aionemu.gameserver.configs.main;

import com.aionemu.commons.configuration.Property;

/**
 * 交易行相关配置。
 * Broker (auction house) related configuration.
 */
public class BrokerConfig {
	/**
	 * 交易行反作弊惩罚类型。
	 * Broker anti-hack punishment type.
	 */
	@Property(key = "gameserver.broker.anti.hack.punishment", defaultValue = "0")
	public static int ANTI_HACK_PUNISHMENT;
	/**
	 * 交易行物品过期时间。
	 * Broker items expire time.
	 */
	@Property(key = "gameserver.broker.items.expire.time", defaultValue = "8")
	public static int ITEMS_EXPIRE_TIME;
}
