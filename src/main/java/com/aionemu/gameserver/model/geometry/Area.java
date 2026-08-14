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
	 * 忽略 Z 值时返回该点是否在区域内。
	 * Returns true if point is inside area ignoring z value.
	 *
	 * @param point 要检查的点 / point to check
	 * @return 点是否在区域内 / point is inside or not
	 */
	public boolean isInside2D(Point2D point);

	/**
	 * 忽略 Z 值时返回坐标是否在区域内。
	 * Returns true if coords are inside area ignoring z value.
	 *
	 * @param x x 坐标 / x coord
	 * @param y y 坐标 / y coord
	 * @return 坐标是否在区域内 / coords are inside or not
	 */
	public boolean isInside2D(float x, float y);

	/**
	 * 返回该点是否在区域内。
	 * Returns true if point is inside area.
	 *
	 * @param point 要检查的点 / point to check
	 * @return 点是否在区域内 / true if point is inside
	 */
	public boolean isInside3D(Point3D point);

	/**
	 * 返回坐标是否在区域内。
	 * Returns true if coords are inside area.
	 *
	 * @param x x 坐标 / x coord
	 * @param y y 坐标 / y coord
	 * @param z z 坐标 / z coord
	 * @return 坐标是否在区域内 / true if coords are inside
	 */
	public boolean isInside3D(float x, float y, float z);

	/**
	 * 检查 Z 坐标是否在范围内。
	 * Checks if z coord is inside.
	 *
	 * @param point 要检查的点 / point to check
	 * @return Z 坐标是否在范围内 / is z inside or not
	 */
	public boolean isInsideZ(Point3D point);

	/**
	 * 检查 Z 坐标是否在范围内。
	 * Checks if z coord is inside.
	 *
	 * @param z z 坐标 / z coord
	 * @return Z 坐标是否在范围内 / is z inside or not
	 */
	public boolean isInsideZ(float z);

	/**
	 * 返回点到区域最近点（忽略 Z）的距离；点在区域内时返回 0。
	 * Returns distance from point to closest point of this area ignoring z.<br> Returns 0 if point is inside area.
	 *
	 * @param point 要计算距离的点 / point to calculate distance from
	 * @return 距离，区域内为 0 / distance or 0 if is inside area
	 */
	public double getDistance2D(Point2D point);

	/**
	 * 返回坐标到区域最近点（忽略 Z）的距离；点在区域内时返回 0。
	 * Returns distance from coords to closest point of this area ignoring z.<br> Returns 0 if point is inside area.
	 *
	 * @param x x 坐标 / x coord
	 * @param y y 坐标 / y coord
	 * @return 距离，区域内为 0 / distance or 0 if is inside area
	 */
	public double getDistance2D(float x, float y);

	/**
	 * 返回点到区域的距离；点在区域内时返回 0。
	 * Returns distance from point to this area.<br> Returns 0 if is inside.
	 *
	 * @param point 要检查的点 / point to check
	 * @return 距离，区域内为 0 / distance or 0 if is inside
	 */
	public double getDistance3D(Point3D point);

	/**
	 * 返回坐标到区域的距离；坐标在区域内时返回 0。
	 * Returns distance from coords to this area.
	 *
	 * @param x x 坐标 / x coord
	 * @param y y 坐标 / y coord
	 * @param z z 坐标 / z coord
	 * @return 距离，区域内为 0 / distance or 0 if is inside
	 */
	public double getDistance3D(float x, float y, float z);

	/**
	 * 返回区域到给定点的最近点；点在区域内时返回该点本身。
	 * Returns closest point of area to given point.<br> Returns point with coords = point arg if is inside.
	 *
	 * @param point 要检查的点 / point to check
	 * @return 最近点 / closest point
	 */
	public Point2D getClosestPoint(Point2D point);

	/**
	 * 返回区域到给定坐标的最近点；坐标在区域内时返回该坐标点。
	 * Returns closest point of area to given coords.<br> Returns point with coords x and y if coords are inside.
	 *
	 * @param x x 坐标 / x coord
	 * @param y y 坐标 / y coord
	 * @return 最近点 / closest point
	 */
	public Point2D getClosestPoint(float x, float y);

	/**
	 * 返回区域到给定点的最近点；Z 坐标在范围内时与二维最近点一致，否则取最近的 Z 边界。
	 * Returns closest point of area to given point.<br> Works exactly like {@link #getClosestPoint(int, int)} if {@link #isInsideZ(int)} returns true.<br> In other case closest z edge is set as z coord.
	 */
	public Point3D getClosestPoint(Point3D point);

	/**
	 * 返回区域到给定坐标的最近点；Z 坐标在范围内时与二维最近点一致，否则取最近的 Z 边界。
	 * Returns closest point of area to given coords.<br> Works exactly like {@link #getClosestPoint(int, int)} if {@link #isInsideZ(int)} returns true.<br> In other case closest z edge is set as z coord.
	 */
	public Point3D getClosestPoint(float x, float y, float z);

	/**
	 * 返回区域的最小 Z 值。
	 * Return minimal z of this area.
	 *
	 * @return 区域的最小 Z 值 / minimal z of this area
	 */
	public float getMinZ();

	/**
	 * 返回区域的最大 Z 值。
	 * Returns maximal z of this area.
	 *
	 * @return 区域的最大 Z 值 / maximal z of this area
	 */
	public float getMaxZ();

	/** 矩形相交 / intersects Rectangle. */
	public boolean intersectsRectangle(RectangleArea area);

	/** 返回世界 ID / Returns the world id */
	public int getWorldId();

	/** 获取区域名称。 / Returns the zone name. */
	public ZoneName getZoneName();
}
