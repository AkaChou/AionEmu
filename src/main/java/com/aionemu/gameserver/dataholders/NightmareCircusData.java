package com.aionemu.gameserver.dataholders;

import java.util.List;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;

import com.aionemu.gameserver.model.nightmarecircus.NightmareCircusLocation;
import com.aionemu.gameserver.model.templates.nightmarecircus.NightmareCircusTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 梦魇马戏团地点数据容器，持有并索引全部 {@link NightmareCircusLocation}。
 * Nightmare Circus location data holder, indexing all {@link NightmareCircusLocation} instances.
 *
 * @author Rinzler (Encom)
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "nightmare_circus")
public class NightmareCircusData {
	@XmlElement(name = "nightmare_location")
	private List<NightmareCircusTemplate> nightmareCircusTemplates;

	@XmlTransient
	private Map<Integer, NightmareCircusLocation> nightmareCircus = new LinkedHashMap<Integer, NightmareCircusLocation>();

	/**
	 * JAXB 反序列化完成后，将模板转为按 ID 索引的地点映射。
	 * After JAXB unmarshalling, builds the id-indexed location map from templates.
	 */
	void afterUnmarshal(Unmarshaller u, Object parent) {
		for (NightmareCircusTemplate template : nightmareCircusTemplates) {
			nightmareCircus.put(template.getId(), new NightmareCircusLocation(template));
		}
	}

	/**
	 * 返回已加载的梦魇马戏团地点数量。
	 * Returns the number of loaded Nightmare Circus locations.
	 *
	 * @return 已加载的噩梦马戏团地点数量 / Returns the number of loaded Nightmare Circus locations.
	 */
	public int size() {
		return nightmareCircus.size();
	}

	/**
	 * 返回全部梦魇马戏团地点映射。
	 * Returns the full Nightmare Circus location map.
	 *
	 * @return ID 到地点的映射 / map of id to location
	 */
	public Map<Integer, NightmareCircusLocation> getNightmareCircusLocations() {
		return nightmareCircus;
	}
}
