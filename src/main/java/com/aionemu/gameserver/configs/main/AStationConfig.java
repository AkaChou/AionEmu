package com.aionemu.gameserver.configs.main;

import com.aionemu.commons.configuration.Property;

/**
 * A-Station（跨服）相关配置。
 * A-Station (cross-server) related configuration.
 *
 * @author Ranastic
 */
public class AStationConfig {
	/**
	 * A-Station 目标服务器 ID。
	 * A-Station target server ID.
	 */
	@Property(key = "a.station.server.id", defaultValue = "2")
	public static int A_STATION_SERVER_ID;
	/**
	 * A-Station 最大角色等级。
	 * Maximum character level for A-Station.
	 */
	@Property(key = "a.station.max.level", defaultValue = "83")
	public static int A_STATION_MAX_LEVEL;
	/**
	 * 是否启用 A-Station。
	 * Whether A-Station is enabled.
	 */
	@Property(key = "a.station.enable", defaultValue = "true")
	public static boolean A_STATION_ENABLE;
}
