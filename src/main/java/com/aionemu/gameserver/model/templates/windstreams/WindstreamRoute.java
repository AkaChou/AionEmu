package com.aionemu.gameserver.model.templates.windstreams;

import java.util.List;

import com.aionemu.gameserver.model.geometry.Point3D;
import com.aionemu.gameserver.utils.MathUtil;

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

	public Point3D positionAt(int elapsedMillis) {
		if (elapsedMillis < 0 || elapsedMillis > durationMillis) {
			return null;
		}
		double index = (double) elapsedMillis * (points.size() - 1) / durationMillis;
		int fromIndex = (int) index;
		Point3D from = points.get(fromIndex);
		if (fromIndex == points.size() - 1) {
			return from;
		}
		Point3D to = points.get(fromIndex + 1);
		float ratio = (float) (index - fromIndex);
		return new Point3D(from.getX() + (to.getX() - from.getX()) * ratio,
			from.getY() + (to.getY() - from.getY()) * ratio,
			from.getZ() + (to.getZ() - from.getZ()) * ratio);
	}

	public boolean contains(int elapsedMillis, float x, float y, float z, float width) {
		Point3D expected = positionAt(elapsedMillis);
		return expected != null && MathUtil.getDistance(expected.getX(), expected.getY(), expected.getZ(), x, y, z) <= width;
	}
}
