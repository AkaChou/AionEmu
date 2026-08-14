package com.aionemu.gameserver.dataholders;

import java.util.List;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;

import com.aionemu.gameserver.model.instancerift.InstanceRiftLocation;
import com.aionemu.gameserver.model.templates.instancerift.InstanceRiftTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 副本裂隙地点数据容器，持有并索引全部 {@link InstanceRiftLocation}。
 * Container holding and indexing all {@link InstanceRiftLocation} instances.
 *
 * @author Rinzler (Encom)
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "instance_rift")
public class InstanceRiftData {
	@XmlElement(name = "instance_location")
	private List<InstanceRiftTemplate> instanceRiftTemplates;

	@XmlTransient
	private Map<Integer, InstanceRiftLocation> instanceRift = new LinkedHashMap<Integer, InstanceRiftLocation>();

	/**
	 * JAXB 反序列化完成后，将模板转为按 ID 索引的地点映射。
	 * After JAXB unmarshalling, builds the id-indexed location map from templates.
	 */
	void afterUnmarshal(Unmarshaller u, Object parent) {
		for (InstanceRiftTemplate template : instanceRiftTemplates) {
			instanceRift.put(template.getId(), new InstanceRiftLocation(template));
		}
	}

	/**
	 * 返回已加载的副本裂隙地点数量。
	 * Returns the number of loaded instance rift locations.
	 *
	 * @return 已加载的副本裂缝地点数量 / Returns the number of loaded instance rift locations.
	 */
	public int size() {
		return instanceRift.size();
	}

	/**
	 * 返回全部副本裂隙地点映射。
	 * Returns the full instance rift location map.
	 *
	 * @return ID 到地点的映射 / map of id to location
	 */
	public Map<Integer, InstanceRiftLocation> getInstanceRiftLocations() {
		return instanceRift;
	}
}
