package com.aionemu.gameserver.configs.main;

import com.aionemu.commons.configuration.Property;
import com.aionemu.gameserver.model.account.Account;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 玩家与账号缓存相关配置，支持 Spring Boot ConfigurationProperties 与静态门面访问。
 * Player and account cache related configuration supporting Spring Boot ConfigurationProperties and static facade access.
 *
 * @author Luno
 */
@Component
@ConfigurationProperties(prefix = "gameserver.cache")
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

	public boolean isSoftcache() {
		return SOFT_CACHE_MAP;
	}

	public void setSoftcache(boolean softcache) {
		SOFT_CACHE_MAP = softcache;
	}

	public boolean isPlayers() {
		return CACHE_PLAYERS;
	}

	public void setPlayers(boolean players) {
		CACHE_PLAYERS = players;
	}

	public boolean isPcd() {
		return CACHE_COMMONDATA;
	}

	public void setPcd(boolean pcd) {
		CACHE_COMMONDATA = pcd;
	}

	public boolean isAccounts() {
		return CACHE_ACCOUNTS;
	}

	public void setAccounts(boolean accounts) {
		CACHE_ACCOUNTS = accounts;
	}
}
