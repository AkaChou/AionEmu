package com.aionemu.gameserver.dataholders;

import java.util.List;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;

import com.aionemu.gameserver.model.base.BaseLocation;
import com.aionemu.gameserver.model.templates.base.BaseTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 据点地点数据容器，持有并索引全部据点地点。
 * Base location data holder, indexing all base locations.
 *
 * @author Rinzler
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "base_locations")
public class BaseData {
	@XmlElement(name = "base_location")
	private List<BaseTemplate> baseTemplates;
	@XmlTransient
	private Map<Integer, BaseLocation> base = new LinkedHashMap<Integer, BaseLocation>();

	/**
	 * JAXB 反序列化完成后，将模板转为运行时地点并按 ID 索引。
	 * After JAXB unmarshalling, converts templates to runtime locations indexed by id.
	 */
	void afterUnmarshal(Unmarshaller u, Object parent) {
		for (BaseTemplate template : baseTemplates) {
			base.put(template.getId(), new BaseLocation(template));
		}
	}

	/**
	 * 返回已加载的据点数量。
	 * Returns the number of loaded bases.
	 *
	 * location count
	 */
	public int size() {
		return base.size();
	}

	/**
	 * 返回全部据点地点映射。
	 * Returns the full base location map.
	 *
	 * @return ID 到地点的映射 / map of id to location
	 */
	public Map<Integer, BaseLocation> getBaseLocations() {
		return base;
	}
}
