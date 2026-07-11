package com.aionemu.gameserver.configs.main;

import com.aionemu.commons.configuration.Property;
import com.aionemu.gameserver.model.account.Account;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData;

/**
 * 玩家与账号缓存相关配置。
 * Player and account cache related configuration.
 *
 * @author Luno
 */
public class CacheConfig {

	/**
	 * 是否使用 SoftCacheMap（否则使用 WeakCacheMap）。
	 * Whether to use SoftCacheMap instead of WeakCacheMap.
	 */
	@Property(key = "gameserver.cache.softcache", defaultValue = "false")
	public static boolean SOFT_CACHE_MAP;

	/**
	 * 是否缓存完整 {@link Player} 对象。
	 * Whether whole {@link Player} objects are cached while memory allows.
	 */
	@Property(key = "gameserver.cache.players", defaultValue = "false")
	public static boolean CACHE_PLAYERS;

	/**
	 * 是否缓存 {@link PlayerCommonData} 对象。
	 * Whether whole {@link PlayerCommonData} objects are cached while memory allows.
	 */
	@Property(key = "gameserver.cache.pcd", defaultValue = "false")
	public static boolean CACHE_COMMONDATA;

	/**
	 * 是否缓存 {@link Account} 对象。
	 * Whether whole {@link Account} objects are cached while memory allows.
	 */
	@Property(key = "gameserver.cache.accounts", defaultValue = "false")
	public static boolean CACHE_ACCOUNTS;
}
