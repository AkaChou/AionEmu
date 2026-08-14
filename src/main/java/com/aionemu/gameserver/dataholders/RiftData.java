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

import com.aionemu.gameserver.model.rift.RiftLocation;
import com.aionemu.gameserver.model.templates.rift.RiftTemplate;

/**
 * 裂隙据点数据容器，按 ID 索引 RiftLocation。
 * Rift location data holder, indexed by id.
 *
 * @author Source
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "rift_locations")
public class RiftData {

	@XmlElement(name = "rift_location")
	private List<RiftTemplate> riftTemplates;
	@XmlTransient
	private Map<Integer, RiftLocation> rift = new LinkedHashMap<Integer, RiftLocation>();

	/**
	 * JAXB 反序列化完成后，将模板包装为裂隙据点并写入索引。
	 * After JAXB unmarshalling, wraps templates into rift locations and indexes them.
	 */
	void afterUnmarshal(Unmarshaller u, Object parent) {
		for (RiftTemplate template : riftTemplates) {
			rift.put(template.getId(), new RiftLocation(template));
		}
	}

	/**
	 * 返回已加载的裂隙据点数量。
	 * Returns the number of loaded rift locations.
	 *
	 * @return 已加载的裂缝地点数量 / Returns the number of loaded rift locations.
	 */
	public int size() {
		return rift.size();
	}

	/**
	 * 返回全部裂隙据点映射。
	 * Returns the full rift location map.
	 *
	 * @return ID 到据点的映射 / map of id to location
	 */
	public Map<Integer, RiftLocation> getRiftLocations() {
		return rift;
	}
}
