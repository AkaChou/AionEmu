package com.aionemu.gameserver.model.templates.towns;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 城镇刷新点地图模板（静态数据/XML）。
 * XML template.
 *
 * @author ViAl
 */
@XmlType(name = "town_spawn_map")
public class TownSpawnMap {

	@XmlAttribute(name = "map_id")
	private int mapId;
	@XmlElement(name = "town_spawn")
	private List<TownSpawn> townSpawns;
	private Map<Integer, TownSpawn> townSpawnsData = new HashMap<Integer, TownSpawn>();

	/**
	 * 反序列化后将城镇出生列表转为按城镇 ID 索引的映射。
	 * Build the town-id-indexed map after unmarshalling.
	 *
	 * @param u JAXB 反序列化器 / Unmarshaller
	 * @param parent 父对象 / Parent object
	 */
	void afterUnmarshal(Unmarshaller u, Object parent) {
		townSpawnsData.clear();

		for (TownSpawn town : townSpawns) {
			townSpawnsData.put(town.getTownId(), town);
		}
		townSpawns.clear();
		townSpawns = null;
	}

	/**
	 * @return the mapId
	 */
	public int getMapId() {
		return mapId;
	}

	/** 获取城镇刷新点。 / Returns the town spawn. */
	public TownSpawn getTownSpawn(int townId) {
		return townSpawnsData.get(townId);
	}

	/** 获取城镇刷新。 / Returns the town spawns. */
	public Collection<TownSpawn> getTownSpawns() {
		return townSpawnsData.values();
	}
}
