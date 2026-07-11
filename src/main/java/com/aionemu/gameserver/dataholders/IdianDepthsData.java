package com.aionemu.gameserver.dataholders;

import java.util.List;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;

import com.aionemu.gameserver.model.idiandepths.IdianDepthsLocation;
import com.aionemu.gameserver.model.templates.idiandepths.IdianDepthsTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 伊迪安深渊地点数据容器，持有并索引全部 {@link IdianDepthsLocation}。
 * Container holding and indexing all {@link IdianDepthsLocation} instances.
 *
 * @author Rinzler (Encom)
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "idian_depths")
public class IdianDepthsData {
	@XmlElement(name = "idian_location")
	private List<IdianDepthsTemplate> idianDepthsTemplates;

	@XmlTransient
	private Map<Integer, IdianDepthsLocation> idianDepths = new LinkedHashMap<Integer, IdianDepthsLocation>();

	/**
	 * JAXB 反序列化完成后，将模板转为按 ID 索引的地点映射。
	 * After JAXB unmarshalling, builds the id-indexed location map from templates.
	 */
	void afterUnmarshal(Unmarshaller u, Object parent) {
		for (IdianDepthsTemplate template : idianDepthsTemplates) {
			idianDepths.put(template.getId(), new IdianDepthsLocation(template));
		}
	}

	/**
	 * 返回已加载的伊迪安深渊地点数量。
	 * Returns the number of loaded Idian Depths locations.
	 *
	 * location count
	 */
	public int size() {
		return idianDepths.size();
	}

	/**
	 * 返回全部伊迪安深渊地点映射。
	 * Returns the full Idian Depths location map.
	 *
	 * @return ID 到地点的映射 / map of id to location
	 */
	public Map<Integer, IdianDepthsLocation> getIdianDepthsLocations() {
		return idianDepths;
	}
}
