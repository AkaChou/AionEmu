package com.aionemu.gameserver.dataholders;

import java.util.List;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;

import com.aionemu.gameserver.model.dynamicrift.DynamicRiftLocation;
import com.aionemu.gameserver.model.templates.dynamicrift.DynamicRiftTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 动态裂隙配置数据容器，维护裂隙地点模板与运行时地点映射。
 * Dynamic rift configuration data holder for rift location templates and runtime locations.
 *
 * @author Rinzler (Encom)
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "dynamic_rift")
public class DynamicRiftData {
	@XmlElement(name = "dynamic_location")
	private List<DynamicRiftTemplate> dynamicRiftTemplates;

	@XmlTransient
	private Map<Integer, DynamicRiftLocation> dynamicRift = new LinkedHashMap<Integer, DynamicRiftLocation>();

	/**
	 * JAXB 反序列化完成后，将模板转换为地点实例并写入索引。
	 * After JAXB unmarshalling, converts templates into location instances and indexes them.
	 */
	void afterUnmarshal(Unmarshaller u, Object parent) {
		for (DynamicRiftTemplate template : dynamicRiftTemplates) {
			dynamicRift.put(template.getId(), new DynamicRiftLocation(template));
		}
	}

	/**
	 * 返回动态裂隙地点数量。
	 * Returns the number of dynamic rift locations.
	 *
	 * @return 动态裂缝地点数量 / Returns the number of dynamic rift locations.
	 */
	public int size() {
		return dynamicRift.size();
	}

	/**
	 * 返回全部动态裂隙地点映射。
	 * Returns the map of all dynamic rift locations.
	 *
	 * @return 地点 ID 到地点实例的映射 / map of location id to location instance
	 */
	public Map<Integer, DynamicRiftLocation> getDynamicRiftLocations() {
		return dynamicRift;
	}
}
