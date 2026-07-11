package com.aionemu.gameserver.model.geometry;

import com.aionemu.gameserver.model.templates.zone.Point2D;
import com.aionemu.gameserver.world.zone.ZoneName;

/**
 * 区域接口。
 * Area interface.
 *
 * @author SoulKeeper
 */
public interface Area {

	/**
	 * 返回若为真则 point 为 insideareaignoringzvalue。 / Returns true if point is inside area ignoring z value
	 *
	 * @param point point to check
	 * @return point is inside or not
	 */
	public boolean isInside2D(Point2D point);

	/**
	 * 返回若为真则 coords 为 insideareaignoringzvalue。 / Returns true if coords are inside area ignoring z value
	 *
	 * @param x x coord
	 * @param y y coord
	 * @return coords are inside or not
	 */
	public boolean isInside2D(float x, float y);

	/**
	 * 返回若为真则 point 为 insidearea。 / Returns true if point is inside area
	 *
	 * @param point point to check
	 * @return true if point is inside
	 */
	public boolean isInside3D(Point3D point);

	/**
	 * 返回若为真则 coors 为 insidearea。 / Returns true if coors are inside area
	 *
	 * @param x x coord
	 * @param y y coord
	 * @param z z coord
	 * @return true if coords are inside
	 */
	public boolean isInside3D(float x, float y, float z);

	/**
	 * 检查是否 zcoord 为 insize。 / Checks if z coord is insize
	 *
	 * @param point point to check
	 * @return is z inside or not
	 */
	public boolean isInsideZ(Point3D point);

	/**
	 * Checks is z coord is inside
	 *
	 * @param z z coord
	 * @return is z inside or not
	 */
	public boolean isInsideZ(float z);

	/**
	 * 返回 distance 从 point 到 closestpoint 的此 areaignoringz.<br> 返回 0 若 point 为 insidearea。 / Returns distance from point to closest point of this area ignoring z.<br> Returns 0 if point is inside area
	 *
	 * @param point point to calculate distance from
	 * @return distance or 0 if is inside area
	 */
	public double getDistance2D(Point2D point);

	/**
	 * 返回 distance 从 point 到 closestpoint 的此 areaignoringz.<br> 返回 0point 为 insidearea。 / Returns distance from point to closest point of this area ignoring z.<br> Returns 0 point is inside area
	 *
	 * @param x x coord
	 * @param y y coord
	 * @return distance or 0 if is inside area
	 */
	public double getDistance2D(float x, float y);

	/**
	 * 返回 distance 从 point 到此 area.<br> 返回 0 若为 inside。 / Returns distance from point to this area.<br> Returns 0 if is inside
	 *
	 * @param point point to check
	 * @return distance or 0 if is inside
	 */
	public double getDistance3D(Point3D point);

	/**
	 * 返回 distance 从 coords 到此 area。 / Returns distance from coords to this area
	 *
	 * @param x x coord
	 * @param y y coord
	 * @param z z coord
	 * @return distance or 0 if is inside
	 */
	public double getDistance3D(float x, float y, float z);

	/**
	 * 返回 closestpoint 的 area 到给定 point.<br> 返回 point 带 coords = pointarg 若为 inside。 / Returns closest point of area to given point.<br> Returns point with coords = point arg if is inside
	 *
	 * @param point point to check
	 * @return closest point
	 */
	public Point2D getClosestPoint(Point2D point);

	/**
	 * 返回 closestpoint 的 area 到给定 coords.<br> 返回 point 带 coordsx 并 y 若 coords 为 inside。 / Returns closest point of area to given coords.<br> Returns point with coords x and y if coords are inside
	 *
	 * @param x x coord
	 * @param y y coord
	 * @return closest point
	 */
	public Point2D getClosestPoint(float x, float y);

	/**
	 * 返回区域到给定点的最近点。 / Returns closest point of area to given point.<br> Works exactly like {@link #getClosestPoint(int, int)} if {@link #isInsideZ(int)} returns true.<br> In other case closest z edge is set as z coord.
	 */
	public Point3D getClosestPoint(Point3D point);

	/**
	 * 返回区域到给定坐标的最近点。 / Returns closest point of area to given coords.<br> Works exactly like {@link #getClosestPoint(int, int)} if {@link #isInsideZ(int)} returns true.<br> In other case closest z edge is set as z coord.
	 */
	public Point3D getClosestPoint(float x, float y, float z);

	/**
	 * Return minimal z of this area
	 *
	 * @return minimal z of this area
	 */
	public float getMinZ();

	/**
	 * Returns maximal z of this area
	 *
	 * @return maximal z of this area
	 */
	public float getMaxZ();

	/** 矩形相交 / intersects Rectangle. */
	public boolean intersectsRectangle(RectangleArea area);

	/** 返回世界 ID / Returns the world id */
	public int getWorldId();

	/** 获取区域名称。 / Returns the zone name. */
	public ZoneName getZoneName();
}
