package com.aionemu.gameserver.configs.main;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import com.aionemu.commons.configuration.Property;

import lombok.Getter;

/**
 * 传送时间配置，支持 Spring Boot ConfigurationProperties 与静态门面访问。
 * Transport timing configuration supporting Spring Boot ConfigurationProperties and static facade access.
 */
@Component
@ConfigurationProperties(prefix = "gameserver")
public class TransportConfig {

	/** 据点传送读条时间（秒）。 / Hotspot teleport cast time in seconds. */
	@Property(key = "gameserver.transport.hotspot.cast_time_seconds", defaultValue = "2")
	public static int HOTSPOT_CAST_TIME_SECONDS = 2;

	/** 据点传送冷却时间（秒）。 / Hotspot teleport cooldown in seconds. */
	@Property(key = "gameserver.transport.hotspot.cooldown_seconds", defaultValue = "5")
	public static int HOTSPOT_COOLDOWN_SECONDS = 5;

	@Getter
	private final TransportProperties transport = new TransportProperties();

	/** 校验传送时间配置。 / Validates transport timing configuration. */
	public static void refresh() {
		if (HOTSPOT_CAST_TIME_SECONDS < 0 || HOTSPOT_COOLDOWN_SECONDS < 0) {
			throw new IllegalArgumentException("Transport times must not be negative");
		}
	}

	@Getter
	public static class TransportProperties {
		private final HotspotProperties hotspot = new HotspotProperties();

		@Getter
		public static class HotspotProperties {
			public int getCastTimeSeconds() {
				return HOTSPOT_CAST_TIME_SECONDS;
			}

			public void setCastTimeSeconds(int castTimeSeconds) {
				HOTSPOT_CAST_TIME_SECONDS = castTimeSeconds;
			}

			public int getCooldownSeconds() {
				return HOTSPOT_COOLDOWN_SECONDS;
			}

			public void setCooldownSeconds(int cooldownSeconds) {
				HOTSPOT_COOLDOWN_SECONDS = cooldownSeconds;
			}
		}
	}
}
