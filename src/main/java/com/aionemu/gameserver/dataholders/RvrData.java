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

import com.aionemu.gameserver.model.rvr.RvrLocation;
import com.aionemu.gameserver.model.templates.rvr.RvrTemplate;

/**
 * RVR（阵营对抗）据点数据容器，按 ID 索引 RvrLocation。
 * RVR (race vs race) location data holder, indexed by id.
 *
 * @author Rinzler (Encom)
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "rvr")
public class RvrData {
	@XmlElement(name = "rvr_location")
	private List<RvrTemplate> rvrTemplates;

	@XmlTransient
	private Map<Integer, RvrLocation> rvr = new LinkedHashMap<Integer, RvrLocation>();

	/**
	 * JAXB 反序列化完成后，将模板包装为 RVR 据点并写入索引。
	 * After JAXB unmarshalling, wraps templates into RVR locations and indexes them.
	 */
	void afterUnmarshal(Unmarshaller u, Object parent) {
		for (RvrTemplate template : rvrTemplates) {
			rvr.put(template.getId(), new RvrLocation(template));
		}
	}

	/**
	 * 返回已加载的 RVR 据点数量。
	 * Returns the number of loaded RVR locations.
	 *
	 * location count
	 */
	public int size() {
		return rvr.size();
	}

	/**
	 * 返回全部 RVR 据点映射。
	 * Returns the full RVR location map.
	 *
	 * @return ID 到据点的映射 / map of id to location
	 */
	public Map<Integer, RvrLocation> getRvrLocations() {
		return rvr;
	}
}
