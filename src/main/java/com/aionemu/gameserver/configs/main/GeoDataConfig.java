package com.aionemu.gameserver.configs.main;

import com.aionemu.commons.configuration.Property;

/**
 * 地理数据与导航寻路相关配置。
 * Geodata and navigation pathfinding related configuration.
 */
public class GeoDataConfig {

	/**
	 * 是否启用地理数据。
	 * Whether geodata is enabled.
	 */
	@Property(key = "gameserver.geodata.enable", defaultValue = "false")
	public static boolean GEO_ENABLE;

	/**
	 * 是否使用地理数据做可见性（canSee）检测。
	 * Whether canSee checks use geodata.
	 */
	@Property(key = "gameserver.geodata.cansee.enable", defaultValue = "true")
	public static boolean CANSEE_ENABLE;

	/**
	 * 是否使用地理数据处理恐惧技能。
	 * Whether Fear skill uses geodata.
	 */
	@Property(key = "gameserver.geodata.fear.enable", defaultValue = "true")
	public static boolean FEAR_ENABLE;

	/**
	 * 是否在 NPC 移动时做地理检测（防飞行怪）。
	 * Whether geo checks run during NPC movement (prevent flying mobs).
	 */
	@Property(key = "gameserver.geo.npc.move", defaultValue = "false")
	public static boolean GEO_NPC_MOVE;

	/**
	 * 是否启用地理材质技能效果。
	 * Whether geo materials using skills are enabled.
	 */
	@Property(key = "gameserver.geo.materials.enable", defaultValue = "false")
	public static boolean GEO_MATERIALS_ENABLE;

	/**
	 * 是否启用地理护盾。
	 * Whether geo shields are enabled.
	 */
	@Property(key = "gameserver.geo.shields.enable", defaultValue = "false")
	public static boolean GEO_SHIELDS_ENABLE;

	@Property(key = "gameserver.geo.path.enable", defaultValue = "false")
	public static boolean GEO_PATH_ENABLE;

	/** 按 NPC 与玩家距离降低非战斗移动和追击重寻频率。 / Distance-tiered NPC movement and chase repathing. */
	@Property(key = "gameserver.geo.path.distance.tiers.enable", defaultValue = "false")
	public static boolean GEO_PATH_DISTANCE_TIERS_ENABLE;

	@Property(key = "gameserver.geo.path.recovery.enable", defaultValue = "true")
	public static boolean GEO_PATH_RECOVERY_ENABLE;

	/** 长距离地面路径使用 PATH block 分层走廊，并在失败时回退普通 A*。 */
	@Property(key = "gameserver.geo.path.hierarchical.enable", defaultValue = "false")
	public static boolean GEO_PATH_HIERARCHICAL_ENABLE;

	/** 运行期最多前视的 PATH 路点数；0=禁用。 / Runtime PATH waypoint lookahead; 0 = disabled. */
	@Property(key = "gameserver.geo.path.waypoint.lookahead", defaultValue = "3")
	public static int GEO_PATH_WAYPOINT_LOOKAHEAD;

	/** 内存中保留的 PATH 地图数；0=不限制。 / Cached PATH maps; 0 = unlimited. */
	@Property(key = "gameserver.geo.path.cache.size", defaultValue = "32")
	public static int GEO_PATH_CACHE_SIZE;

	@Property(key = "gameserver.geo.path.max.nodes", defaultValue = "50000")
	public static int GEO_PATH_MAX_NODES;

	@Property(key = "gameserver.geo.path.timeout.ms", defaultValue = "250")
	public static int GEO_PATH_TIMEOUT_MS;

	@Property(key = "gameserver.geo.path.spatial.step", defaultValue = "2")
	public static float GEO_PATH_SPATIAL_STEP;

	/** 寻路 worker 数；0 表示按 CPU 自动（最多 8）。 / Path worker count; 0 = auto by CPU (cap 8). */
	@Property(key = "gameserver.geo.path.workers", defaultValue = "0")
	public static int GEO_PATH_WORKERS;

	/** 异步寻路队列容量。 / Async path queue capacity. */
	@Property(key = "gameserver.geo.path.queue.capacity", defaultValue = "256")
	public static int GEO_PATH_QUEUE_CAPACITY;

}
