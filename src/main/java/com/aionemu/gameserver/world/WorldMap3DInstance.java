package com.aionemu.gameserver.world;

import com.aionemu.gameserver.world.zone.ZoneInstance;

/**
 * 三维世界地图实例：按 X/Y/Z 划分区域（欧比斯等立体地图）。
 * Three-dimensional world-map instance: partitions by X/Y/Z (abyss and similar vertical maps).
 *
 * @author ATracer
 */
public class WorldMap3DInstance extends WorldMapInstance {

	/**
	 * 构造 3D 地图实例。
	 * Construct a 3D map instance.
	 *
	 * @param parent 父级世界地图 / parent world map
	 * @param instanceId 实例 ID / instance id
	 */
	public WorldMap3DInstance(WorldMap parent, int instanceId) {
		super(parent, instanceId);
	}

	/**
	 * 按 X/Y/Z 坐标取得 3D 地图区域。
	 * Resolve the 3D map region for X/Y/Z.
	 *
	 * @param x 坐标 X / X coordinate
	 * @param y 坐标 Y / Y coordinate
	 * @param z 坐标 Z / Z coordinate
	 * @return 地图区域 / the map region
	 */
	@Override
	public MapRegion getRegion(float x, float y, float z) {
		int regionId = RegionUtil.get3dRegionId(x, y, z);
		return regions.get(regionId);
	}

	/**
	 * 初始化全部 3D 区域并建立邻接关系。
	 * Initialize all 3D regions and wire neighbour links.
	 */
	protected void initMapRegions() {
		int size = this.getParent().getWorldSize();
		float maxZ = Math.round((float) size / regionSize) * regionSize;

		// Create all mapRegion
		for (int x = 0; x <= size; x = x + regionSize) {
			for (int y = 0; y <= size; y = y + regionSize) {
				for (int z = 0; z < maxZ; z = z + regionSize) {
					int regionId = RegionUtil.get3dRegionId(x, y, z);
					regions.put(regionId, createMapRegion(regionId));
				}
			}
		}

		// 添加邻居 / Add Neighbour
		for (int x = 0; x <= size; x = x + regionSize) {
			for (int y = 0; y <= size; y = y + regionSize) {
				for (int z = 0; z < maxZ; z = z + regionSize) {
					int regionId = RegionUtil.get3dRegionId(x, y, z);
					MapRegion mapRegion = regions.get(regionId);
					for (int x2 = x - regionSize; x2 <= x + regionSize; x2 += regionSize) {
						for (int y2 = y - regionSize; y2 <= y + regionSize; y2 += regionSize) {
							for (int z2 = z - regionSize; z2 < z + regionSize; z2 += regionSize) {
								if (x2 == x && y2 == y && z2 == z) {
									continue;
								}
								int neighbourId = RegionUtil.get3dRegionId(x2, y2, z2);
								MapRegion neighbour = regions.get(neighbourId);
								if (neighbour != null) {
									mapRegion.addNeighbourRegion(neighbour);
								}
							}
						}
					}
				}
			}
		}
	}

	/**
	 * 按 3D 区域 ID 创建地图区域及关联 Zone。
	 * Create a map region and related zones for a 3D region id.
	 *
	 * @param regionId 区域 ID / the region id
	 * @return 新建的地图区域 / newly created map region
	 */
	@Override
	protected MapRegion createMapRegion(int regionId) {
		float startX = RegionUtil.getXFrom3dRegionId(regionId);
		float startY = RegionUtil.getYFrom3dRegionId(regionId);
		float startZ = RegionUtil.getZFrom3dRegionId(regionId);
		ZoneInstance[] zones = filterZones(this.getMapId(), regionId, startX, startY, startZ, startZ + regionSize);
		return new MapRegion(regionId, this, zones);
	}

	/**
	 * 3D 实例不为个人实例。
	 * 3D instances are never personal.
	 *
	 * @return 恒为 false / always false
	 */
	@Override
	public boolean isPersonal() {
		return false;
	}

	/**
	 * 3D 实例无个人所有者。
	 * 3D instances have no personal owner.
	 *
	 * @return 恒为 0 / always 0
	 */
	@Override
	public int getOwnerId() {
		return 0;
	}
}
