package com.aionemu.gameserver.model.geometry;

import com.aionemu.gameserver.utils.MathUtil;
import com.aionemu.gameserver.world.zone.ZoneName;

/**
 * 半球区域，用于几何相关逻辑。
 * Semisphere Area for geometry logic.
 *
 * @author Rolandas
 */
public class SemisphereArea extends SphereArea {

	public SemisphereArea(ZoneName zoneName, int worldId, float x, float y, float z, float r) {
		super(zoneName, worldId, x, y, z, r);
	}

	/**
	 * @param point Whether inside 3 d
	 */
	@Override
	public boolean isInside3D(Point3D point) {
		return this.z < point.getZ() && MathUtil.isIn3dRange(x, y, z, point.getX(), point.getY(), point.getZ(), r);
	}

	/**
	 * @return Whether inside 3 d
	 */
	@Override
	public boolean isInside3D(float x, float y, float z) {
		return this.z < z && MathUtil.isIn3dRange(x, y, z, this.x, this.y, this.z, r);
	}

	/**
	 * @param point Whether inside z
	 */
	@Override
	public boolean isInsideZ(Point3D point) {
		return isInsideZ(point.getZ());
	}

	/** 返回 min z / Returns the min z */
	@Override
	public float getMinZ() {
		return z;
	}

	/** 返回 max z / Returns the max z */
	@Override
	public float getMaxZ() {
		return z + r;
	}

	/** 返回 distance 3 d / Returns the distance 3 d */
	@Override
	public double getDistance3D(Point3D point) {
		return getDistance3D(point.getX(), point.getY(), point.getZ());
	}

	/** 返回 distance 3 d / Returns the distance 3 d */
	@Override
	public double getDistance3D(float x, float y, float z) {
		double distance = MathUtil.getDistance(x, y, z, this.x, this.y, this.z) - r;
		if (z < this.z) {
			return distance;
		}
		return distance > 0 ? distance : 0;
	}

	/** 矩形相交 / intersects Rectangle. */
	@Override
	public boolean intersectsRectangle(RectangleArea area) {
		if ((area.getMaxZ() >= z || z <= area.getMinZ()) && area.getDistance3D(x, y, z) <= r) {
			return true;
		}
		return false;
	}
}
