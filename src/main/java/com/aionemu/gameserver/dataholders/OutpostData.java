package com.aionemu.gameserver.dataholders;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;

import com.aionemu.gameserver.model.outpost.OutpostLocation;
import com.aionemu.gameserver.model.templates.outpost.OutpostTemplate;

/**
 * 前哨据点数据容器，按 ID 索引 OutpostLocation。
 * Outpost location data holder, indexed by id.
 *
 * Created by Wnkrz on 27/08/2017.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "outpost_locations")
public class OutpostData {
	@XmlElement(name = "outpost_location")
	private List<OutpostTemplate> outpostTemplates;
	@XmlTransient
	private Map<Integer, OutpostLocation> out = new LinkedHashMap<Integer, OutpostLocation>();

	/**
	 * JAXB 反序列化完成后，将模板包装为据点实例并写入索引。
	 * After JAXB unmarshalling, wraps templates into location instances and indexes them.
	 */
	void afterUnmarshal(Unmarshaller u, Object parent) {
		for (OutpostTemplate template : outpostTemplates) {
			out.put(template.getId(), new OutpostLocation(template));
		}
	}

	/**
	 * 返回已加载的前哨据点数量。
	 * Returns the number of loaded outpost locations.
	 *
	 * location count
	 */
	public int size() {
		return out.size();
	}

	/**
	 * 返回全部前哨据点映射。
	 * Returns the full outpost location map.
	 *
	 * @return ID 到据点的映射 / map of id to location
	 */
	public Map<Integer, OutpostLocation> getOutpostLocations() {
		return out;
	}
}
