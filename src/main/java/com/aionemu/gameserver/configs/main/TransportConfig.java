package com.aionemu.gameserver.configs.main;

import com.aionemu.commons.configuration.Property;

/**
 * 传送时间配置。
 * Transport timing configuration.
 */
public class TransportConfig {

	/** 据点传送读条时间（秒）。 / Hotspot teleport cast time in seconds. */
	@Property(key = "gameserver.transport.hotspot.cast_time_seconds", defaultValue = "2")
	public static int HOTSPOT_CAST_TIME_SECONDS = 2;

	/** 据点传送冷却时间（秒）。 / Hotspot teleport cooldown in seconds. */
	@Property(key = "gameserver.transport.hotspot.cooldown_seconds", defaultValue = "5")
	public static int HOTSPOT_COOLDOWN_SECONDS = 5;

	/** 校验传送时间配置。 / Validates transport timing configuration. */
	public static void refresh() {
		if (HOTSPOT_CAST_TIME_SECONDS < 0 || HOTSPOT_COOLDOWN_SECONDS < 0) {
			throw new IllegalArgumentException("Transport times must not be negative");
		}
	}
}
