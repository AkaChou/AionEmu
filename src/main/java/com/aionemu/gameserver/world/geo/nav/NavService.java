package com.aionemu.gameserver.world.geo.nav;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import java.util.ArrayList;

import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.gameserver.configs.main.GeoDataConfig;
import com.aionemu.gameserver.geoEngine.bounding.BoundingBox;
import com.aionemu.gameserver.geoEngine.collision.CollisionResults;
import com.aionemu.gameserver.geoEngine.math.Ray;
import com.aionemu.gameserver.geoEngine.math.Vector3f;
import com.aionemu.gameserver.geoEngine.models.GeoMap;
import com.aionemu.gameserver.geoEngine.scene.NavGeometry;
import com.aionemu.gameserver.geoEngine.scene.Spatial;
import com.aionemu.gameserver.lifecycle.GameWorldServices;
import com.aionemu.gameserver.model.gameobjects.Creature;

/**
 * 导航服务入口，类似 {@link com.aionemu.gameserver.world.geo.GeoService GeoService}，提供寻路与拉取可行性查询。
 * Entry point for navigational queries (pathfinding), similar to
 * {@link com.aionemu.gameserver.world.geo.GeoService GeoService}.
 *
 * @author Yon (Aion Reconstruction Project)
 */
@Slf4j
public final class NavService {

	/** 可选 Spring 单例提供者 / Optional Spring singleton provider */
	private static volatile ObjectProvider<NavService> instanceProvider;
	/** 导航数据源。 / Navigation data source. */
	private final NavData navData = GameWorldServices.navData();

	/**
	 * 默认构造。
	 * Default constructor.
	 */
	public NavService() {};

	/**
	 * 初始化导航：启用时扫描并索引 .nav 文件。
	 * Initializes navigation: when enabled, scans and indexes .nav files.
	 */
	public void initializeNav() {
		if (GeoDataConfig.GEO_NAV_ENABLE) {
			log.info(I18n.get("log.703d1b3e51b5"));
			if (!navData.isLoaded()) {
				navData.loadNavMaps();
			} else {
				log.warn(I18n.get("log.028418e35732"));
			}
		} else {
			log.info(I18n.get("log.cd503ddf46e3"));
		}
	}

	/**
	 * 检查实体是否可通过连续导航网格直线拉取目标。
	 * 目标飞行时立即返回 true；禁用导航时立即返回 true。
	 * 副效应：拉取者若远高于导航网格也可能返回 false（与官方行为一致）。
	 * Checks whether one creature can pull another by continuous straight-line navmesh path.
	 * Returns true immediately if the target is flying, or if nav is disabled.
	 * Side-effect: flying too far above the navmesh may also return false (matches retail).
	 * Assumes the entities can see each other.
	 *
	 * @param creature 尝试拉取的实体 / entity attempting the pull
	 * @param target 被拉取的目标 / target being pulled
	 * @return 可拉取则为 true / true if the target can be pulled
	 */
	public boolean canPullTarget(Creature creature, Creature target) {
		if (!GeoDataConfig.GEO_NAV_ENABLE || !GeoDataConfig.GEO_NAV_PULL_ENABLE) return true;
		if (target.isFlying()) return true;
		float x1 = creature.getX(), y1 = creature.getY(), z1 = creature.getZ();
		NavGeometry tile1 = getNavTile(creature.getWorldId(), x1, y1, z1);
		if (tile1 == null) {
			tile1 = getNavTileWithBox(creature.getWorldId(), x1, y1, z1);
			if (tile1 == null) return false;
		}
		float x2 = target.getX(), y2 = target.getY(), z2 = target.getZ();
		NavGeometry tile2 = getNavTile(target.getWorldId(), x2, y2, z2);
		if (tile2 == null) {
			tile2 = getNavTileWithBox(target.getWorldId(), x2, y2, z2);
			if (tile2 == null) return false;
		}
		// 因路径需从目标存在而翻转（实际影响不大）。 / They're flipped around because the path needs to exist from the target (though it doesn't actually matter)
		float[][] path = attemptStraightLinePath(tile2, tile1, x2, y2, z2, x1, y1, z1);
		if (path != null && path.length == 1) return true;
		return false;
	}

	/**
	 * 尝试在两三角形间走漏斗直达路径，无需完整 A*。
	 * Attempts a funnel straight-line path between two tiles without full A*.
	 *
	 * @param tile1 起点三角形 / start tile
	 * @param tile2 终点三角形 / end tile
	 * @param x1 起点 X / start x
	 * @param y1 起点 Y / start y
	 * @param z1 起点 Z / start z
	 * @param x2 终点 X / end x
	 * @param y2 终点 Y / end y
	 * @param z2 终点 Z / end z
	 * @return 路径点数组，失败则为 null / pathway points, or null on failure
	 */
	private float[][] attemptStraightLinePath(NavGeometry tile1, NavGeometry tile2, float x1, float y1, float z1, float x2, float y2, float z2) {
		// 基础检查 / basic checks
		assert tile1 != null:"NavService#validateStraightLinePath() tile1 is null!";
		if (tile2 == null) return null;
		if (tile1 == tile2) return new float[][] {{x2, y2, z2}};
		// 尝试在无路径时漏斗到目标 / Attempt to funnel to the target without a path
		float[] targetDir = new float[] {x2 - x1, y2 - y1};
		NavGeometry last = tile1;
		NavGeometry current;
		if (last.isFunnelTowardsEdge((byte) 1, targetDir, x1, y1)) {
			current = last.getEdge1();
		} else if (last.isFunnelTowardsEdge((byte) 2, targetDir, x1, y1)) {
			current = last.getEdge2();
		} else if (last.isFunnelTowardsEdge((byte) 3, targetDir, x1, y1)) {
			current = last.getEdge3();
		} else {
			return null;
		}
		int triCount = 0;
		while (triCount < GeoDataConfig.GEO_NAV_MAX_NODES && current != tile2 && current != null) {
			triCount++;
			switch (current.getEdgeMatching(last)) {
				case 0:
					return null;
				case 1:
					// 检查边 2 与 3 / check edge 2 and 3
					if (current.isFunnelTowardsEdge((byte) 2, targetDir, x1, y1)) {
						last = current;
						current = current.getEdge2();
						continue;
					}
					if (current.isFunnelTowardsEdge((byte) 3, targetDir, x1, y1)) {
						last = current;
						current = current.getEdge3();
						continue;
					}
					break;
				case 2:
					// 检查边 1 与 3 / check edge 1 and 3
					if (current.isFunnelTowardsEdge((byte) 1, targetDir, x1, y1)) {
						last = current;
						current = current.getEdge1();
						continue;
					}
					if (current.isFunnelTowardsEdge((byte) 3, targetDir, x1, y1)) {
						last = current;
						current = current.getEdge3();
						continue;
					}
					break;
				case 3:
					// 检查边 1 与 2 / check edge 1 and 2
					if (current.isFunnelTowardsEdge((byte) 1, targetDir, x1, y1)) {
						last = current;
						current = current.getEdge1();
						continue;
					}
					if (current.isFunnelTowardsEdge((byte) 2, targetDir, x1, y1)) {
						last = current;
						current = current.getEdge2();
						continue;
					}
					break;
			}
			return null;
		}
		if (current == tile2) {
			return funnelPathway(new NavPathway[] {new NavPathway(current, current.getEdgeMatching(last))}, true, x1, y1, z1, x2, y2, z2);
		}
		return null;
	}

	/**
	 * 为生物寻路至另一生物的位置。
	 * Navigates a creature toward another creature's position.
	 *
	 * path owner
	 * target creature
	 * @return 路径点序列，失败则为 null / pathway points, or null on failure
	 */
	public float[][] navigateToTarget(Creature pathOwner, Creature target) {
		// 基础检查 / basic checks
		if (pathOwner == null) return null;
		if (pathOwner.getLifeStats().isAlreadyDead()) return null;
		if (target == null) return null;
//		if (target.getLifeStats().isAlreadyDead()) return null;
		if (pathOwner.getWorldId() != target.getWorldId()) return null;

		int worldId = pathOwner.getWorldId();
		float x1 = pathOwner.getX(), y1 = pathOwner.getY(), z1 = pathOwner.getZ();
		float x2 = target.getX(), y2 = target.getY(), z2 = target.getZ();
		// 待办：对生物使用缓存瓦片 / TO-DO: Use Cached Tile for Creature
		return navigateFromLocationToLocation(worldId, null, null, x1, y1, z1, x2, y2, z2);
	}

	/**
	 * 为生物寻路至指定坐标。
	 * Navigates a creature toward the given coordinates.
	 *
	 * path owner
	 *
	 * @param x 目标 X / target x
	 * @param y 目标 Y / target y
	 * @param z 目标 Z / target z
	 * @param z
	 * @return 路径点序列，失败则为 null / pathway points, or null on failure
	 */
	public float[][] navigateToLocation(Creature pathOwner, float x, float y, float z) {
		// 基础检查 / basic checks
		if (pathOwner == null) return null;
		if (pathOwner.getLifeStats().isAlreadyDead()) return null;
		int worldId = pathOwner.getWorldId();
		float x1 = pathOwner.getX(), y1 = pathOwner.getY(), z1 = pathOwner.getZ();
		// 待办：对生物使用缓存瓦片 / TO-DO: Use Cached Tile for Creature
		return navigateFromLocationToLocation(worldId, null, null, x1, y1, z1, x, y, z);
	}

	/**
	 * 在世界内从一点寻路到另一点；必要时回退到盒体采样与 A*。
	 * Navigates from one point to another within a world; falls back to box sampling and A* as needed.
	 *
	 * 世界 ID / world id
	 *
	 * @param tile 起点三角形（可为 null） / start tile (nullable)
	 * @param tile2 终点三角形（可为 null） / end tile (nullable)
	 * @param x1 起点 X / start x
	 * @param y1 起点 Y / start y
	 * @param z1 起点 Z / start z
	 * @param x2 终点 X / end x
	 * @param y2 终点 Y / end y
	 * @param z2 终点 Z / end z
	 * @param z2
	 * @return 路径点序列，失败则为 null / pathway points, or null on failure
	 */
	private float[][] navigateFromLocationToLocation(int worldId, NavGeometry tile, NavGeometry tile2, float x1, float y1, float z1, float x2, float y2, float z2) {
		boolean boxed = false;
		if (tile == null) {
			tile = getNavTile(worldId, x1, y1, z1);
		}
		if (tile == null) {
			tile = getNavTileWithBox(worldId, x1, y1, z1);
			if (tile == null) return null;
			boxed = true;
		}
		if (tile2 == null) {
			tile2 = getNavTile(worldId, x2, y2, z2);
		}
		if (tile2 == null) {
			tile2 = getNavTileWithBox(worldId, x2, y2, z2);
		}
		if (tile == tile2) {
			return new float[][] {{x2, y2, z2}};
		}
		if (boxed) {
			float[] p = tile.getClosestPoint(x1, y1, z1);
			float[][] pathFromP = attemptStraightLinePath(tile, tile2, x1, y1, z1, x2, y2, z2);
			if (pathFromP == null) {
				NavHelper helper = new NavHelper(tile, tile2, p[0], p[1], p[2], x2, y2, z2);
				NavPathway[] pathway = helper.createPathway();
				helper.destroy();
				pathFromP = funnelPathway(pathway, tile2 != null, p[0], p[1], p[2], x2, y2, z2);
			}
			float[][] ret = new float[pathFromP.length + 1][];
			ret[0] = p;
			System.arraycopy(pathFromP, 0, ret, 1, pathFromP.length);
			return ret;
		}
		float[][] straightLinePath = attemptStraightLinePath(tile, tile2, x1, y1, z1, x2, y2, z2);
		if (straightLinePath != null) {
			return straightLinePath;
		}
		NavHelper helper = new NavHelper(tile, tile2, x1, y1, z1, x2, y2, z2);
		NavPathway[] pathway = helper.createPathway();
		helper.destroy();
		return funnelPathway(pathway, tile2 != null, x1, y1, z1, x2, y2, z2);
	}

	/**
	 * 对走廊路径做漏斗平滑，生成路点序列。
	 * Applies funnel smoothing to a corridor pathway to produce waypoints.
	 *
	 * corridor pathway
	 * @param includeTargetPoint 是否包含目标点 / whether to include the target point
	 * @param x1 起点 X / start x
	 * @param y1 起点 Y / start y
	 * @param z1 起点 Z / start z
	 * @param x2 终点 X / end x
	 * @param y2 终点 Y / end y
	 * @param z2 终点 Z / end z
	 * waypoint sequence
	 */
	private static float[][] funnelPathway(NavPathway[] pathway, boolean includeTargetPoint, float x1, float y1, float z1, float x2, float y2, float z2) {
		if (pathway == null) return null; //Mob will ignore all obstacles
		if (pathway.length == 0) return new float[][] {{x1, y1, z1}}; //Mob will not move
		if (pathway.length == 1 && pathway[0].edge == 0) return new float[][] {{x2, y2, z2}}; //Mob will move directly to target
		if (!GeoDataConfig.GEO_NAV_SMOOTH_PATH) {
			return rawPathway(pathway, includeTargetPoint, x2, y2, z2);
		}
		ArrayList<float[]> ret = new ArrayList<float[]>();
		for (int i = pathway.length - 1; i >= 0;) {
			float[][] endpoints = pathway[i--].getEndpoints();
			float[] p;
			if (ret.size() == 0) {
				p = new float[] {x1, y1, z1};
			} else {
				p = ret.get(ret.size() - 1);
				while ((areEqualPoints(p, endpoints[0]) || areEqualPoints(p, endpoints[1])) && i >= 0) {
					endpoints = pathway[i--].getEndpoints();
				}
				if (i < 0 && (areEqualPoints(p, endpoints[0]) || areEqualPoints(p, endpoints[1]))) {
					if (includeTargetPoint) {
						ret.add(new float[]{x2, y2, z2});
					} else {
						ret.add(pathway[0].tile.getClosestPoint(x2, y2, z2));
					}
					break;
				}
			}
			float[] vec1 = new float[] {endpoints[0][0] - p[0], endpoints[0][1] - p[1], endpoints[0][2] - p[2]};
			float[] end1 = endpoints[0];
			int end1i = i;
			float[] pointer1 = endpoints[0];
			float[] vec2 = new float[] {endpoints[1][0] - p[0], endpoints[1][1] - p[1], endpoints[1][2] - p[2]};
			float[] end2 = endpoints[1];
			int end2i = i;
			float[] pointer2 = endpoints[1];
			boolean positive = crossZ(vec1, vec2) > 0; //Cannot == 0
			boolean done = false;
			while (!done && i >= 0) {
				endpoints = pathway[i].getEndpoints();
				boolean v1 = false;
				if (areEqualPoints(pointer1, endpoints[0]) || areEqualPoints(pointer1, endpoints[1])) {
					v1 = true;
				}
				if (v1) {
					// 移动 vec2 / move vec2
					float[] vec2p; //vec2 placeholder
					if (areEqualPoints(pointer1, endpoints[0])) {
						vec2p = new float[] {endpoints[1][0] - p[0], endpoints[1][1] - p[1], endpoints[1][2] - p[2]};
						pointer2 = endpoints[1];
					} else {
						assert areEqualPoints(pointer1, endpoints[1]);
						vec2p = new float[] {endpoints[0][0] - p[0], endpoints[0][1] - p[1], endpoints[0][2] - p[2]};
						pointer2 = endpoints[0];
					}
					if (compareFunnelCross(crossZ(vec2p, vec2), positive, true)) {
						if (compareFunnelCross(crossZ(vec1, vec2p), positive, false)) {
							// 移动 vec2，更新 end2，递减 i，继续 / move vec2, update end2, decrement i, continue
							vec2 = vec2p;
							end2 = pointer2;
							end2i = i;
							i--;
							continue;
						}
						// 将 end1 加入 ret，回到 end1i；vec2p 不在 vec1-vec2 之间且已交叉！ / add end1 to ret, go back to end1i, vec2p wasn't between vec1 and vec2, and crossed over!
						ret.add(end1);
						i = end1i;
						done = true;
						break;
					}
					// vec2p 使漏斗变大！跳到下一端点。 / vec2p made the funnel bigger! skip to next endpoints.
					i--;
				} else {
					// 移动 vec1 / move vec1
					float[] vec1p; //vec1 placeholder
					if (areEqualPoints(pointer2, endpoints[0])) {
						vec1p = new float[] {endpoints[1][0] - p[0], endpoints[1][1] - p[1], endpoints[1][2] - p[2]};
						pointer1 = endpoints[1];
					} else {
						assert areEqualPoints(pointer2, endpoints[1]);
						vec1p = new float[] {endpoints[0][0] - p[0], endpoints[0][1] - p[1], endpoints[0][2] - p[2]};
						pointer1 = endpoints[0];
					}
					if (compareFunnelCross(crossZ(vec1, vec1p), positive, true)) {
						if (compareFunnelCross(crossZ(vec1p, vec2), positive, false)) {
							// 移动 vec1，更新 end1，递减 i，继续 / move vec1, update end1, decrement i, continue
							vec1 = vec1p;
							end1 = pointer1;
							end1i = i;
							i--;
							continue;
						}
						// 将 end2 加入 ret，回到 end2i；vec1p 不在 vec1-vec2 之间且已交叉！ / add end2 to ret, go back to end2i, vec1p wasn't between vec1 and vec2, and crossed over!
						ret.add(end2);
						i = end2i;
						done = true;
						break;
					}
					// vec1p 使漏斗变大！跳到下一端点。 / vec1p made the funnel bigger! skip to next endpoints.
					i--;
				}
			}
			if (!done) {
				float[] vec1p = new float[] {x2 - p[0], y2 - p[1], z2 - p[2]};
				if (compareFunnelCross(crossZ(vec1, vec1p), positive, true)) {
					if (compareFunnelCross(crossZ(vec1p, vec2), positive, false)) {
						if (includeTargetPoint) {
							ret.add(new float[]{x2, y2, z2});
						} else {
							ret.add(pathway[0].tile.getClosestPoint(x2, y2, z2));
						}
					} else {
						// 将 end2 加入 ret，然后目标点 / add end2 to ret, then target point
						ret.add(end2);
						if (includeTargetPoint) {
							ret.add(new float[]{x2, y2, z2});
						} else {
							ret.add(pathway[0].tile.getClosestPoint(x2, y2, z2));
						}
					}
				} else {
					// 将 end1 加入 ret，然后目标点 / add end1 to ret, then target point
					ret.add(end1);
					if (includeTargetPoint) {
						ret.add(new float[]{x2, y2, z2});
					} else {
						ret.add(pathway[0].tile.getClosestPoint(x2, y2, z2));
					}
				}
				break;
			}
		}
		return ret.toArray(new float[0][]);
	}

	/**
	 * 未启用平滑时，按边中点输出原始路点。
	 * Emits raw mid-edge waypoints when path smoothing is disabled.
	 *
	 * corridor pathway
	 * @param includeTargetPoint 是否包含目标点 / whether to include the target point
	 * @param x2 终点 X / end x
	 * @param y2 终点 Y / end y
	 * @param z2 终点 Z / end z
	 * waypoint sequence
	 */
	private static float[][] rawPathway(NavPathway[] pathway, boolean includeTargetPoint, float x2, float y2, float z2) {
		ArrayList<float[]> ret = new ArrayList<float[]>();
		int limit = Math.max(1, GeoDataConfig.GEO_NAV_CORRIDOR_LENGTH);
		for (int i = pathway.length - 1; i >= 0 && ret.size() < limit; i--) {
			float[][] endpoints = pathway[i].getEndpoints();
			if (endpoints != null) {
				ret.add(new float[] {
					(endpoints[0][0] + endpoints[1][0]) / 2F,
					(endpoints[0][1] + endpoints[1][1]) / 2F,
					(endpoints[0][2] + endpoints[1][2]) / 2F
				});
			}
		}
		if (includeTargetPoint && ret.size() < limit) {
			ret.add(new float[] {x2, y2, z2});
		} else if (!includeTargetPoint && ret.size() < limit) {
			ret.add(pathway[0].tile.getClosestPoint(x2, y2, z2));
		}
		return ret.toArray(new float[0][]);
	}

	/**
	 * 判断两点坐标是否完全相等。
	 * Whether two points have identical coordinates.
	 *
	 * @param p1 点 1 / point 1
	 * @param p2 点 2 / point 2
	 * @return 若 equal 则为 true / true if equal
	 */
	private static boolean areEqualPoints(float[] p1, float[] p2) {
		assert p1.length == 3 && p2.length == 3;
		return p1[0] == p2[0] && p1[1] == p2[1] && p1[2] == p2[2];
	}

	/**
	 * 比较漏斗叉积符号是否符合期望方向。
	 * Compares funnel cross-product sign against the expected direction.
	 *
	 * cross-product z component
	 *
	 * @param positive 是否期望正号 / whether a positive sign is expected
	 * @param zeroAllowed 是否允许零 / whether zero is allowed
	 * @param zeroAllowed
	 * @return 符合期望则为 true / true if the sign matches
	 */
	private static boolean compareFunnelCross(float crossZ, boolean positive, boolean zeroAllowed) {
		if (crossZ == 0) return zeroAllowed;
		if (positive) {
			return crossZ > 0;
		} else {
			return crossZ < 0;
		}
	}

	/**
	 * 计算二维向量叉积的 Z 分量。
	 * Computes the Z component of a 2D vector cross product.
	 *
	 * vector 1
	 * vector 2
	 * cross-product z
	 */
	private static float crossZ(float[] vec1, float[] vec2/*, float x1, float y1, float x2, float y2*/) {
		return ((vec1[0] * vec2[1]) - (vec1[1] * vec2[0]));
//		return ((x1 * y2) - (y1 * x2));
	}

	/**
	 * 用向下射线查找坐标所在导航三角形。
	 * Finds the nav tile under coordinates with a downward ray.
	 *
	 * 世界 ID / world id
	 *
	 * @param x X 坐标 / x coordinate
	 * @param y Y 坐标 / y coordinate
	 * @param z Z 坐标 / z coordinate
	 * @param z
	 * @return 导航三角形，未命中则为 null / nav geometry, or null if none
	 */
	private NavGeometry getNavTile(int worldId, float x, float y, float z) {
		return findNavTile(worldId, x, y, z);
	}

	/**
	 * 射线碰撞查找导航三角形。
	 * Locates a nav tile via ray collision.
	 *
	 * 世界 ID / world id
	 *
	 * @param x X 坐标 / x coordinate
	 * @param y Y 坐标 / y coordinate
	 * @param z Z 坐标 / z coordinate
	 * @param z
	 * @return 导航三角形，未命中则为 null / nav geometry, or null if none
	 */
	private NavGeometry findNavTile(int worldId, float x, float y, float z) {
		GeoMap navMap = navData.getNavMap(worldId);
		if (navMap == null) return null;
		Vector3f pos = Vector3f.newInstance().set(x, y, z + 1F),
				 dir = Vector3f.newInstance().set(0, 0, -1F);
		Ray ray = new Ray(pos, dir);
		ray.setLimit(GeoDataConfig.GEO_NAV_GROUND_SEARCH_DISTANCE);
		CollisionResults results = new CollisionResults((byte) 1, false, 0); //Instance ID shouldn't be needed
		int collisionCount = navMap.collideWith(ray, results);
		Vector3f.recycle(pos);
		Vector3f.recycle(dir);
		if (collisionCount == 0) return null;
		Spatial ret = results.getClosestCollision().getGeometry();
		assert ret instanceof NavGeometry;
		try {
			return (NavGeometry) ret;
		} catch (ClassCastException e) {
			log.error(e.toString());
		}
		return null;
	}

	/**
	 * 用包围盒查找附近导航三角形。
	 * Finds a nearby nav tile with a bounding box.
	 *
	 * 世界 ID / world id
	 *
	 * @param x X 坐标 / x coordinate
	 * @param y Y 坐标 / y coordinate
	 * @param z Z 坐标 / z coordinate
	 * @param z
	 * @return 导航三角形，未命中则为 null / nav geometry, or null if none
	 */
	private NavGeometry getNavTileWithBox(int worldId, float x, float y, float z) {
		return findNavTileWithBox(worldId, x, y, z);
	}

	/**
	 * 盒体碰撞查找导航三角形。
	 * Locates a nav tile via bounding-box collision.
	 *
	 * 世界 ID / world id
	 *
	 * @param x X 坐标 / x coordinate
	 * @param y Y 坐标 / y coordinate
	 * @param z Z 坐标 / z coordinate
	 * @param z
	 * @return 导航三角形，未命中则为 null / nav geometry, or null if none
	 */
	private NavGeometry findNavTileWithBox(int worldId, float x, float y, float z) {
		GeoMap navMap = navData.getNavMap(worldId);
		if (navMap == null) return null;
		float extent = GeoDataConfig.GEO_NAV_BOX_EXTENT_XY;
		Vector3f min = Vector3f.newInstance().set(x - extent, y - extent, z + GeoDataConfig.GEO_NAV_BOX_OFFSET_Z_MIN),
				 max = Vector3f.newInstance().set(x + extent, y + extent, z + GeoDataConfig.GEO_NAV_BOX_OFFSET_Z_MAX),
				center = Vector3f.newInstance().set(x, y, z + GeoDataConfig.GEO_NAV_BOX_CENTER_Z);
		BoundingBox box = new BoundingBox(min,max);
		box.setCenter(center);
		CollisionResults results = new CollisionResults((byte) 1, false, 0); //Instance ID shouldn't be needed
		int collisionCount = navMap.collideWith(box, results);
		Vector3f.recycle(min);
		Vector3f.recycle(max);
		Vector3f.recycle(center);
		if (collisionCount == 0) return null;
		Spatial ret = results.getClosestCollision().getGeometry();
		assert ret instanceof NavGeometry;
		try {
			return (NavGeometry) ret;
		} catch (ClassCastException e) {
			log.error(e.toString());
		}
		return null;
	}

	/**
	 * 路径走廊中的一段：三角形 + 穿越边编号。
	 * One segment of a path corridor: tile plus the crossed edge index.
	 */
	static class NavPathway {
		/** 导航三角形。 / Nav geometry tile. */
		NavGeometry tile;
		/** 边编号：0/1/2/3。 / Edge index: 0, 1, 2, or 3. */
		byte edge; //Values are 0, 1, 2, or 3

		/**
		 * 构造路径段。
		 * Constructs a pathway segment.
		 *
		 * @param tile 导航三角形 / nav geometry
		 * edge index
		 */
		NavPathway(NavGeometry tile, byte edge) {
			this.tile = tile;
			this.edge = edge;
		}

		/**
		 * 返回穿越边的两端点；edge 为 0 时表示目标在起点三角形内。
		 * Returns the endpoints of the crossed edge; edge 0 means the target was inside the start tile.
		 *
		 * @return 两端点坐标，或 null / endpoint coordinates, or null
		 */
		float[][] getEndpoints() {
			float[][] ret;
			switch (edge) {
			case 0:
				// 表示目标在起始瓦片内 / Means the target was inside the starting tile
				return null;
			case 1:
			case 2:
			case 3:
				ret = tile.getEndpoints(edge);
				break;
			default:
				assert false:"Incorrect NavPathway Creation";
				return null;
			}
			return ret;
		}
	}

	/**
	 * 获取导航服务单例（优先 Spring 提供者）。
	 * Returns the nav-service singleton (preferring the Spring provider).
	 *
	 * service instance
	 */
	public static final NavService getInstance() {
		ObjectProvider<NavService> provider = instanceProvider;
		if (provider == null) {
			return SingletonHolder.INSTANCE;
		}
		return provider.getIfAvailable(() -> SingletonHolder.INSTANCE);
	}

	/**
	 * 注入 Spring 单例提供者。
	 * Injects the Spring singleton provider.
	 *
	 * provider
	 */
	public static void setInstanceProvider(ObjectProvider<NavService> instanceProvider) {
		NavService.instanceProvider = instanceProvider;
	}

	/**
	 * 单例持有者。
	 * Singleton holder.
	 */
	private static final class SingletonHolder {
		/** 默认实例。 / Default instance. */
		protected static final NavService INSTANCE = new NavService();
	}
}
