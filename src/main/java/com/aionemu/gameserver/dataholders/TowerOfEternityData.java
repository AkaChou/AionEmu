package com.aionemu.gameserver.dataholders;

import java.util.List;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;

import com.aionemu.gameserver.model.templates.towerofeternity.TowerOfEternityTemplate;
import com.aionemu.gameserver.model.towerofeternity.TowerOfEternityLocation;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 永恒之塔静态数据容器，按位置 ID 索引塔点位。
 * Tower of Eternity static-data holder, indexing tower locations by id.
 *
 * @author Wnkrz
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "tower_of_eternity")
public class TowerOfEternityData {
	@XmlElement(name = "tower_location")
	private List<TowerOfEternityTemplate> towerOfEternityTemplates;

	@XmlTransient
	private Map<Integer, TowerOfEternityLocation> towerOfEternity = new LinkedHashMap<Integer, TowerOfEternityLocation>();

	/**
	 * JAXB 反序列化完成后，将模板索引为塔点位映射。
	 * After JAXB unmarshalling, indexes templates as tower locations.
	 */
	void afterUnmarshal(Unmarshaller u, Object parent) {
		for (TowerOfEternityTemplate template : towerOfEternityTemplates) {
			towerOfEternity.put(template.getId(), new TowerOfEternityLocation(template));
		}
	}

	/**
	 * 返回已加载的塔点位数量。
	 * Returns the number of loaded tower locations.
	 *
	 * @return 已加载的塔地点数量 / Returns the number of loaded tower locations.
	 */
	public int size() {
		return towerOfEternity.size();
	}

	/**
	 * 返回全部永恒之塔点位映射。
	 * Returns the full map of Tower of Eternity locations.
	 *
	 * @return 完整的永恒之塔地点映射 / Returns the full map of Tower of Eternity locations.
	 */
	public Map<Integer, TowerOfEternityLocation> getTowerOfEternityLocations() {
		return towerOfEternity;
	}
}
