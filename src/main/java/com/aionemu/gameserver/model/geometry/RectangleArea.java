package com.aionemu.gameserver.model.geometry;

import java.awt.Point;
import java.awt.Rectangle;

import com.aionemu.gameserver.model.templates.zone.Point2D;
import com.aionemu.gameserver.utils.MathUtil;
import com.aionemu.gameserver.world.zone.ZoneName;

/**
 * Rectangle 区域，用于几何相关逻辑。
 * Rectangle Area for geometry logic.
 *
 * @author SoulKeeper
 */
public class RectangleArea extends AbstractArea {

	/**
	 * 最小 X 坐标。
	 * Min x point.
	 */
	private final float minX;

	/**
	 * @return 最小 X 坐标 / the min x
	 */
	public float getMinX() {
		return minX;
	}

	/**
	 * @return 最大 X 坐标 / the max x
	 */
	public float getMaxX() {
		return maxX;
	}

	/**
	 * @return 最小 Y 坐标 / the min y
	 */
	public float getMinY() {
		return minY;
	}

	/**
	 * @return 最大 Y 坐标 / the max y
	 */
	public float getMaxY() {
		return maxY;
	}

	/**
	 * 最大 X 坐标。
	 * Max x point.
	 */
	private final float maxX;

	/**
	 * 最小 Y 坐标。
	 * Min y point.
	 */
	private final float minY;

	/**
	 * 最大 Y 坐标。
	 * Max y point.
	 */
	private final float maxY;

	/**
	 * 由给定点创建新的矩形区域，点的顺序无关紧要。
	 * Creates new area from given points. Point order doesn't matter.
	 *
	 * @param zoneName 区域名称 / zone name
	 * @param worldId 世界 ID / world id
	 * @param p1 顶点 1 / point 1
	 * @param p2 顶点 2 / point 2
	 * @param p3 顶点 3 / point 3
	 * @param p4 顶点 4 / point 4
	 * @param minZ 最小 Z 坐标 / minimal z
	 * @param maxZ 最大 Z 坐标 / maximal z
	 */
	public RectangleArea(ZoneName zoneName, int worldId, Point p1, Point p2, Point p3, Point p4, int minZ, int maxZ) {
		super(zoneName, worldId, minZ, maxZ);

		Rectangle r = new Rectangle();
		r.add(p1);
		r.add(p2);
		r.add(p3);
		r.add(p4);

		minX = (int) r.getMinX();
		maxX = (int) r.getMaxX();
		minY = (int) r.getMinY();
		maxY = (int) r.getMaxY();
	}

	/**
	 * 由给定坐标创建新的矩形区域。
	 * Creates new area from given coords.
	 *
	 * @param zoneName 区域名称 / zone name
	 * @param worldId 世界 ID / world id
	 * @param minX 最小 X 坐标 / minimal x point
	 * @param minY 最小 Y 坐标 / minimal y point
	 * @param maxX 最大 X 坐标 / maximal x point
	 * @param maxY 最大 Y 坐标 / maximal y point
	 * @param minZ 最小 Z 坐标 / minimal z point
	 * @param maxZ 最大 Z 坐标 / maximal z point
	 */
	public RectangleArea(ZoneName zoneName, int worldId, float minX, float minY, float maxX, float maxY, float minZ,
			float maxZ) {
		super(zoneName, worldId, minZ, maxZ);
		this.minX = minX;
		this.maxX = maxX;
		this.minY = minY;
		this.maxY = maxY;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isInside2D(float x, float y) {
		return x >= minX && x <= maxX && y >= minY && y <= maxY;
	}

	/**
	 * @return 是否在三维区域内 / Whether inside 3D
	 */
	@Override
	public boolean isInside3D(float x, float y, float z) {
		if (!isInside2D(x, y)) {
			return false;
		}
		return super.isInside3D(x, y, z);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public double getDistance2D(float x, float y) {
		if (isInside2D(x, y)) {
			return 0;
		} else {
			Point2D cp = getClosestPoint(x, y);
			return MathUtil.getDistance(x, y, cp.getX(), cp.getY());
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public double getDistance3D(float x, float y, float z) {
		if (isInside3D(x, y, z)) {
			return 0;
		} else if (isInsideZ(z)) {
			return getDistance2D(x, y);
		} else {
			Point3D cp = getClosestPoint(x, y, z);
			return MathUtil.getDistance(x, y, z, cp.getX(), cp.getY(), cp.getZ());
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Point2D getClosestPoint(float x, float y) {

		if (isInside2D(x, y)) {
			return new Point2D(x, y);
		} else {
			// 底边 / bottom edge
			Point2D closestPoint = MathUtil.getClosestPointOnSegment(minX, minY, maxX, minY, x, y);
			double distance = MathUtil.getDistance(x, y, closestPoint.getX(), closestPoint.getY());

			// 顶边 / top edge
			Point2D cp = MathUtil.getClosestPointOnSegment(minX, maxY, maxX, maxY, x, y);
			double d = MathUtil.getDistance(x, y, cp.getX(), cp.getY());
			if (d < distance) {
				closestPoint = cp;
				distance = d;
			}

			// 左边 / left edge
			cp = MathUtil.getClosestPointOnSegment(minX, minY, minX, maxY, x, y);
			d = MathUtil.getDistance(x, y, cp.getX(), cp.getY());
			if (d < distance) {
				closestPoint = cp;
				distance = d;
			}

			// 右边 / Right edge
			cp = MathUtil.getClosestPointOnSegment(maxX, minY, maxX, maxY, x, y);
			d = MathUtil.getDistance(x, y, cp.getX(), cp.getY());
			if (d < distance) {
				closestPoint = cp;
				// distance = d;
			}
			return closestPoint;
		}
	}

	@Override
	public boolean intersectsRectangle(RectangleArea area) {
		return area.getMinZ() <= getMaxZ() && area.getMaxZ() >= getMinZ()
				&& area.minX <= maxX && area.maxX >= minX
				&& area.minY <= maxY && area.maxY >= minY;
	}
}
