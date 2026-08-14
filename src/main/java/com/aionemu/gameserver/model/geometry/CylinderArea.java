package com.aionemu.gameserver.model.geometry;

import com.aionemu.gameserver.model.templates.zone.Point2D;
import com.aionemu.gameserver.utils.MathUtil;
import com.aionemu.gameserver.world.zone.ZoneName;

/**
 * 圆柱区域，用于几何相关逻辑。
 * Cylinder Area for geometry logic.
 *
 * @author SoulKeeper
 */
public class CylinderArea extends AbstractArea {

	/**
	 * 圆心 X 坐标。
	 * Center X of cylinder.
	 */
	private final float centerX;

	/**
	 * 圆心 Y 坐标。
	 * Center Y of cylinder.
	 */
	private final float centerY;

	/**
	 * 圆柱半径。
	 * Cylinder radius.
	 */
	private final float radius;

	/**
	 * 创建给定半径的圆柱区域。
	 * Creates new cylinder with given radius.
	 *
	 * @param zoneName 区域名称 / zone name
	 * @param worldId 世界 ID / world id
	 * @param center 圆心 / center of the circle
	 * @param radius 半径 / radius of the circle
	 * @param minZ 最小 Z 坐标 / minimal z
	 * @param maxZ 最大 Z 坐标 / maximal z
	 */
	public CylinderArea(ZoneName zoneName, int worldId, Point2D center, float radius, float minZ, float maxZ) {
		this(zoneName, worldId, center.getX(), center.getY(), radius, minZ, maxZ);
	}

	/**
	 * 创建给定半径的圆柱区域。
	 * Creates new cylinder with given radius.
	 *
	 * @param zoneName 区域名称 / zone name
	 * @param worldId 世界 ID / world id
	 * @param x 圆心 X 坐标 / center x coord
	 * @param y 圆心 Y 坐标 / center y coord
	 * @param radius 半径 / radius of the circle
	 * @param minZ 最小 Z 坐标 / minimal z
	 * @param maxZ 最大 Z 坐标 / maximal z
	 */
	public CylinderArea(ZoneName zoneName, int worldId, float x, float y, float radius, float minZ, float maxZ) {
		super(zoneName, worldId, minZ, maxZ);
		this.centerX = x;
		this.centerY = y;
		this.radius = radius;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isInside2D(float x, float y) {
		return MathUtil.getDistance(centerX, centerY, x, y) < radius;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public double getDistance2D(float x, float y) {
		if (isInside2D(x, y)) {
			return 0;
		} else {
			return Math.abs(MathUtil.getDistance(centerX, centerY, x, y) - radius);
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
			if (z < getMinZ()) {
				return MathUtil.getDistance(centerX, centerY, getMinZ(), x, y, z);
			} else {
				return MathUtil.getDistance(centerX, centerY, getMaxZ(), x, y, z);
			}
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
			float vX = x - this.centerX;
			float vY = y - this.centerY;
			double magV = MathUtil.getDistance(centerX, centerY, x, y);
			double pointX = centerX + vX / magV * radius;
			double pointY = centerY + vY / magV * radius;
			return new Point2D((float) pointX, (float) pointY);
		}
	}

	/** 矩形相交 / intersects Rectangle. */
	@Override
	public boolean intersectsRectangle(RectangleArea area) {
		if (area.getMinZ() > getMaxZ() || area.getMaxZ() < getMinZ()) {
			return false;
		}
		if (area.getDistance2D(centerX, centerY) < radius) {
			return true;
		}
		return false;
	}
}
