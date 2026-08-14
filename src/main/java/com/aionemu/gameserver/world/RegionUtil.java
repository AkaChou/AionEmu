package com.aionemu.gameserver.world;

import com.aionemu.gameserver.configs.main.WorldConfig;

/**
 * 地图区域 ID 编解码工具：在 2D/3D 坐标与区域 ID 之间转换。
 * Map-region id encode/decode utilities for converting between 2D/3D coordinates and region ids.
 *
 * @author ATracer
 */
public class RegionUtil {

	/** 3D 区域 ID 的 X 轴基础偏移 / X-axis base offset for 3D region ids */
	public static final int X_3D_OFFSET = 1000000;
	/** 3D 区域 ID 的 Y 轴基础偏移 / Y-axis base offset for 3D region ids */
	public static final int Y_3D_OFFSET = 1000;
	/** 2D 区域 ID 的 X 轴基础偏移 / X-axis base offset for 2D region ids */
	public static final int X_2D_OFFSET = 1000;

	/**
	 * 由区域尺寸与二维坐标计算 2D 区域 ID。
	 * Compute a 2D region id from region size and coordinates.
	 *
	 * @param regionSize 区域边长 / region edge length
	 * @param x 坐标 X / X coordinate
	 * @param y 坐标 Y / Y coordinate
	 * @return 2D 区域 ID / the 2D region id
	 */
	public static final int get2DRegionId(int regionSize, float x, float y) {
		return (int) x / regionSize * X_2D_OFFSET + (int) y / regionSize;
	}

	/**
	 * 由区域尺寸与三维坐标计算 3D 区域 ID。
	 * Compute a 3D region id from region size and coordinates.
	 *
	 * @param regionSize 区域边长 / region edge length
	 * @param x 坐标 X / X coordinate
	 * @param y 坐标 Y / Y coordinate
	 * @param z 坐标 Z / Z coordinate
	 * @return 3D 区域 ID / the 3D region id
	 */
	public static final int get3DRegionId(int regionSize, float x, float y, float z) {
		return (int) x / regionSize * X_3D_OFFSET + (int) y / regionSize * Y_3D_OFFSET + (int) z / regionSize;
	}

	/**
	 * 使用全局配置的区域尺寸计算 2D 区域 ID。
	 * Compute a 2D region id using the global region size config.
	 *
	 * @param x 坐标 X / X coordinate
	 * @param y 坐标 Y / Y coordinate
	 * @return 2D 区域 ID / the 2D region id
	 */
	public static final int get2dRegionId(float x, float y) {
		return get2DRegionId(WorldConfig.WORLD_REGION_SIZE, x, y);
	}

	/**
	 * 使用全局配置的区域尺寸计算 3D 区域 ID。
	 * Compute a 3D region id using the global region size config.
	 *
	 * @param x 坐标 X / X coordinate
	 * @param y 坐标 Y / Y coordinate
	 * @param z 坐标 Z / Z coordinate
	 * @return 3D 区域 ID / the 3D region id
	 */
	public static final int get3dRegionId(float x, float y, float z) {
		return get3DRegionId(WorldConfig.WORLD_REGION_SIZE, x, y, z);
	}

	/**
	 * 从 2D 区域 ID 还原区域起始 X。
	 * Recover region start X from a 2D region id.
	 *
	 * @param regionId 2D 区域 ID / the 2D region id
	 * @return 区域起始 X / the start X
	 */
	public static final int getXFrom2dRegionId(int regionId) {
		return regionId / X_2D_OFFSET * WorldConfig.WORLD_REGION_SIZE;
	}

	/**
	 * 从 2D 区域 ID 还原区域起始 Y。
	 * Recover region start Y from a 2D region id.
	 *
	 * @param regionId 2D 区域 ID / the 2D region id
	 * @return 区域起始 Y / the start Y
	 */
	public static final int getYFrom2dRegionId(int regionId) {
		return regionId % X_2D_OFFSET * WorldConfig.WORLD_REGION_SIZE;
	}

	/**
	 * 从 3D 区域 ID 还原区域起始 X。
	 * Recover region start X from a 3D region id.
	 *
	 * @param regionId 3D 区域 ID / the 3D region id
	 * @return 区域起始 X / the start X
	 */
	public static final int getXFrom3dRegionId(int regionId) {
		return regionId / X_3D_OFFSET * WorldConfig.WORLD_REGION_SIZE;
	}

	/**
	 * 从 3D 区域 ID 还原区域起始 Y。
	 * Recover region start Y from a 3D region id.
	 *
	 * @param regionId 3D 区域 ID / the 3D region id
	 * @return 区域起始 Y / the start Y
	 */
	public static final int getYFrom3dRegionId(int regionId) {
		return regionId % X_3D_OFFSET / Y_3D_OFFSET * WorldConfig.WORLD_REGION_SIZE;
	}

	/**
	 * 从 3D 区域 ID 还原区域起始 Z。
	 * Recover region start Z from a 3D region id.
	 *
	 * @param regionId 3D 区域 ID / the 3D region id
	 * @return 区域起始 Z / the start Z
	 */
	public static final int getZFrom3dRegionId(int regionId) {
		return regionId % X_3D_OFFSET % Y_3D_OFFSET * WorldConfig.WORLD_REGION_SIZE;
	}
}
