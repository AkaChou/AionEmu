package com.aionemu.gameserver.configs.main;

import com.aionemu.commons.configuration.Property;

/**
 * 老兵奖励系统相关配置。
 * Veteran reward system related configuration.
 */
public class VeteranRewardConfig {
	/**
	 * 是否启用老兵奖励。
	 * Whether veteran rewards are enabled.
	 */
	@Property(key = "gameserver.veteranreward.enabled", defaultValue = "false")
	public static boolean VETERANREWARDS_ENABLED;

	/**
	 * 是否记录老兵奖励错误日志。
	 * Whether veteran reward error logging is enabled.
	 */
	@Property(key = "gameserver.veteranreward.log_error", defaultValue = "true")
	public static boolean VETERANREWARDS_ENABLED_ERROR_LOG;

	/**
	 * 是否记录老兵奖励信息日志。
	 * Whether veteran reward info logging is enabled.
	 */
	@Property(key = "gameserver.veteranreward.log_info", defaultValue = "true")
	public static boolean VETERANREWARDS_ENABLED_INFO_LOG;
}
