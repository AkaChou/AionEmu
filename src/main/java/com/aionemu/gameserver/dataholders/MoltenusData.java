package com.aionemu.gameserver.dataholders;

import java.util.List;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;

import com.aionemu.gameserver.model.moltenus.MoltenusLocation;
import com.aionemu.gameserver.model.templates.moltenus.MoltenusTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 熔岩领主（Moltenus）活动地点数据容器，持有并索引全部 {@link MoltenusLocation}。
 * Moltenus event location data holder, indexing all {@link MoltenusLocation} instances.
 *
 * @author Rinzler (Encom)
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "moltenus")
public class MoltenusData {
	@XmlElement(name = "moltenus_location")
	private List<MoltenusTemplate> moltenusTemplates;

	@XmlTransient
	private Map<Integer, MoltenusLocation> moltenus = new LinkedHashMap<Integer, MoltenusLocation>();

	/**
	 * JAXB 反序列化完成后，将模板转为按 ID 索引的地点映射。
	 * After JAXB unmarshalling, builds the id-indexed location map from templates.
	 */
	void afterUnmarshal(Unmarshaller u, Object parent) {
		for (MoltenusTemplate template : moltenusTemplates) {
			moltenus.put(template.getId(), new MoltenusLocation(template));
		}
	}

	/**
	 * 返回已加载的 Moltenus 地点数量。
	 * Returns the number of loaded Moltenus locations.
	 *
	 * location count
	 */
	public int size() {
		return moltenus.size();
	}

	/**
	 * 返回全部 Moltenus 地点映射。
	 * Returns the full Moltenus location map.
	 *
	 * @return ID 到地点的映射 / map of id to location
	 */
	public Map<Integer, MoltenusLocation> getMoltenusLocations() {
		return moltenus;
	}
}
