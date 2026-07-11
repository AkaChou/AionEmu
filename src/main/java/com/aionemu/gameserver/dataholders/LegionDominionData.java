package com.aionemu.gameserver.dataholders;

import java.util.List;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;

import com.aionemu.gameserver.model.legiondominion.LegionDominionLocation;
import com.aionemu.gameserver.model.templates.legiondominion.LegionDominionTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 军团领地地点数据容器，持有并索引全部 {@link LegionDominionLocation}。
 * Legion dominion location data holder, indexing all {@link LegionDominionLocation} instances.
 *
 * @author Rinzler (Encom)
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "dominion_locations")
public class LegionDominionData {
	@XmlElement(name = "dominion_location")
	private List<LegionDominionTemplate> legionDominionTemplates;

	@XmlTransient
	private Map<Integer, LegionDominionLocation> legionDominion = new LinkedHashMap<Integer, LegionDominionLocation>();

	/**
	 * JAXB 反序列化完成后，将模板转为按领地 ID 索引的地点映射。
	 * After JAXB unmarshalling, builds the dominion-id-indexed location map from templates.
	 */
	void afterUnmarshal(Unmarshaller u, Object parent) {
		for (LegionDominionTemplate template : legionDominionTemplates) {
			legionDominion.put(template.getLegionDominionId(), new LegionDominionLocation(template));
		}
	}

	/**
	 * 返回已加载的军团领地数量。
	 * Returns the number of loaded legion dominion locations.
	 *
	 * location count
	 */
	public int size() {
		return legionDominion.size();
	}

	/**
	 * 返回全部军团领地地点映射。
	 * Returns the full legion dominion location map.
	 *
	 * @return 领地 ID 到地点的映射 / map of dominion id to location
	 */
	public Map<Integer, LegionDominionLocation> getLegionDominionLocations() {
		return legionDominion;
	}
}
