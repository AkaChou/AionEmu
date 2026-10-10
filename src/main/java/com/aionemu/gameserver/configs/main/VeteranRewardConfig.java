package com.aionemu.gameserver.configs.main;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import com.aionemu.commons.configuration.Property;

import lombok.Getter;

/**
 * 老兵奖励系统相关配置，支持 Spring Boot ConfigurationProperties 与静态门面访问。
 * Veteran reward system related configuration supporting Spring Boot ConfigurationProperties and static facade access.
 */
@Component
@ConfigurationProperties(prefix = "gameserver")
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

	@Getter
	private final VeteranrewardProperties veteranreward = new VeteranrewardProperties();

	@Getter
	public static class VeteranrewardProperties {
		public boolean isEnabled() {
			return VETERANREWARDS_ENABLED;
		}

		public void setEnabled(boolean enabled) {
			VETERANREWARDS_ENABLED = enabled;
		}

		public boolean isLogError() {
			return VETERANREWARDS_ENABLED_ERROR_LOG;
		}

		public void setLogError(boolean logError) {
			VETERANREWARDS_ENABLED_ERROR_LOG = logError;
		}

		public boolean isLogInfo() {
			return VETERANREWARDS_ENABLED_INFO_LOG;
		}

		public void setLogInfo(boolean logInfo) {
			VETERANREWARDS_ENABLED_INFO_LOG = logInfo;
		}
	}
}
