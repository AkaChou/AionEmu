package com.aionemu.gameserver.dataholders;

import java.util.List;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.aionemu.gameserver.model.templates.spawns.Spawn;
import com.aionemu.gameserver.model.templates.towns.TownLevel;
import com.aionemu.gameserver.model.templates.towns.TownSpawn;
import com.aionemu.gameserver.model.templates.towns.TownSpawnMap;

import com.aionemu.commons.utils.collections.IntObjectHashMap;

/**
 * 城镇刷怪静态数据容器，按地图 ID 索引城镇刷怪配置。
 * Town spawn static-data holder, indexing town spawn maps by map id.
 */
@XmlRootElement(name = "town_spawns_data")
public class TownSpawnsData {
	@XmlElement(name = "spawn_map")
	private List<TownSpawnMap> spawnMap;

	private IntObjectHashMap<TownSpawnMap> spawnMapsData = new IntObjectHashMap<TownSpawnMap>();

	/**
	 * JAXB 反序列化完成后，将刷怪地图索引到映射并释放列表。
	 * After JAXB unmarshalling, indexes spawn maps by map id and clears the list.
	 */
	void afterUnmarshal(Unmarshaller u, Object parent) {
		spawnMapsData.clear();
		for (TownSpawnMap map : spawnMap) {
			spawnMapsData.put(map.getMapId(), map);
		}
		spawnMap.clear();
		spawnMap = null;
	}

	/**
	 * 统计全部城镇刷怪点数量。
	 * Counts the total number of town spawn entries.
	 *
	 * @return 刷怪点总数 / total spawn count
	 */
	public int getSpawnsCount() {
		int counter = 0;
		for (TownSpawnMap spawnMap : spawnMapsData.values()) {
			for (TownSpawn townSpawn : spawnMap.getTownSpawns()) {
				for (TownLevel townLevel : townSpawn.getTownLevels()) {
					counter += townLevel.getSpawns().size();
				}
			}
		}
		return counter;
	}

	/**
	 * 按城镇 ID 与等级获取刷怪列表。
	 * Returns the spawn list for the given town id and level.
	 *
	 * town id
	 * town level
	 * @return 刷怪列表，不存在则为 null / spawn list or null
	 */
	public List<Spawn> getSpawns(int townId, int townLevel) {
		for (TownSpawnMap spawnMap : spawnMapsData.values()) {
			if (spawnMap.getTownSpawn(townId) != null) {
				TownSpawn townSpawn = spawnMap.getTownSpawn(townId);
				return townSpawn.getSpawnsForLevel(townLevel).getSpawns();
			}
		}
		return null;
	}

	/**
	 * 按城镇 ID 查询所属世界地图 ID。
	 * Returns the world map id that contains the given town.
	 *
	 * town id
	 *
	 * @param townId @return 地图 ID，未找到则为 0 / map id, or 0 if not found
	 */
	public int getWorldIdForTown(int townId) {
		for (TownSpawnMap spawnMap : spawnMapsData.values())
			if (spawnMap.getTownSpawn(townId) != null) {
				return spawnMap.getMapId();
			}
		return 0;
	}
}
