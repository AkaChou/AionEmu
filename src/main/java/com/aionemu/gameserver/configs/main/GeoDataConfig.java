package com.aionemu.gameserver.configs.main;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import com.aionemu.commons.configuration.Property;

import lombok.Getter;

/**
 * 地理数据与导航寻路相关配置，支持 Spring Boot ConfigurationProperties 与静态门面访问。
 * Geodata and navigation pathfinding related configuration supporting Spring Boot ConfigurationProperties and static facade access.
 */
@Component
@ConfigurationProperties(prefix = "gameserver")
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
	 * always=所有航段沿 Path（实机 A/B 用）。原版语义：航点之间用 Path 连通图连接，不是直线。
	 * Walker (WALK_PATH) along-Path mode: off = disabled (rollback); blocked = only legs whose straight line is
	 * unreachable (default); always = every leg follows the Path (for live A/B). Retail legs follow the Path
	 * connectivity graph between waypoints, not a straight line.
	 */
	@Property(key = "gameserver.geo.npc.walk.path.mode", defaultValue = "off")
	public static String GEO_NPC_WALK_PATH_MODE;

	/**
	 * 行走 NPC 每步碰撞/步高抬升解算开关（原版：线段碰撞查询 + 按步高抬升 + 向下打地面）。
	 * Per-step collision / step-height lift resolution for walkers (retail: segment collision query +
	 * step-height lift + downward ground probe).
	 */
	@Property(key = "gameserver.geo.npc.walk.collision.enable", defaultValue = "false")
	public static boolean GEO_NPC_WALK_COLLISION_ENABLE;

	/**
	 * 行走路线装载校验模式：off=关闭（默认）；log=只记录不改数据；enforce=按世界生成净化副本
	 * （截断超长/LoS 失败段、闭环去重）。对照原版 WayPointInfo::CalcWayPointZPos 的装载期校验。
	 * Walker route validation mode: off = disabled (default); log = record only; enforce = per-world
	 * sanitized copy (truncate over-long / LoS-failed legs, de-duplicate closed loops). Mirrors the retail
	 * load-time checks in WayPointInfo::CalcWayPointZPos.
	 */
	@Property(key = "gameserver.geo.npc.walk.route.validate", defaultValue = "off")
	public static String GEO_NPC_WALK_ROUTE_VALIDATE;

	/**
	 * 行走路线校验汇总日志开关：开启时按 (世界, 路线) 打印一次统计汇总（步数、需要沿 Path 的段数、
	 * 问题数），默认关闭。问题本身（不在可行走面、段超长）仍逐条 warn，不受本开关影响。
	 * Walker route validation summary log: when enabled, prints one per-(world, route) statistics line
	 * (steps, legs needing the Path, problems); disabled by default. Individual problems (no walkable
	 * surface, over-long leg) stay per-item warnings regardless of this switch.
	 */
	@Property(key = "gameserver.geo.npc.walk.route.validate.summary.enable", defaultValue = "false")
	public static boolean GEO_NPC_WALK_ROUTE_VALIDATE_SUMMARY_ENABLE;

	/** 地理数据绑定树 / geodata binding tree. */
	@Getter
	private final GeodataProperties geodata = new GeodataProperties();
	/** 地理域绑定树 / geo domain binding tree. */
	@Getter
	private final GeoProperties geo = new GeoProperties();

	/**
	 * 地理数据节点：绑定 gameserver.geodata.*。
	 * Geodata node binding gameserver.geodata.*.
	 */
	@Getter
	public static class GeodataProperties {
		private final CanseeProperties cansee = new CanseeProperties();
		private final FearProperties fear = new FearProperties();

		public boolean isEnable() {
			return GEO_ENABLE;
		}

		public void setEnable(boolean enable) {
			GEO_ENABLE = enable;
		}
	}

	/** 可见性检测节点 / CanSee-check node. */
	public static class CanseeProperties {
		public boolean isEnable() {
			return CANSEE_ENABLE;
		}

		public void setEnable(boolean enable) {
			CANSEE_ENABLE = enable;
		}
	}

	/** 恐惧技能节点 / Fear-skill node. */
	public static class FearProperties {
		public boolean isEnable() {
			return FEAR_ENABLE;
		}

		public void setEnable(boolean enable) {
			FEAR_ENABLE = enable;
		}
	}

	/**
	 * 地理域节点：绑定 gameserver.geo.*。
	 * Geo domain node binding gameserver.geo.*.
	 */
	@Getter
	public static class GeoProperties {
		private final MaterialsProperties materials = new MaterialsProperties();
		private final ShieldsProperties shields = new ShieldsProperties();
		private final PathProperties path = new PathProperties();
		private final NpcProperties npc = new NpcProperties();
	}

	/** 地理材质节点 / Geo-materials node. */
	public static class MaterialsProperties {
		public boolean isEnable() {
			return GEO_MATERIALS_ENABLE;
		}

		public void setEnable(boolean enable) {
			GEO_MATERIALS_ENABLE = enable;
		}
	}

	/** 地理护盾节点 / Geo-shields node. */
	public static class ShieldsProperties {
		public boolean isEnable() {
			return GEO_SHIELDS_ENABLE;
		}

		public void setEnable(boolean enable) {
			GEO_SHIELDS_ENABLE = enable;
		}
	}

	/**
	 * 寻路节点：绑定 gameserver.geo.path.*。
	 * Pathfinding node binding gameserver.geo.path.*.
	 */
	@Getter
	public static class PathProperties {
		private final DistanceProperties distance = new DistanceProperties();
		private final RecoveryProperties recovery = new RecoveryProperties();
		private final HierarchicalProperties hierarchical = new HierarchicalProperties();
		private final WaypointProperties waypoint = new WaypointProperties();
		private final CacheProperties cache = new CacheProperties();
		private final MaxProperties max = new MaxProperties();
		private final TimeoutProperties timeout = new TimeoutProperties();
		private final SpatialProperties spatial = new SpatialProperties();
		private final QueueProperties queue = new QueueProperties();

		public boolean isEnable() {
			return GEO_PATH_ENABLE;
		}

		public void setEnable(boolean enable) {
			GEO_PATH_ENABLE = enable;
		}

		public int getWorkers() {
			return GEO_PATH_WORKERS;
		}

		public void setWorkers(int workers) {
			GEO_PATH_WORKERS = workers;
		}
	}

	/** 距离分层父节点 / Distance-tier parent node. */
	@Getter
	public static class DistanceProperties {
		private final TiersProperties tiers = new TiersProperties();
	}

	/** 距离分层节点 / Distance-tier node. */
	public static class TiersProperties {
		public boolean isEnable() {
			return GEO_PATH_DISTANCE_TIERS_ENABLE;
		}

		public void setEnable(boolean enable) {
			GEO_PATH_DISTANCE_TIERS_ENABLE = enable;
		}
	}

	/** 寻路恢复节点 / Path-recovery node. */
	public static class RecoveryProperties {
		public boolean isEnable() {
			return GEO_PATH_RECOVERY_ENABLE;
		}

		public void setEnable(boolean enable) {
			GEO_PATH_RECOVERY_ENABLE = enable;
		}
	}

	/** 分层寻路节点 / Hierarchical-path node. */
	public static class HierarchicalProperties {
		public boolean isEnable() {
			return GEO_PATH_HIERARCHICAL_ENABLE;
		}

		public void setEnable(boolean enable) {
			GEO_PATH_HIERARCHICAL_ENABLE = enable;
		}
	}

	/** 路点父节点 / Waypoint parent node. */
	@Getter
	public static class WaypointProperties {
		public int getLookahead() {
			return GEO_PATH_WAYPOINT_LOOKAHEAD;
		}

		public void setLookahead(int lookahead) {
			GEO_PATH_WAYPOINT_LOOKAHEAD = lookahead;
		}
	}

	/** 缓存父节点 / Cache parent node. */
	@Getter
	public static class CacheProperties {
		public int getSize() {
			return GEO_PATH_CACHE_SIZE;
		}

		public void setSize(int size) {
			GEO_PATH_CACHE_SIZE = size;
		}
	}

	/** 节点上限父节点 / Max-nodes parent node. */
	@Getter
	public static class MaxProperties {
		public int getNodes() {
			return GEO_PATH_MAX_NODES;
		}

		public void setNodes(int nodes) {
			GEO_PATH_MAX_NODES = nodes;
		}
	}

	/** 超时父节点 / Timeout parent node. */
	@Getter
	public static class TimeoutProperties {
		public int getMs() {
			return GEO_PATH_TIMEOUT_MS;
		}

		public void setMs(int ms) {
			GEO_PATH_TIMEOUT_MS = ms;
		}
	}

	/** 空间步长父节点 / Spatial-step parent node. */
	@Getter
	public static class SpatialProperties {
		public float getStep() {
			return GEO_PATH_SPATIAL_STEP;
		}

		public void setStep(float step) {
			GEO_PATH_SPATIAL_STEP = step;
		}
	}

	/** 队列父节点 / Queue parent node. */
	@Getter
	public static class QueueProperties {
		public int getCapacity() {
			return GEO_PATH_QUEUE_CAPACITY;
		}

		public void setCapacity(int capacity) {
			GEO_PATH_QUEUE_CAPACITY = capacity;
		}
	}

	/**
	 * NPC 节点：绑定 gameserver.geo.npc.*。
	 * NPC node binding gameserver.geo.npc.*.
	 */
	@Getter
	public static class NpcProperties {
		private final WalkProperties walk = new WalkProperties();

		public boolean isMove() {
			return GEO_NPC_MOVE;
		}

		public void setMove(boolean move) {
			GEO_NPC_MOVE = move;
		}
	}

	/**
	 * 行走节点：绑定 gameserver.geo.npc.walk.*。
	 * Walk node binding gameserver.geo.npc.walk.*.
	 */
	@Getter
	public static class WalkProperties {
		private final WalkPathProperties path = new WalkPathProperties();
		private final WalkCollisionProperties collision = new WalkCollisionProperties();
		private final WalkRouteProperties route = new WalkRouteProperties();
	}

	/**
	 * 行走路径父节点：对应配置元素 gameserver.geo.npc.walk.path。
	 * Walk-path parent node matching the gameserver.geo.npc.walk.path element.
	 */
	@Getter
	public static class WalkPathProperties {
		public String getMode() {
			return GEO_NPC_WALK_PATH_MODE;
		}

		public void setMode(String mode) {
			GEO_NPC_WALK_PATH_MODE = mode;
		}
	}

	/**
	 * 行走碰撞父节点：对应配置元素 gameserver.geo.npc.walk.collision。
	 * Walk-collision parent node matching the gameserver.geo.npc.walk.collision element.
	 */
	@Getter
	public static class WalkCollisionProperties {
		public boolean isEnable() {
			return GEO_NPC_WALK_COLLISION_ENABLE;
		}

		public void setEnable(boolean enable) {
			GEO_NPC_WALK_COLLISION_ENABLE = enable;
		}
	}

	/**
	 * 行走路线父节点：对应配置元素 gameserver.geo.npc.walk.route。
	 * Walk-route parent node matching the gameserver.geo.npc.walk.route element.
	 */
	@Getter
	public static class WalkRouteProperties {
		private final ValidateProperties validate = new ValidateProperties();

		/** 兼容裸标量 gameserver.geo.npc.walk.route.validate 经 valueOf 转换后的赋值（no-op）。 / No-op setter for the valueOf-converted legacy scalar. */
		public void setValidate(ValidateProperties validate) {
		}
	}

	/**
	 * 行走路线校验节点：绑定 gameserver.geo.npc.walk.route.validate.*，
	 * 并兼容旧版裸标量 gameserver.geo.npc.walk.route.validate=off。
	 * Walk-route validation node binding gameserver.geo.npc.walk.route.validate.*, also accepting the
	 * legacy bare scalar gameserver.geo.npc.walk.route.validate=off.
	 */
	@Getter
	public static class ValidateProperties {
		private final SummaryProperties summary = new SummaryProperties();

		/**
		 * 兼容旧版裸标量 gameserver.geo.npc.walk.route.validate=off 的绑定入口。
		 * Binding entry for the legacy bare scalar gameserver.geo.npc.walk.route.validate=off.
		 */
		public static ValidateProperties valueOf(String value) {
			try {
				GEO_NPC_WALK_ROUTE_VALIDATE = value;
			} catch (RuntimeException ignored) {
			}
			return new ValidateProperties();
		}

		public String getValue() {
			return GEO_NPC_WALK_ROUTE_VALIDATE;
		}

		public void setValue(String value) {
			GEO_NPC_WALK_ROUTE_VALIDATE = value;
		}
	}

	/**
	 * 校验汇总父节点：对应配置元素 gameserver.geo.npc.walk.route.validate.summary。
	 * Validation-summary parent node matching the gameserver.geo.npc.walk.route.validate.summary element.
	 */
	@Getter
	public static class SummaryProperties {
		public boolean isEnable() {
			return GEO_NPC_WALK_ROUTE_VALIDATE_SUMMARY_ENABLE;
		}

		public void setEnable(boolean enable) {
			GEO_NPC_WALK_ROUTE_VALIDATE_SUMMARY_ENABLE = enable;
		}
	}
}
