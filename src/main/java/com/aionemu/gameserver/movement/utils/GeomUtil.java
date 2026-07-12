package com.aionemu.gameserver.movement.utils;

import com.aionemu.gameserver.geoEngine.math.FastMath;
import com.aionemu.gameserver.geoEngine.math.Vector2f;
import com.aionemu.gameserver.geoEngine.math.Vector3f;

/**
 * 移动相关几何工具：二维/三维点推算、方向与距离计算。
 * Geometry helpers for movement: 2D/3D point projection, direction and distance.
 */
public class GeomUtil {

	/**
	 * 从源点按角度（度）与距离推算二维目标点。
	 * Project a 2D target point from a source by angle (degrees) and distance.
	 *
	 * Source point
	 * Angle in degrees
	 * Distance
	 * @return 目标二维点 / Target 2D point
	 */
	public static Vector2f getNextPoint2D(Vector2f source, float angle, float distance) {
		double x = (double) source.x + (double) distance * Math.cos((double) angle * Math.PI / 180.0);
		double y = (double) source.y + (double) distance * Math.sin((double) angle * Math.PI / 180.0);
		return new Vector2f((float) x, (float) y);
	}

	/**
	 * 从源坐标沿单位向量方向按距离推算二维目标点。
	 * Project a 2D target from source coordinates along a direction vector by distance.
	 *
	 * @param sX 源 X / Source X
	 * @param sY 源 Y / Source Y
	 * @param vecX 方向 X 分量 / Direction X component
	 * @param vecY 方向 Y 分量 / Direction Y component
	 * Distance
	 * @return 目标二维点 / Target 2D point
	 */
	public static Vector2f getNextPoint2D(float sX, float sY, float vecX, float vecY, float distance) {
		return new Vector2f(sX + vecX * distance, sY + vecY * distance);
	}

	/**
	 * 计算从 {@code from} 指向 {@code to} 的单位方向向量。
	 * Compute the unit direction vector from {@code from} toward {@code to}.
	 *
	 * Origin
	 *
	 * @param to 终点 / Destination
	 * @param to
	 * @return 归一化方向向量 / Normalized direction vector
	 */
	public static Vector3f getDirection3D(Vector3f from, Vector3f to) {
		Vector3f direction = to.subtract(from);
		return direction.normalizeLocal();
	}

	/**
	 * 从源点沿方向向量按距离推算三维目标点。
	 * Project a 3D target point from a source along a direction by distance.
	 *
	 * Source point
	 * Direction vector
	 * Distance
	 * @return 目标三维点 / Target 3D point
	 */
	public static Vector3f getNextPoint3D(Vector3f source, Vector3f direction, float distance) {
		return source.add(direction.mult(distance));
	}

	/**
	 * 计算源点到指定坐标的三维欧氏距离。
	 * Compute the 3D Euclidean distance from a source point to given coordinates.
	 *
	 * Source point
	 * @param x2 目标 X / Target X
	 * @param y2 目标 Y / Target Y
	 * @param z2 目标 Z / Target Z
	 * Distance
	 */
	public static float getDistance3D(Vector3f source, float x2, float y2, float z2) {
		return GeomUtil.getDistance3D(source.x, source.y, source.z, x2, y2, z2);
	}

	/**
	 * 计算两点间的三维欧氏距离。
	 * Compute the 3D Euclidean distance between two points.
	 *
	 * @param x1 起点 X / Origin X
	 * @param y1 起点 Y / Origin Y
	 * @param z1 起点 Z / Origin Z
	 * @param x2 终点 X / Destination X
	 * @param y2 终点 Y / Destination Y
	 * @param z2 终点 Z / Destination Z
	 * Distance
	 */
	public static float getDistance3D(float x1, float y1, float z1, float x2, float y2, float z2) {
		double dx = x1 - x2;
		double dy = y1 - y2;
		double dz = z1 - z2;
		return FastMath.sqrt((float) (dx * dx + dy * dy + dz * dz));
	}
}
