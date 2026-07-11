package com.aionemu.gameserver.world.zone;

import com.aionemu.gameserver.configs.main.WorldConfig;
import com.aionemu.gameserver.model.geometry.AbstractArea;
import com.aionemu.gameserver.model.geometry.RectangleArea;

/**
 * 地图分块对应的矩形区域。
 * Rectangular area corresponding to a map region tile.
 *
 * @author ATracer
 */
public class RegionZone extends RectangleArea {

	/**
	 * 以给定起点与 Z 范围构造一个世界分块矩形。
	 * Build a world-region rectangle from the given start point and Z range.
	 *
	 * start X
	 * start Y
	 * min Z
	 * max Z
	 */
	public RegionZone(float startX, float startY, float minZ, float maxZ) {
		super(null, 0, startX, startY, startX + WorldConfig.WORLD_REGION_SIZE, startY + WorldConfig.WORLD_REGION_SIZE,
				minZ, maxZ);
	}

	/**
	 * 判断给定区域是否在本分块内（当前恒为 true）。
	 * Whether the given area is inside this region (currently always true).
	 *
	 * @param area 待测区域 / area to test
	 * whether inside
	 */
	public boolean isInside(AbstractArea area) {
		return true;
	}
}
