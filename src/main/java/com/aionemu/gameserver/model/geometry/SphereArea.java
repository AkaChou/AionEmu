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
	 * 创建球体区域。
	 * Creates a sphere area.
	 *
	 * @param zoneName 区域名称 / zone name
	 * @param worldId 世界 ID / world id
	 * @param x 球心 X 坐标 / center x coord
	 * @param y 球心 Y 坐标 / center y coord
	 * @param z 球心 Z 坐标 / center z coord
	 * @param r 半径 / radius
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
	 * @param point 要检查的点 / point to check
	 * @return 是否在二维区域内 / Whether inside 2D
	 */
	@Override
	public boolean isInside2D(Point2D point) {
		return false;
	}

	@Deprecated
	/**
	 * @return 是否在二维区域内 / Whether inside 2D
	 */
	@Override
	public boolean isInside2D(float x, float y) {
		return false;
	}

	/**
	 * @param point 要检查的点 / point to check
	 * @return 是否在球体区域内 / Whether inside 3D
	 */
	@Override
	public boolean isInside3D(Point3D point) {
		return MathUtil.isIn3dRange(x, y, z, point.getX(), point.getY(), point.getZ(), r);
	}

	/**
	 * @return 是否在球体区域内 / Whether inside 3D
	 */
	@Override
	public boolean isInside3D(float x, float y, float z) {
		return MathUtil.isIn3dRange(x, y, z, this.x, this.y, this.z, r);
	}

	/**
	 * @param point 要检查的点 / point to check
	 * @return Z 坐标是否在球体范围内 / Whether inside z
	 */
	@Override
	public boolean isInsideZ(Point3D point) {
		return isInsideZ(point.getZ());
	}

	/**
	 * @param z z 坐标 / z coord
	 * @return Z 坐标是否在球体范围内 / Whether inside z
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
