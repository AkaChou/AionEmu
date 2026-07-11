package com.aionemu.gameserver.dataholders;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;

import com.aionemu.gameserver.model.templates.vortex.VortexTemplate;
import com.aionemu.gameserver.model.vortex.VortexLocation;

/**
 * 次元漩涡静态数据容器，按点位 ID 与入侵世界 ID 索引漩涡位置。
 * Dimensional vortex static-data holder, indexing vortex locations by id and invasion world id.
 *
 * @author Source
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "dimensional_vortex")
public class VortexData {

	@XmlElement(name = "vortex_location")
	private List<VortexTemplate> vortexTemplates;
	@XmlTransient
	private Map<Integer, VortexLocation> vortex = new LinkedHashMap<Integer, VortexLocation>();
	@XmlTransient
	private Map<Integer, VortexLocation> vortexByInvasionWorldId = new HashMap<Integer, VortexLocation>();

	/**
	 * JAXB 反序列化完成后，将模板索引为漩涡点位映射。
	 * After JAXB unmarshalling, indexes templates as vortex locations.
	 */
	void afterUnmarshal(Unmarshaller u, Object parent) {
		for (VortexTemplate template : vortexTemplates) {
			VortexLocation location = new VortexLocation(template);
			vortex.put(template.getId(), location);
			vortexByInvasionWorldId.putIfAbsent(location.getInvasionWorldId(), location);
		}
	}

	/**
	 * 返回已加载的漩涡点位数量。
	 * Returns the number of loaded vortex locations.
	 *
	 * location count
	 */
	public int size() {
		return vortex.size();
	}

	/**
	 * 按入侵世界 ID 获取漩涡点位。
	 * Returns the vortex location for the given invasion world id.
	 *
	 * invasion world id
	 *
	 * @param invasionWorldId @return 漩涡点位，不存在则为 null / vortex location or null
	 */
	public VortexLocation getVortexLocation(int invasionWorldId) {
		return vortexByInvasionWorldId.get(invasionWorldId);
	}

	/**
	 * 返回全部漩涡点位映射。
	 * Returns the full map of vortex locations.
	 *
	 * location map
	 */
	public Map<Integer, VortexLocation> getVortexLocations() {
		return vortex;
	}
}
