package com.aionemu.gameserver.dataholders;

import java.util.List;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;

import com.aionemu.gameserver.model.conquest.ConquestLocation;
import com.aionemu.gameserver.model.templates.conquest.ConquestTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 征服地点数据容器，持有并索引全部征服地点。
 * Conquest location data holder, indexing all conquest locations.
 *
 * @author Rinzler (Encom)
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "conquest")
public class ConquestData {
	@XmlElement(name = "conquest_location")
	private List<ConquestTemplate> conquestTemplates;

	@XmlTransient
	private Map<Integer, ConquestLocation> conquest = new LinkedHashMap<Integer, ConquestLocation>();

	/**
	 * JAXB 反序列化完成后，将模板转为运行时地点并按 ID 索引。
	 * After JAXB unmarshalling, converts templates to runtime locations indexed by id.
	 */
	void afterUnmarshal(Unmarshaller u, Object parent) {
		for (ConquestTemplate template : conquestTemplates) {
			conquest.put(template.getId(), new ConquestLocation(template));
		}
	}

	/**
	 * 返回已加载的地点数量。
	 * Returns the number of loaded locations.
	 *
	 * @return 已加载的地点数量 / Returns the number of loaded locations.
	 */
	public int size() {
		return conquest.size();
	}

	/**
	 * 返回全部征服地点映射。
	 * Returns the full conquest location map.
	 *
	 * @return ID 到地点的映射 / map of id to location
	 */
	public Map<Integer, ConquestLocation> getConquestLocations() {
		return conquest;
	}
}
