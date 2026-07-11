package com.aionemu.gameserver.model.geometry;

import com.aionemu.gameserver.model.templates.zone.Point2D;
import com.aionemu.gameserver.utils.MathUtil;
import com.aionemu.gameserver.world.zone.ZoneName;

/**
 * 球体区域，用于几何相关逻辑。
 * Sphere Area for geometry logic.
 *
 * @author MrPoke
 */
public class SphereArea implements Area {

	protected float x;
	protected float y;
	protected float z;
	protected float r;
	protected int worldId;
	protected ZoneName zoneName;

	/**
	 * @param zoneName
	 * @param worldId
	 * @param x
	 * @param y
	 * @param z
	 * @param r
	 */
	public SphereArea(ZoneName zoneName, int worldId, float x, float y, float z, float r) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.r = r;
		this.worldId = worldId;
		this.zoneName = zoneName;
	}

	@Deprecated
	/**
	 * @param point Whether inside 2 d / Whether inside 2 d
	 */
	@Override
	public boolean isInside2D(Point2D point) {
		return false;
	}

	@Deprecated
	/**
	 * @return Whether inside 2 d / Whether inside 2 d
	 */
	@Override
	public boolean isInside2D(float x, float y) {
		return false;
	}

	/**
	 * @param point Whether inside 3 d / Whether inside 3 d
	 */
	@Override
	public boolean isInside3D(Point3D point) {
		return MathUtil.isIn3dRange(x, y, z, point.getX(), point.getY(), point.getZ(), r);
	}

	/**
	 * @return Whether inside 3 d / Whether inside 3 d
	 */
	@Override
	public boolean isInside3D(float x, float y, float z) {
		return MathUtil.isIn3dRange(x, y, z, this.x, this.y, this.z, r);
	}

	/**
	 * @param point Whether inside z / Whether inside z
	 */
	@Override
	public boolean isInsideZ(Point3D point) {
		return isInsideZ(point.getZ());
	}

	/**
	 * @param z Whether inside z / Whether inside z
	 */
	@Override
	public boolean isInsideZ(float z) {
		return z >= this.getMinZ() && z <= this.getMaxZ();
	}

	@Deprecated
	/** 返回 distance 2 d / Returns the distance 2 d */
	@Override
	public double getDistance2D(Point2D point) {
		return 0;
	}

	@Deprecated
	/** 返回 distance 2 d / Returns the distance 2 d */
	@Override
	public double getDistance2D(float x, float y) {
		return 0;
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
		return distance > 0 ? distance : 0;
	}

	@Deprecated
	/** 返回 closest point / Returns the closest point */
	@Override
	public Point2D getClosestPoint(Point2D point) {
		return null;
	}

	@Deprecated
	/** 返回 closest point / Returns the closest point */
	@Override
	public Point2D getClosestPoint(float x, float y) {
		return null;
	}

	/** 返回 closest point / Returns the closest point */
	@Override
	public Point3D getClosestPoint(Point3D point) {
		return null;
	}

	/** 返回 closest point / Returns the closest point */
	@Override
	public Point3D getClosestPoint(float x, float y, float z) {
		return null;
	}

	/** 返回 min z / Returns the min z */
	@Override
	public float getMinZ() {
		return z - r;
	}

	/** 返回 max z / Returns the max z */
	@Override
	public float getMaxZ() {
		return z + r;
	}

	/** 矩形相交 / intersects Rectangle. */
	@Override
	public boolean intersectsRectangle(RectangleArea area) {
		if (area.getDistance3D(x, y, z) <= r) {
			return true;
		}
		return false;
	}

	/** 返回世界 ID / Returns the world id */
	@Override
	public int getWorldId() {
		return worldId;
	}

	/** 获取区域名称。 / Returns the zone name. */
	@Override
	public ZoneName getZoneName() {
		return zoneName;
	}
}
