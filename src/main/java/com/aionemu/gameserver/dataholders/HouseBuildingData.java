package com.aionemu.gameserver.dataholders;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.templates.housing.Building;

/**
 * 房屋建筑配置数据容器，按建筑 ID 索引建筑模板。
 * House building configuration data holder, indexed by building id.
 *
 * @author Rolandas
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = { "buildings" })
public class HouseBuildingData {

	@XmlElement(name = "building")
	protected List<Building> buildings;

	@XmlTransient
	Map<Integer, Building> buildingById = new HashMap<Integer, Building>();

	/**
	 * JAXB 反序列化完成后，按建筑 ID 建立索引并释放原始列表。
	 * After JAXB unmarshalling, indexes buildings by id and releases the raw list.
	 */
	void afterUnmarshal(Unmarshaller u, Object parent) {
		if (buildings == null) {
			return;
		}
		for (Building building : buildings) {
			buildingById.put(building.getId(), building);
		}
		buildings.clear();
		buildings = null;
	}

	/**
	 * 按建筑 ID 获取建筑模板。
	 * Returns the building template for the given building id.
	 *
	 * building id
	 *
	 * @param buildingId @return 建筑模板，不存在则为 null / building template, or null if absent
	 */
	public Building getBuilding(int buildingId) {
		return buildingById.get(buildingId);
	}

	/**
	 * 返回建筑模板数量。
	 * Returns the number of building templates.
	 *
	 * template count
	 */
	public int size() {
		return buildingById.size();
	}
}
