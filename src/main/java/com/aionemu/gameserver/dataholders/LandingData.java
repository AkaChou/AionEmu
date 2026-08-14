package com.aionemu.gameserver.dataholders;

import java.util.List;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;

import com.aionemu.gameserver.model.landing.LandingLocation;
import com.aionemu.gameserver.model.templates.landing.LandingTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 登陆据点数据容器，持有并索引全部 {@link LandingLocation}。
 * Landing location data holder, indexing all {@link LandingLocation} instances.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "landing")
public class LandingData {
	@XmlElement(name = "landing_location")
	private List<LandingTemplate> landingTemplates;

	@XmlTransient
	private Map<Integer, LandingLocation> landing = new LinkedHashMap<Integer, LandingLocation>();

	/**
	 * JAXB 反序列化完成后，将模板转为按 ID 索引的地点映射。
	 * After JAXB unmarshalling, builds the id-indexed location map from templates.
	 */
	void afterUnmarshal(Unmarshaller u, Object parent) {
		for (LandingTemplate template : landingTemplates) {
			landing.put(template.getId(), new LandingLocation(template));
		}
	}

	/**
	 * 返回已加载的登陆据点数量。
	 * Returns the number of loaded landing locations.
	 *
	 * @return 已加载的着陆点数量 / Returns the number of loaded landing locations.
	 */
	public int size() {
		return landing.size();
	}

	/**
	 * 返回全部登陆据点映射。
	 * Returns the full landing location map.
	 *
	 * @return ID 到地点的映射 / map of id to location
	 */
	public Map<Integer, LandingLocation> getLandingLocations() {
		return landing;
	}
}
