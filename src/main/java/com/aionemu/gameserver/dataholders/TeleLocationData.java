package com.aionemu.gameserver.dataholders;

import java.util.List;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.aionemu.gameserver.model.templates.teleport.TelelocationTemplate;

import com.aionemu.commons.utils.collections.IntObjectHashMap;

/**
 * 传送点位置数据容器，按位置 ID 索引传送坐标模板。
 * Teleport-location data holder, indexing telelocation templates by location id.
 *
 * @author orz
 */
@XmlRootElement(name = "teleport_location")
@XmlAccessorType(XmlAccessType.FIELD)
public class TeleLocationData {

	@XmlElement(name = "teleloc_template")
	private List<TelelocationTemplate> tlist;

	/** 传送点位置模板映射。 / Map of all teleport location templates. */
	private IntObjectHashMap<TelelocationTemplate> loctlistData = new IntObjectHashMap<TelelocationTemplate>();

	/**
	 * JAXB 反序列化完成后，按位置 ID 索引传送点模板。
	 * After JAXB unmarshalling, indexes telelocation templates by location id.
	 */
	void afterUnmarshal(Unmarshaller u, Object parent) {
		for (TelelocationTemplate loc : tlist) {
			loctlistData.put(loc.getLocId(), loc);
		}
	}

	/**
	 * 返回已加载的传送点数量。
	 * Returns the number of loaded teleport locations.
	 *
	 * @return 已加载的传送地点数量 / Returns the number of loaded teleport locations.
	 */
	public int size() {
		return loctlistData.size();
	}

	/**
	 * 按位置 ID 获取传送点模板。
	 * Returns the telelocation template for the given location id.
	 *
	 * @param id 位置 ID / location id
	 * @return 传送点模板，不存在则为 null / telelocation template or null
	 */
	public TelelocationTemplate getTelelocationTemplate(int id) {
		return loctlistData.get(id);
	}
}
