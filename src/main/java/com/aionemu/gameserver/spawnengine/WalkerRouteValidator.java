package com.aionemu.gameserver.spawnengine;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.configs.main.GeoDataConfig;
import com.aionemu.gameserver.lifecycle.GameWorldServices;
import com.aionemu.gameserver.model.templates.walker.RouteStep;
import com.aionemu.gameserver.model.templates.walker.WalkerTemplate;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 行走路线装载校验：对照真端 {@code WayPointInfo::CalcWayPointZPos} 的装载期检查
 * （相邻点 ≤100m、每点必须落在可行走面、相邻点必须可寻路、闭环末点 &lt;1m 去重）。
 * <p>
 * 惰性 per-(worldId, routeId) 一次：首次在该世界启动某条路线巡逻时触发，结果只记一次。
 * 默认 {@code off} 关闭；{@code log} 模式只记录不改数据；{@code enforce}
 * （按世界生成净化副本：截断超长/不可达段、闭环去重）留待后续切片实现。
 * 校验只消费静态数据与只读查询（geo 探测 + PATH 网格直线检查），不改动 {@link WalkerTemplate} 共享数据。
 * <p>
 * Walker route load-time validation, mirroring the retail {@code WayPointInfo::CalcWayPointZPos} checks
 * (adjacent steps &le; 100 m, every step on a walkable surface, adjacent steps pathable, closed-loop
 * de-duplication below 1 m). Lazy once per (worldId, routeId) when a route first starts walking in that
 * world. The default mode is {@code off} (disabled); {@code log} mode only records; {@code enforce}
 * (per-world sanitized copy) is left for a later slice. Validation only reads static data and read-only
 * queries; it never mutates the shared {@link WalkerTemplate}.
 */
@Slf4j
public final class WalkerRouteValidator {

	/**
	 * 真端相邻航点距离上限（米）：超过即截断路线。
	 * Retail max distance between adjacent waypoints (m); longer legs truncate the route.
	 */
	static final float MAX_LEG_DISTANCE = 100f;

	/**
	 * 真端闭环收口阈值（米）：末点距首点小于该值时视作闭环重复。
	 * Retail closed-loop margin (m): a last step this close to the first counts as a closed-loop duplicate.
	 */
	static final float CLOSED_LOOP_MARGIN = 1f;

	/**
	 * 校验采样实例带；与 AIM-009 的运行时采样保持一致（instanceId=1）。
	 * Sampling instance id; kept identical to the AIM-009 runtime sampling (instanceId=1).
	 */
	private static final int SAMPLE_INSTANCE_ID = 1;

	/** 已校验的 (worldId, routeId) 集合。 / Validated (worldId, routeId) pairs. */
	private static final Set<String> CHECKED_ROUTES = ConcurrentHashMap.newKeySet();

	private WalkerRouteValidator() {
	}

	/**
	 * 校验入口：每个 (worldId, routeId) 只执行一次；离线对话、死亡恢复等重复启动直接短路。
	 * Validation entry point: runs once per (worldId, routeId); repeat starts short-circuit.
	 * @param worldId 路线所属世界 / owning world id
	 * @param template 行走模板（只读）/ walker template (read-only)
	 */
	public static void validate(int worldId, WalkerTemplate template) {
		if (template == null || "off".equalsIgnoreCase(GeoDataConfig.GEO_NPC_WALK_ROUTE_VALIDATE)) {
			return;
		}
		String routeId = template.getRouteId();
		if (routeId == null || !CHECKED_ROUTES.add(worldId + "/" + routeId)) {
			return;
		}
		List<RouteStep> route = template.getRouteSteps();
		if (route == null || route.size() < 2) {
			return;
		}
		int legsNeedingPath = 0;
		int problems = 0;
		float[] groundZ = new float[route.size()];
		for (int i = 0; i < route.size(); i++) {
			float probed = probeGroundZ(worldId, route.get(i));
			if (Float.isNaN(probed)) {
				problems++;
				log.warn(I18n.get("log.walker.route.no_ground", routeId, i));
				groundZ[i] = route.get(i).getZ();
			} else {
				groundZ[i] = probed;
			}
		}
		for (int i = 0; i + 1 < route.size(); i++) {
			RouteStep from = route.get(i);
			RouteStep to = route.get(i + 1);
			float legDistance = distance(from, to);
			if (legTooLong(legDistance)) {
				problems++;
				log.warn(I18n.get("log.walker.route.leg_too_long", routeId, i, i + 1, legDistance, MAX_LEG_DISTANCE));
			} else if (!canWalkStraight(worldId, from, to, groundZ[i], groundZ[i + 1])) {
				legsNeedingPath++;
			}
		}
		float closure = distance(route.get(route.size() - 1), route.get(0));
		if (closedLoopDuplicate(closure)) {
			log.info(I18n.get("log.walker.route.closed_loop", routeId, closure));
		}
		if (GeoDataConfig.GEO_NPC_WALK_ROUTE_VALIDATE_SUMMARY_ENABLE && (legsNeedingPath > 0 || problems > 0)) {
			log.info(I18n.get("log.walker.route.summary", routeId, worldId, route.size(), legsNeedingPath, problems));
		}
	}

	/**
	 * 相邻航点是否超过真端上限（米）。
	 * Whether an adjacent-leg distance exceeds the retail limit (m).
	 * @param legDistance 相邻点距离（米）/ adjacent distance in meters
	 * @return 超限返回 true / true when over the limit
	 */
	static boolean legTooLong(float legDistance) {
		return legDistance > MAX_LEG_DISTANCE;
	}

	/**
	 * 末点是否构成闭环重复（非零且小于闭环阈值，米）。
	 * Whether the last step forms a closed-loop duplicate (non-zero and below the margin, in meters).
	 * @param lastToFirst 末点距首点距离（米）/ last-to-first distance in meters
	 * @return 构成重复返回 true / true when it is a duplicate
	 */
	static boolean closedLoopDuplicate(float lastToFirst) {
		return lastToFirst > 0 && lastToFirst < CLOSED_LOOP_MARGIN;
	}

	/**
	 * 两点三维距离。
	 * 3-D distance between two route steps.
	 * @param a 点 A / step A
	 * @param b 点 B / step B
	 * @return 距离（米）/ distance in meters
	 */
	static float distance(RouteStep a, RouteStep b) {
		float dx = a.getX() - b.getX();
		float dy = a.getY() - b.getY();
		float dz = a.getZ() - b.getZ();
		return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
	}

	/**
	 * 该点的地面高度（不兜底的窄带向下探测）；geo 未启用时返回模板 Z（不做地面检查），无地面返回 NaN。
	 * Ground height at the step (non-fallback narrow-band downward probe); returns the template Z when geo
	 * is disabled (no ground check) and NaN when no ground exists.
	 * @param worldId 世界 ID / world id
	 * @param step 路线点 / route step
	 * @return 地面高度或 NaN / ground height or NaN
	 */
	private static float probeGroundZ(int worldId, RouteStep step) {
		if (!GeoDataConfig.GEO_ENABLE) {
			return step.getZ();
		}
		return GameWorldServices.geoService().projectGroundZ(worldId, step.getX(), step.getY(),
				step.getZ(), SAMPLE_INSTANCE_ID);
	}

	/**
	 * 相邻点是否 PATH 网格直线可达（廉价 Bresenham；无 PATH 数据/点不在网格时视为可达）。
	 * 端点 Z 用**地面解算值**（与运行时 refreshWalkerLegPath 同口径）：模板 Z 常年与地面差数米，
	 * 直接用它会让 projectPoint 落在 0.7m 垂直容差之外 → 一律「视为可达」的假阴性
	 * （2026-10-08 实机日志暴露：Verteron 地面路线全部 0 段需要沿 Path）。
	 * Whether the adjacent steps are straight-line reachable on the PATH grid (cheap Bresenham; treated as
	 * reachable when PATH data is absent or a step is off-grid). Endpoint Z uses the resolved ground
	 * height exactly like the runtime refreshWalkerLegPath verdict: stale template Z values sit meters off
	 * the surface and fall outside projectPoint's 0.7 m vertical tolerance, which previously made every
	 * such leg a false-negative (field logs 2026-10-08: all Verteron ground routes reported 0 legs).
	 * @param worldId 世界 ID / world id
	 * @param from 起点 / from step
	 * @param to 终点 / to step
	 * @param fromZ 起点地面高度 / resolved ground height of the from step
	 * @param toZ 终点地面高度 / resolved ground height of the to step
	 * @return 直线可达返回 true / true when a straight line works
	 */
	private static boolean canWalkStraight(int worldId, RouteStep from, RouteStep to, float fromZ, float toZ) {
		return GameWorldServices.pathService().canWalkStraightLine(worldId, from.getX(), from.getY(), fromZ,
				to.getX(), to.getY(), toZ);
	}
}
