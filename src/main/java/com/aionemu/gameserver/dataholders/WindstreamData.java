package com.aionemu.gameserver.dataholders;

import java.util.List;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.templates.windstreams.WindstreamTemplate;

import com.aionemu.commons.utils.collections.IntObjectHashMap;

/**
 * 风道数据容器，按地图 ID 索引风道模板。
 * Windstream data holder, indexing windstream templates by map id.
 *
 * @author LokiReborn
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "")
@XmlRootElement(name = "windstreams")
public class WindstreamData {
	@XmlElement(name = "windstream")

	private List<WindstreamTemplate> wts;
	private IntObjectHashMap<WindstreamTemplate> windstreams;

	/**
	 * JAXB 反序列化完成后，将风道模板按地图 ID 建索引并释放列表。
	 * After JAXB unmarshalling, indexes windstream templates by map id and clears the list.
	 */
	void afterUnmarshal(Unmarshaller u, Object parent) {

		windstreams = new IntObjectHashMap<WindstreamTemplate>();
		for (WindstreamTemplate wt : wts) {
			windstreams.put(wt.getMapid(), wt);
		}
		wts = null;
	}

	/**
	 * 按地图 ID 获取风道模板。
	 * Returns the windstream template for the given map id.
	 *
	 * map id
	 *
	 * @param mapId
	 * @return 风道模板，不存在则为 null / windstream template or null
	 */
	public WindstreamTemplate getStreamTemplate(int mapId) {
		return windstreams.get(mapId);
	}

	/**
	 * 返回已加载的风道模板数量。
	 * Returns the number of loaded windstream templates.
	 *
	 * template count
	 */
	public int size() {
		return windstreams.size();
	}
}
