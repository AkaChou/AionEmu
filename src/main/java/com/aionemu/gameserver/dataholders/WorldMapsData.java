package com.aionemu.gameserver.dataholders;

import java.util.Iterator;
import java.util.List;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.aionemu.gameserver.model.templates.world.WorldMapTemplate;

import com.aionemu.commons.utils.collections.IntObjectHashMap;

/**
 * 世界地图数据容器，持有全部地图模板，数据源为 data/static_data/world_maps.xml。
 * World maps data holder containing all {@link WorldMapTemplate} objects, loaded from data/static_data/world_maps.xml.
 *
 * @author Luno
 */
@XmlRootElement(name = "world_maps")
@XmlAccessorType(XmlAccessType.NONE)
public class WorldMapsData implements Iterable<WorldMapTemplate> {

	@XmlElement(name = "map")
	protected List<WorldMapTemplate> worldMaps;

	protected IntObjectHashMap<WorldMapTemplate> worldIdMap = new IntObjectHashMap<WorldMapTemplate>();

	/**
	 * JAXB 反序列化完成后，将地图模板按地图 ID 建索引。
	 * After JAXB unmarshalling, indexes map templates by map id.
	 */
	protected void afterUnmarshal(Unmarshaller u, Object parent) {
		for (WorldMapTemplate map : worldMaps) {
			worldIdMap.put(map.getMapId(), map);
		}
	}

	/**
	 * 返回地图模板列表的迭代器。
	 * Returns an iterator over the world map templates.
	 *
	 * @return 地图模板迭代器 / map template iterator
	 */
	@Override
	public Iterator<WorldMapTemplate> iterator() {
		return worldMaps.iterator();
	}

	/**
	 * 返回已加载的地图数量。
	 * Returns the number of loaded maps.
	 *
	 * map count
	 */
	public int size() {
		return worldMaps == null ? 0 : worldMaps.size();
	}

	/**
	 * 按世界 ID 获取地图模板。
	 * Returns the world map template for the given world id.
	 *
	 * 世界 ID / world id
	 *
	 * @param worldId @return 地图模板，不存在则为 null / map template or null
	 */
	public WorldMapTemplate getTemplate(int worldId) {
		return worldIdMap.get(worldId);
	}
}
