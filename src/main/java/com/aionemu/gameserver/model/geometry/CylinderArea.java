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
	 * 圆柱中心 / Center of cylinder
	 */
	private final float centerX;

	/**
	 * 圆柱中心 / Center of cylinder
	 */
	private final float centerY;

	/**
	 * Cylinder radius
	 */
	private final float radius;

	/**
	 * 创建新 cylinder 给定 radius。 / Creates new cylinder with given radius
	 *
	 * @param zoneName center of the circle
	 * @param worldId radius of the circle
	 * @param center   min z
	 * @param radius   max z
	 */
	public CylinderArea(ZoneName zoneName, int worldId, Point2D center, float radius, float minZ, float maxZ) {
		this(zoneName, worldId, center.getX(), center.getY(), radius, minZ, maxZ);
	}

	/**
	 * 创建新 cylider 给定 radius。 / Creates new cylider with given radius
	 *
	 * @param zoneName      center coord
	 * @param worldId      center coord
	 * @param x radius of the circle
	 * @param y   min z
	 * @param radius   max z
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
