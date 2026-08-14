package com.aionemu.gameserver.movement.processors.movement;

import com.aionemu.gameserver.lifecycle.GameWorldServices;

import com.aionemu.gameserver.geoEngine.math.Vector2f;
import com.aionemu.gameserver.geoEngine.math.Vector3f;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.utils.MathUtil;
import com.aionemu.gameserver.movement.utils.GeomUtil;

/**
 * 寻路辅助：在可见扇形内采样可通行点，用于跟随与步进路径选择。
 * Pathfinding helper: samples walkable points in a visible sector for follow and step selection.
 */
public class PathfindHelper {

	/**
	 * 可见扇形总角度（度）。
	 * Total visible sector angle in degrees.
	 */
	private static final int VISIBLE_ANGLE = 180;

	/**
	 * 扇形内角度采样步长（度）。
	 * Angle sampling step within the sector, in degrees.
	 */
	private static final int PATHFIND_ANGLE_STEP = 20;

	/**
	 * 从源生物到目标点选择下一步可通行位置（扇形采样）。
	 * Select the next walkable step from a creature toward a target via sector sampling.
	 *
	 * @param source 源生物 / source creature
	 * @param target 目标点 / target point
	 * @return 最近可通行采样点，若无则为 null / closest walkable sample, or null
	 */
	public static Vector3f selectStep(Creature source, Vector3f target) {
		int mapId = source.getPosition().getMapId();
		int instanceId = source.getPosition().getInstanceId();
		float zOffset = source.getObjectTemplate().getBoundRadius().getUpper() / 2.0f;
		if ((double) zOffset > 2.2) {
			zOffset = 2.2f;
		}
		float newZOffset = Math.max(0.6f, source.getObjectTemplate().getBoundRadius().getUpper() * 0.7f);
		if (source.getTarget() instanceof Player) {
			newZOffset = 1.5f;
		}
		Vector3f sourcePoint = new Vector3f(source.getX(), source.getY(), source.getZ());
		Vector3f targetPoint = target.clone();
		double futureDistance = GeomUtil.getDistance3D(sourcePoint.x, sourcePoint.y, sourcePoint.z, targetPoint.x,
				targetPoint.y, targetPoint.z);
		int offset = VISIBLE_ANGLE / 2;
		int rounds = VISIBLE_ANGLE / PATHFIND_ANGLE_STEP + 1;

		Vector3f closetsPoint = null;
		double minimalDistance = Short.MAX_VALUE;

		for (int i = 0; i < rounds; ++i) {
			Vector3f rotated = PathfindHelper.Rotate(source, sourcePoint.x, sourcePoint.y, targetPoint.x, targetPoint.y,
					futureDistance, i * PATHFIND_ANGLE_STEP - offset, targetPoint.z);
			if (targetPoint.z - rotated.z > source.getObjectTemplate().getBoundRadius().getUpper() || rotated.z == 0.0f)
				continue;

			double newRotatedDistance = MathUtil.getDistance(sourcePoint.x, sourcePoint.y, sourcePoint.z, rotated.x,
					rotated.y, rotated.z);
			boolean canPassTemp = GameWorldServices.geoService().canPass(mapId, sourcePoint.x, sourcePoint.y,
					sourcePoint.z + zOffset, rotated.x, rotated.y, rotated.z + newZOffset, (float) newRotatedDistance,
					instanceId);
			if (!canPassTemp)
				continue;

			double canPassDistance = MathUtil.getDistance(targetPoint.x, targetPoint.y, targetPoint.z, rotated.x,
					rotated.y, rotated.z);
			if (!(minimalDistance > canPassDistance))
				break;
			minimalDistance = canPassDistance;
			closetsPoint = rotated;
		}
		return closetsPoint;
	}

	/**
	 * 为跟随目标选择下一步位置；地图或实例不一致时返回 null。
	 * Select the next follow step toward a visible target; returns null on map/instance mismatch.
	 *
	 * @param source 源生物 / source creature
	 * @param target 跟随目标 / follow target
	 * @return 下一步点，不可用为 null / next step, or null if unavailable
	 */
	public static Vector3f selectFollowStep(Creature source, VisibleObject target) {
		int mapId = source.getPosition().getMapId();
		int instanceId = source.getPosition().getInstanceId();
		if (target.getPosition().getMapId() != mapId || target.getPosition().getInstanceId() != instanceId) {
			return null;
		}
		Vector3f point = new Vector3f(target.getX(), target.getY(), target.getZ());
		assert (point.x != 0.0f && point.y != 0.0f);
		return PathfindHelper.selectStep(source, point);
	}

	/**
	 * 以源点为圆心、给定半径与角度偏移旋转目标，并采样地表高度。
	 * Rotate a target around a center by radius and degree offset, then sample ground Z.
	 *
	 * @param owner 用于取世界/实例的生物 / Creature used for world/instance lookup
	 * @param cx 圆心 X / Center X
	 * @param cy 圆心 Y / Center Y
	 * @param x1 参考点 X / Reference X
	 * @param y1 参考点 Y / Reference Y
	 * Radius
	 * @param degrees 相对角度偏移（度） / Relative angle offset in degrees
	 * Default Z
	 * @return 旋转后的三维点 / Rotated 3D point
	 */
	private static Vector3f Rotate(Creature owner, float cx, float cy, float x1, float y1, double radius, float degrees,
			float defaultZ) {
		double beginDeg = Math.toDegrees(Math.atan2(y1 - cy, x1 - cx));
		degrees = (float) ((double) degrees + beginDeg);
		double x = (double) cx + radius * Math.cos((double) degrees * Math.PI / 180.0);
		double y = (double) cy + radius * Math.sin((double) degrees * Math.PI / 180.0);
		double z = GameWorldServices.geoService().getZ(owner.getWorldId(), (float) x, (float) y, defaultZ, 100.0f,
				owner.getInstanceId());
		return new Vector3f((float) x, (float) y, (float) z);
	}

	/**
	 * 在给定距离范围内尝试选取随机可走点（当前实现恒返回 null）。
	 * Try to pick a random walkable point within a range (currently always returns null).
	 *
	 * Source creature
	 * Minimum range
	 * Maximum range
	 * @return 随机点，当前恒为 null / Random point; currently always null
	 */
	public static Vector3f getRandomPoint(Creature source, float minRange, float maxRange) {
		Vector3f origin = new Vector3f(source.getX(), source.getY(), source.getZ());
		assert (minRange > 0.0f && maxRange > minRange);
		int SearchAngle = 360;
		int AngleStep = 60;
		int randDist = (int) (Math.random() * (double) maxRange + (double) minRange);
		int randAngle = (int) (Math.random() * 360.0);
		for (int i = 0; i < SearchAngle; i += AngleStep) {
			Vector2f rotated2D = GeomUtil.getNextPoint2D(new Vector2f(origin.x, origin.y), randAngle + i, randDist);
		}
		return null;
	}
}
