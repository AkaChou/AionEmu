package com.aionemu.gameserver.dataholders;

import java.util.List;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;

import com.aionemu.gameserver.model.beritra.BeritraLocation;
import com.aionemu.gameserver.model.templates.beritra.BeritraTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 贝里特拉入侵地点数据容器，持有并索引全部 Beritra 地点。
 * Beritra invasion location data holder, indexing all beritra locations.
 *
 * @author Rinzler (Encom)
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "beritra_invasion")
public class BeritraData {
	@XmlElement(name = "beritra_location")
	private List<BeritraTemplate> beritraTemplates;

	@XmlTransient
	private Map<Integer, BeritraLocation> beritra = new LinkedHashMap<Integer, BeritraLocation>();

	/**
	 * JAXB 反序列化完成后，将模板转为运行时地点并按 ID 索引。
	 * After JAXB unmarshalling, converts templates to runtime locations indexed by id.
	 */
	void afterUnmarshal(Unmarshaller u, Object parent) {
		for (BeritraTemplate template : beritraTemplates) {
			beritra.put(template.getId(), new BeritraLocation(template));
		}
	}

	/**
	 * 返回已加载的地点数量。
	 * Returns the number of loaded locations.
	 *
	 * @return 已加载的地点数量 / Returns the number of loaded locations.
	 */
	public int size() {
		return beritra.size();
	}

	/**
	 * 返回全部 Beritra 地点映射。
	 * Returns the full beritra location map.
	 *
	 * @return ID 到地点的映射 / map of id to location
	 */
	public Map<Integer, BeritraLocation> getBeritraLocations() {
		return beritra;
	}
}
