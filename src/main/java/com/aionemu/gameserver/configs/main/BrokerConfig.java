package com.aionemu.gameserver.configs.main;

import com.aionemu.commons.configuration.Property;

/**
 * 交易行相关配置。
 * Broker (auction house) related configuration.
 */
public class BrokerConfig {
	/**
	 * 交易行存盘管理器执行间隔。
	 * Broker save manager interval.
	 */
	@Property(key = "gameserver.broker.save.manager.interval", defaultValue = "6")
	public static int SAVE_MANAGER_INTERVAL;
	/**
	 * 过期物品检查间隔。
	 * Interval for checking expired broker items.
	 */
	@Property(key = "gameserver.broker.time.check.expired.items.interval", defaultValue = "60")
	public static int CHECK_EXPIRED_ITEMS_INTERVAL;
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
