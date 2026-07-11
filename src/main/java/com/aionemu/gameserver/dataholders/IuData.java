package com.aionemu.gameserver.dataholders;

import java.util.List;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;

import com.aionemu.gameserver.model.iu.IuLocation;
import com.aionemu.gameserver.model.templates.iu.IuTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * IU 活动地点数据容器，持有并索引全部 {@link IuLocation}。
 * IU event location data holder, indexing all {@link IuLocation} instances.
 *
 * @author Rinzler (Encom)
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "iu")
public class IuData {
	@XmlElement(name = "iu_location")
	private List<IuTemplate> iuTemplates;

	@XmlTransient
	private Map<Integer, IuLocation> iu = new LinkedHashMap<Integer, IuLocation>();

	/**
	 * JAXB 反序列化完成后，将模板转为按 ID 索引的地点映射。
	 * After JAXB unmarshalling, builds the id-indexed location map from templates.
	 */
	void afterUnmarshal(Unmarshaller u, Object parent) {
		for (IuTemplate template : iuTemplates) {
			iu.put(template.getId(), new IuLocation(template));
		}
	}

	/**
	 * 返回已加载的 IU 地点数量。
	 * Returns the number of loaded IU locations.
	 *
	 * location count
	 */
	public int size() {
		return iu.size();
	}

	/**
	 * 返回全部 IU 地点映射。
	 * Returns the full IU location map.
	 *
	 * @return ID 到地点的映射 / map of id to location
	 */
	public Map<Integer, IuLocation> getIuLocations() {
		return iu;
	}
}
