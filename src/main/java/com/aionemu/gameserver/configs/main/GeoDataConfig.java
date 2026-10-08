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

	/**
	 * 是否启用地理寻路（PATH block 导航）。
	 * Whether geodata pathfinding (PATH block navigation) is enabled.
	 */
	@Property(key = "gameserver.geo.path.enable", defaultValue = "false")
	public static boolean GEO_PATH_ENABLE;

	/** 按 NPC 与玩家距离降低非战斗移动和追击重寻频率。 / Distance-tiered NPC movement and chase repathing. */
	@Property(key = "gameserver.geo.path.distance.tiers.enable", defaultValue = "false")
	public static boolean GEO_PATH_DISTANCE_TIERS_ENABLE;

	/**
	 * 是否启用寻路失败恢复（回退方案）。
	 * Whether pathfinding recovery fallback is enabled.
	 */
	@Property(key = "gameserver.geo.path.recovery.enable", defaultValue = "true")
	public static boolean GEO_PATH_RECOVERY_ENABLE;

	/** 长距离地面路径使用 PATH block 分层走廊，并在失败时回退普通 A*。 / Long-distance ground paths use layered PATH-block corridors, falling back to plain A* on failure. */
	@Property(key = "gameserver.geo.path.hierarchical.enable", defaultValue = "false")
	public static boolean GEO_PATH_HIERARCHICAL_ENABLE;

	/** 运行期最多前视的 PATH 路点数；0=禁用。 / Runtime PATH waypoint lookahead; 0 = disabled. */
	@Property(key = "gameserver.geo.path.waypoint.lookahead", defaultValue = "3")
	public static int GEO_PATH_WAYPOINT_LOOKAHEAD;

	/** 内存中保留的 PATH 地图数；0=不限制。 / Cached PATH maps; 0 = unlimited. */
	@Property(key = "gameserver.geo.path.cache.size", defaultValue = "32")
	public static int GEO_PATH_CACHE_SIZE;

	/**
	 * 单次寻路的最大节点数。
	 * Maximum node count per pathfinding request.
	 */
	@Property(key = "gameserver.geo.path.max.nodes", defaultValue = "50000")
	public static int GEO_PATH_MAX_NODES;

	/**
	 * 寻路超时时间（毫秒）。
	 * Pathfinding timeout in milliseconds.
	 */
	@Property(key = "gameserver.geo.path.timeout.ms", defaultValue = "250")
	public static int GEO_PATH_TIMEOUT_MS;

	/**
	 * 寻路空间步长。
	 * Pathfinding spatial step.
	 */
	@Property(key = "gameserver.geo.path.spatial.step", defaultValue = "2")
	public static float GEO_PATH_SPATIAL_STEP;

	/** 寻路 worker 数；0 表示按 CPU 自动（最多 8）。 / Path worker count; 0 = auto by CPU (cap 8). */
	@Property(key = "gameserver.geo.path.workers", defaultValue = "0")
	public static int GEO_PATH_WORKERS;

	/** 异步寻路队列容量。 / Async path queue capacity. */
	@Property(key = "gameserver.geo.path.queue.capacity", defaultValue = "256")
	public static int GEO_PATH_QUEUE_CAPACITY;

	/**
	 * 行走 NPC（WALK_PATH）沿 Path 推进模式：off=关闭（回滚态）；blocked=仅当航段直线不可达时沿 Path（默认）；
	 * always=所有航段沿 Path（实机 A/B 用）。真端语义：航点之间用 Path 连通图连接，不是直线。
	 * Walker (WALK_PATH) along-Path mode: off = disabled (rollback); blocked = only legs whose straight line is
	 * unreachable (default); always = every leg follows the Path (for live A/B). Retail legs follow the Path
	 * connectivity graph between waypoints, not a straight line.
	 */
	@Property(key = "gameserver.geo.npc.walk.path.mode", defaultValue = "off")
	public static String GEO_NPC_WALK_PATH_MODE;

	/**
	 * 行走 NPC 每步碰撞/步高抬升解算开关（真端：线段碰撞查询 + 按步高抬升 + 向下打地面）。
	 * Per-step collision / step-height lift resolution for walkers (retail: segment collision query +
	 * step-height lift + downward ground probe).
	 */
	@Property(key = "gameserver.geo.npc.walk.collision.enable", defaultValue = "false")
	public static boolean GEO_NPC_WALK_COLLISION_ENABLE;

	/**
	 * 行走路线装载校验模式：off=关闭；log=只记录不改数据（默认）；enforce=按世界生成净化副本
	 * （截断超长/LoS 失败段、闭环去重）。对照真端 WayPointInfo::CalcWayPointZPos 的装载期校验。
	 * Walker route validation mode: off; log = record only (default); enforce = per-world sanitized copy
	 * (truncate over-long / LoS-failed legs, de-duplicate closed loops). Mirrors the retail load-time
	 * checks in WayPointInfo::CalcWayPointZPos.
	 */
	@Property(key = "gameserver.geo.npc.walk.route.validate", defaultValue = "log")
	public static String GEO_NPC_WALK_ROUTE_VALIDATE;

}
