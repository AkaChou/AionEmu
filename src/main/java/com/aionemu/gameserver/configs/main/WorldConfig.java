package com.aionemu.gameserver.configs.main;

import com.aionemu.commons.configuration.Property;

/**
 * 世界区域与频道分线相关配置。
 * World region and channel twin related configuration.
 */
public class WorldConfig {
	/**
	 * 世界区域格子大小。
	 * World region grid size.
	 */
	@Property(key = "gameserver.world.region.size", defaultValue = "128")
	public static int WORLD_REGION_SIZE;
	/**
	 * 是否启用活跃区域追踪。
	 * Whether active region tracing is enabled.
	 */
	@Property(key = "gameserver.world.region.active.trace", defaultValue = "true")
	public static boolean WORLD_ACTIVE_TRACE;
	/**
	 * 是否模拟 A-Station 世界行为。
	 * Whether A-Station world behavior is emulated.
	 */
	@Property(key = "gameserver.world.emulate.a.station", defaultValue = "false")
	public static boolean WORLD_EMULATE_A_STATION;
	/**
	 * 常规地图最大频道数。
	 * Maximum twin/channel count for usual maps.
	 */
	@Property(key = "gameserver.world.max.twincount.usual", defaultValue = "1")
	public static int WORLD_MAX_TWINS_USUAL;
	/**
	 * 新手地图最大频道数（-1 表示不限制）。
	 * Maximum twin/channel count for beginner maps (-1 unlimited).
	 */
	@Property(key = "gameserver.world.max.twincount.beginner", defaultValue = "-1")
	public static int WORLD_MAX_TWINS_BEGINNER;
}
