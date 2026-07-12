package com.aionemu.gameserver.world;

import com.aionemu.gameserver.world.zone.ZoneInstance;

import lombok.Getter;
import lombok.Setter;

/**
 * 二维世界地图实例：按 X/Y 划分区域，可绑定个人所有者。
 * Two-dimensional world-map instance: partitions by X/Y, optionally personal-owned.
 *
 * @author ATracer
 */
public class WorldMap2DInstance extends WorldMapInstance {

	/** 个人实例所有者 ID，0 表示公共 / personal-instance owner id, 0 if public */
	@Getter
	@Setter
	private int ownerId;

	/**
	 * 构造 2D 地图实例。
	 * Construct a 2D map instance.
	 *
	 * @param parent 父级世界地图 / parent world map
	 * instance id
	 * @param ownerId 个人所有者 ID / personal owner id
	 */
	public WorldMap2DInstance(WorldMap parent, int instanceId, int ownerId) {
		super(parent, instanceId);
		this.ownerId = ownerId;
	}

	/**
	 * 按 2D 区域 ID 创建地图区域及关联 Zone。
	 * Create a map region and related zones for a 2D region id.
	 *
	 * region id
	 *
	 * @param regionId
	 * @return 新建的地图区域 / newly created map region
	 */
	@Override
	protected MapRegion createMapRegion(int regionId) {
		float startX = RegionUtil.getXFrom2dRegionId(regionId);
		float startY = RegionUtil.getYFrom2dRegionId(regionId);
		int size = this.getParent().getWorldSize();
		float maxZ = Math.round((float) size / regionSize) * regionSize;
		ZoneInstance[] zones = filterZones(this.getMapId(), regionId, startX, startY, 0, maxZ);
		return new MapRegion(regionId, this, zones);
	}

	/**
	 * 初始化全部 2D 区域并建立邻接关系。
	 * Initialize all 2D regions and wire neighbour links.
	 */
	protected void initMapRegions() {
		int size = this.getParent().getWorldSize();
		// Create all mapRegion
		for (int x = 0; x <= size; x = x + regionSize) {
			for (int y = 0; y <= size; y = y + regionSize) {
				int regionId = RegionUtil.get2dRegionId(x, y);
				regions.put(regionId, createMapRegion(regionId));
			}
		}

		// 添加邻居 / Add Neighbour
		for (int x = 0; x <= size; x = x + regionSize) {
			for (int y = 0; y <= size; y = y + regionSize) {
				int regionId = RegionUtil.get2dRegionId(x, y);
				MapRegion mapRegion = regions.get(regionId);
				for (int x2 = x - regionSize; x2 <= x + regionSize; x2 += regionSize) {
					for (int y2 = y - regionSize; y2 <= y + regionSize; y2 += regionSize) {
						if (x2 == x && y2 == y) {
							continue;
						}
						int neighbourId = RegionUtil.get2dRegionId(x2, y2);
						MapRegion neighbour = regions.get(neighbourId);
						if (neighbour != null) {
							mapRegion.addNeighbourRegion(neighbour);
						}
					}
				}
			}
		}
	}

	/**
	 * 按 X/Y 坐标取得 2D 地图区域（Z 忽略）。
	 * Resolve the 2D map region for X/Y (Z ignored).
	 *
	 * @param x 坐标 X / X coordinate
	 * @param y 坐标 Y / Y coordinate
	 * @param z 坐标 Z（未使用） / Z coordinate (unused)
	 * map region
	 */
	@Override
	public MapRegion getRegion(float x, float y, float z) {
		int regionId = RegionUtil.get2dRegionId(x, y);
		return regions.get(regionId);
	}

	/**
	 * 是否为个人实例。
	 * Whether this is a personal instance.
	 *
	 * true when ownerId is non-zero
	 */
	@Override
	public boolean isPersonal() {
		return ownerId != 0;
	}
}
