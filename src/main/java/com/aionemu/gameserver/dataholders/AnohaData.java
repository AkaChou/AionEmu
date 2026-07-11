package com.aionemu.gameserver.dataholders;

import java.util.List;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;

import com.aionemu.gameserver.model.anoha.AnohaLocation;
import com.aionemu.gameserver.model.templates.anoha.AnohaTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 阿诺哈活动地点数据容器，持有并索引全部 Anoha 地点。
 * Anoha event location data holder, indexing all anoha locations.
 *
 * @author Rinzler (Encom)
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "anoha")
public class AnohaData {
	@XmlElement(name = "anoha_location")
	private List<AnohaTemplate> anohaTemplates;

	@XmlTransient
	private Map<Integer, AnohaLocation> anoha = new LinkedHashMap<Integer, AnohaLocation>();

	/**
	 * JAXB 反序列化完成后，将模板转为运行时地点并按 ID 索引。
	 * After JAXB unmarshalling, converts templates to runtime locations indexed by id.
	 */
	void afterUnmarshal(Unmarshaller u, Object parent) {
		for (AnohaTemplate template : anohaTemplates) {
			anoha.put(template.getId(), new AnohaLocation(template));
		}
	}

	/**
	 * 返回已加载的地点数量。
	 * Returns the number of loaded locations.
	 *
	 * location count
	 */
	public int size() {
		return anoha.size();
	}

	/**
	 * 返回全部 Anoha 地点映射。
	 * Returns the full anoha location map.
	 *
	 * @return ID 到地点的映射 / map of id to location
	 */
	public Map<Integer, AnohaLocation> getAnohaLocations() {
		return anoha;
	}
}
