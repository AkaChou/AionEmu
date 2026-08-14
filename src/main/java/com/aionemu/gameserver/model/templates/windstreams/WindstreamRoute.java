package com.aionemu.gameserver.model.templates.windstreams;

import java.util.List;

import com.aionemu.gameserver.model.geometry.Point3D;
import com.aionemu.gameserver.utils.MathUtil;

/**
 * 风道路线：按经过毫秒数在航点间线性插值计算飞行位置。
 * Windstream route: computes flight positions by linear interpolation between waypoints over elapsed time.
 */
public final class WindstreamRoute {
	private final int mapId;
	private final int id;
	private final int durationMillis;
	private final List<Point3D> points;

	public WindstreamRoute(int mapId, int id, int durationMillis, List<Point3D> points) {
		this.mapId = mapId;
		this.id = id;
		this.durationMillis = durationMillis;
		this.points = List.copyOf(points);
	}

	public int getMapId() {
		return mapId;
	}

	public int getId() {
		return id;
	}

	public int getDurationMillis() {
		return durationMillis;
	}

	public int getPointCount() {
		return points.size();
	}

	/**
	 * 返回经过指定毫秒数时的插值位置；越界返回 null。
	 * Returns the interpolated position at the given elapsed time; null when out of range.
	 *
	 * @param elapsedMillis 经过毫秒数 / elapsed milliseconds
	 * @return 位置或 null / position or null
	 */
	public Point3D positionAt(int elapsedMillis) {
		if (elapsedMillis < 0 || elapsedMillis > durationMillis) {
			return null;
		}
		double index = elapsedMillis / 1000.0;
		int fromIndex = (int) index;
		if (fromIndex >= points.size() - 1) {
			return points.getLast();
		}
		Point3D from = points.get(fromIndex);
		Point3D to = points.get(fromIndex + 1);
		float ratio = (float) (index - fromIndex);
		return new Point3D(from.getX() + (to.getX() - from.getX()) * ratio,
			from.getY() + (to.getY() - from.getY()) * ratio,
			from.getZ() + (to.getZ() - from.getZ()) * ratio);
	}

	/**
	 * 判断给定坐标是否在指定时刻的路线位置附近（宽度范围内）。
	 * Whether the given coordinates are within the route position at the given time plus width.
	 *
	 * @param elapsedMillis 经过毫秒数 / elapsed milliseconds
	 * @param x X 坐标 / x coordinate
	 * @param y Y 坐标 / y coordinate
	 * @param z Z 坐标 / z coordinate
	 * @param width 允许距离 / allowed distance
	 * @return 在范围内则为 true / true if within range
	 */
	public boolean contains(int elapsedMillis, float x, float y, float z, float width) {
		Point3D expected = positionAt(elapsedMillis);
		return expected != null && MathUtil.getDistance(expected.getX(), expected.getY(), expected.getZ(), x, y, z) <= width;
	}
}
