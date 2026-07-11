package com.aionemu.gameserver.dataholders;

import java.util.List;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;

import com.aionemu.gameserver.model.landing_special.LandingSpecialLocation;
import com.aionemu.gameserver.model.templates.landing_special.LandingSpecialTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 特殊登陆据点数据容器，持有并索引全部 {@link LandingSpecialLocation}。
 * Special landing location data holder, indexing all {@link LandingSpecialLocation} instances.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "landing_special")
public class LandingSpecialData {
	@XmlElement(name = "landing_special_location")
	private List<LandingSpecialTemplate> landingSpecialTemplates;

	@XmlTransient
	private Map<Integer, LandingSpecialLocation> landingSpecial = new LinkedHashMap<Integer, LandingSpecialLocation>();

	/**
	 * JAXB 反序列化完成后，将模板转为按 ID 索引的地点映射。
	 * After JAXB unmarshalling, builds the id-indexed location map from templates.
	 */
	void afterUnmarshal(Unmarshaller u, Object parent) {
		for (LandingSpecialTemplate template : landingSpecialTemplates) {
			landingSpecial.put(template.getId(), new LandingSpecialLocation(template));
		}
	}

	/**
	 * 返回已加载的特殊登陆据点数量。
	 * Returns the number of loaded special landing locations.
	 *
	 * location count
	 */
	public int size() {
		return landingSpecial.size();
	}

	/**
	 * 返回全部特殊登陆据点映射。
	 * Returns the full special landing location map.
	 *
	 * @return ID 到地点的映射 / map of id to location
	 */
	public Map<Integer, LandingSpecialLocation> getLandingSpecialLocations() {
		return landingSpecial;
	}
}
