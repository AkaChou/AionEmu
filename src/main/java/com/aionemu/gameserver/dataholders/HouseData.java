package com.aionemu.gameserver.dataholders;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.templates.housing.Building;
import com.aionemu.gameserver.model.templates.housing.HouseAddress;
import com.aionemu.gameserver.model.templates.housing.HouseType;
import com.aionemu.gameserver.model.templates.housing.HousingLand;

/**
 * 房屋地块配置数据容器，按地块 ID 与入口世界 ID 索引房屋地块。
 * House land configuration data holder, indexed by land id and entry world id.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = { "lands" })
@XmlRootElement(name = "house_lands")
public class HouseData {

	@XmlElement(name = "land")
	protected List<HousingLand> lands;

	@XmlTransient
	Map<Integer, HousingLand> landsById = new HashMap<Integer, HousingLand>();

	@XmlTransient
	Map<Integer, Set<HousingLand>> landsByEntryWorldId = new HashMap<Integer, Set<HousingLand>>();

	/**
	 * JAXB 反序列化完成后，按地块 ID 与入口世界 ID 建立索引并释放原始列表。
	 * After JAXB unmarshalling, indexes lands by id and entry world id, then releases the raw list.
	 */
	void afterUnmarshal(Unmarshaller u, Object parent) {
		if (lands == null) {
			return;
		}
		for (HousingLand land : lands) {
			landsById.put(land.getId(), land);
			for (HouseAddress address : land.getAddresses()) {
				Integer exitMapId = address.getExitMapId();
				if (exitMapId == null) {
					exitMapId = Integer.valueOf(address.getMapId());
				}
				Set<HousingLand> landList = landsByEntryWorldId.get(exitMapId);
				if (landList == null) {
					landList = new HashSet<HousingLand>();
					landsByEntryWorldId.put(exitMapId, landList);
				}
				landList.add(land);
			}
		}
		lands.clear();
		lands = null;
	}

	/**
	 * 按世界 ID 获取该入口世界下的全部地块。
	 * Returns all lands for the given entry world id.
	 *
	 * entry world id
	 *
	 * @param worldId
	 * @return 地块集合，不存在则为 null / set of lands, or null if absent
	 */
	public Set<HousingLand> getLandsForWorldId(int worldId) {
		return landsByEntryWorldId.get(worldId);
	}

	/**
	 * 在指定世界中按房屋类型尺寸查找匹配地块。
	 * Finds a land in the given world that matches the house size type.
	 *
	 * entry world id
	 *
	 * @param houseSize 房屋类型尺寸 / house size type
	 * @param houseSize
	 * @return 匹配地块，不存在则为 null / matching land, or null if absent
	 */
	public HousingLand getLandForHouse(int worldId, HouseType houseSize) {
		Set<HousingLand> worldHouseAreas = landsByEntryWorldId.get(worldId);
		if (worldHouseAreas == null) {
			return null;
		}
		for (HousingLand land : worldHouseAreas) {
			for (Building building : land.getBuildings()) {
				if (houseSize.value().equals(building.getSize())) {
					return land;
				}
			}
		}
		return null;
	}

	/**
	 * 按地块 ID 获取地块模板。
	 * Returns the land template for the given land id.
	 *
	 * land id
	 *
	 * @param landId
	 * @return 地块模板，不存在则为 null / land template, or null if absent
	 */
	public HousingLand getLand(int landId) {
		return landsById.get(landId);
	}

	/**
	 * 返回全部地块集合。
	 * Returns all land templates.
	 *
	 * all lands
	 */
	public Collection<HousingLand> getLands() {
		return landsById.values();
	}

	/**
	 * 返回地块数量。
	 * Returns the number of lands.
	 *
	 * land count
	 */
	public int size() {
		return landsById.size();
	}
}
