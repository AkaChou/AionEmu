package com.aionemu.gameserver.dataholders;

import java.util.List;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;

import com.aionemu.gameserver.model.svs.SvsLocation;
import com.aionemu.gameserver.model.templates.svs.SvsTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * SVS 活动数据容器，按位置 ID 索引 SVS 地点实例。
 * SVS event data holder, indexing SVS location instances by location id.
 *
 * @author Rinzler (Encom)
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "svs")
public class SvsData {
	@XmlElement(name = "svs_location")
	private List<SvsTemplate> svsTemplates;

	@XmlTransient
	private Map<Integer, SvsLocation> svs = new LinkedHashMap<Integer, SvsLocation>();

	/**
	 * JAXB 反序列化完成后，将模板实例化为 SVS 地点并建立索引。
	 * After JAXB unmarshalling, instantiates SVS locations from templates and indexes them.
	 */
	void afterUnmarshal(Unmarshaller u, Object parent) {
		for (SvsTemplate template : svsTemplates) {
			svs.put(template.getId(), new SvsLocation(template));
		}
	}

	/**
	 * 返回已加载的 SVS 地点数量。
	 * Returns the number of loaded SVS locations.
	 *
	 * location count
	 */
	public int size() {
		return svs.size();
	}

	/**
	 * 返回全部 SVS 地点映射。
	 * Returns the full map of SVS locations.
	 *
	 * @return 位置 ID → SVS 地点映射 / location-id to SVS-location map
	 */
	public Map<Integer, SvsLocation> getSvsLocations() {
		return svs;
	}
}
