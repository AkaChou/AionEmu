package com.aionemu.gameserver.world;

import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;

/**
 * 世界中对象的位置：地图、区域、坐标与朝向。
 * Position of an object in the world: map, region, coordinates and heading.
 *
 * @author Rinzler (Encom)
 * @author -Nemesiss-
 */
@Slf4j
public class WorldPosition {

	/**
	 * 以指定地图 ID 构造位置。
	 * Construct a position for the given map id.
	 *
	 * @param mapId 地图 ID / the map id
	 */
	public WorldPosition(int mapId) {
		this.mapId = mapId;
	}

	/** 地图 ID / map id */
	private int mapId;
	/** 所在地图区域 / map region */
	private MapRegion mapRegion;
	/** 世界 X / world X */
	private float x;
	/** 世界 Y / world Y */
	private float y;
	/** 世界 Z / world Z */
	private float z;
	/** 朝向，0–120（120 等价于 0） / heading, 0–120 (120 equals 0) */
	private byte heading;
	/** 是否已生成（可见） / whether spawned (visible) */
	private boolean isSpawned = false;

	/**
	 * 返回世界地图 ID。
	 * Return the world map id.
	 *
	 * @return 地图 ID / the map id
	 */
	public int getMapId() {
		if (mapId == 0)
			log.warn(I18n.get("log.c35fe2659e5e", this.toString()));
		return mapId;
	}

	/**
	 * 设置地图 ID。
	 * Set the map id.
	 *
	 * @param mapId 地图 ID / the map id
	 */
	public void setMapId(int mapId) {
		this.mapId = mapId;
	}

	/**
	 * 返回世界坐标 X。
	 * Return world X.
	 *
	 * @return X
	 */
	public float getX() {
		return x;
	}

	/**
	 * 返回世界坐标 Y。
	 * Return world Y.
	 *
	 * @return Y
	 */
	public float getY() {
		return y;
	}

	/**
	 * 返回世界坐标 Z。
	 * Return world Z.
	 *
	 * @return Z
	 */
	public float getZ() {
		return z;
	}

	/**
	 * 返回当前地图区域；未生成时返回 null。
	 * Return the current map region; null when not spawned.
	 *
	 * @return 地图区域或 null / map region or null
	 */
	public MapRegion getMapRegion() {
		return isSpawned ? mapRegion : null;
	}

	/**
	 * 返回实例 ID。
	 * Return the instance id.
	 *
	 * @return 实例 ID / the instance id
	 */
	public int getInstanceId() {
		return mapRegion.getParent().getInstanceId();
	}

	/**
	 * 返回父地图的实例数量。
	 * Return the parent map's instance count.
	 *
	 * @return 实例数量 / the instance count
	 */
	public int getInstanceCount() {
		return mapRegion.getParent().getParent().getInstanceCount();
	}

	/**
	 * 是否副本类型地图。
	 * Whether this is an instance-type map.
	 *
	 * @return 副本地图为 true / true if instance map
	 */
	public boolean isInstanceMap() {
		return mapRegion.getParent().getParent().isInstanceType();
	}

	/**
	 * 当前地图区域是否处于激活状态。
	 * Whether the current map region is active.
	 *
	 * @return 若 active 则为 true / true if active
	 */
	public boolean isMapRegionActive() {
		return mapRegion.isMapRegionActive();
	}

	/**
	 * 返回朝向。
	 * Return heading.
	 *
	 * @return 朝向 / the heading
	 */
	public byte getHeading() {
		return heading;
	}

	/**
	 * 返回所属 {@link World}。
	 * Return the owning {@link World}.
	 *
	 * @return 所属世界 / the owning world
	 */
	public World getWorld() {
		return mapRegion.getWorld();
	}

	/**
	 * 返回所属地图实例。
	 * Return the owning world-map instance.
	 *
	 * @return 所属地图实例 / the world map instance
	 */
	public WorldMapInstance getWorldMapInstance() {
		return mapRegion.getParent();
	}

	/**
	 * 对象是否已生成。
	 * Whether the object is spawned.
	 *
	 * @return 已生成返回 true / true if spawned
	 */
	public boolean isSpawned() {
		return isSpawned;
	}

	/**
	 * 设置生成状态。
	 * Set spawned flag.
	 *
	 * @param val 是否生成 / whether spawned
	 */
	void setIsSpawned(boolean val) {
		isSpawned = val;
	}

	/**
	 * 设置地图区域。
	 * Set the map region.
	 *
	 * @param r 地图区域 / map region
	 */
	void setMapRegion(MapRegion r) {
		mapRegion = r;
	}

	/**
	 * 设置世界坐标与朝向（null 参数表示保持原值）。
	 * Set world coordinates and heading (null args keep previous values).
	 *
	 * @param newX 新 X，可为 null / new X, or null
	 * @param newY 新 Y，可为 null / new Y, or null
	 * @param newZ 新 Z，可为 null / new Z, or null
	 * @param newHeading 新朝向，可为 null；取值 0–120 / new heading, or null; 0–120
	 */
	public void setXYZH(Float newX, Float newY, Float newZ, Byte newHeading) {
		if (newX != null) {
			x = newX;
		}
		if (newY != null) {
			y = newY;
		}
		if (newZ != null) {
			z = newZ;
		}
		if (newHeading != null) {
			heading = newHeading;
		}
	}

	/**
	 * 设置 Z 坐标。
	 * Set Z coordinate.
	 *
	 * @param z 坐标 Z / Z coordinate
	 */
	public void setZ(float z) {
		this.z = z;
	}

	/**
	 * 设置朝向。
	 * Set heading.
	 *
	 * @param h 朝向 / heading
	 */
	public void setH(byte h) {
		this.heading = h;
	}

	@Override
	public String toString() {
		return "WorldPosition [heading=" + heading + ", isSpawned=" + isSpawned + ", mapRegion=" + mapRegion + ", x="
				+ x + ", y=" + y + ", z=" + z + "]";
	}

	/**
	 * 浅克隆当前位置（共享 mapRegion 引用）。
	 * Shallow-clone this position (shares the mapRegion reference).
	 *
	 * @return 浅克隆副本 / the clone
	 */
	@Override
	public WorldPosition clone() {
		WorldPosition pos = new WorldPosition(this.mapId);
		pos.heading = this.heading;
		pos.isSpawned = this.isSpawned;
		pos.mapRegion = this.mapRegion;
		pos.x = this.x;
		pos.y = this.y;
		pos.z = this.z;
		return pos;
	}
}
