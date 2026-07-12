package com.aionemu.gameserver.dataholders;

import java.util.List;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.aionemu.gameserver.model.templates.BindPointTemplate;

import com.aionemu.commons.utils.collections.IntObjectHashMap;

/**
 * 绑定点数据容器，按 NPC ID 索引绑定点模板。
 * Bind-point data holder, indexing bind-point templates by NPC id.
 *
 * @author avol
 */
@XmlRootElement(name = "bind_points")
@XmlAccessorType(XmlAccessType.FIELD)
public class BindPointData {

	@XmlElement(name = "bind_point")
	private List<BindPointTemplate> bplist;

	/** 绑定点模板索引 / bind-point template index */
	private IntObjectHashMap<BindPointTemplate> bindplistData = new IntObjectHashMap<BindPointTemplate>();

	/**
	 * JAXB 反序列化完成后，按 NPC ID 建立索引。
	 * After JAXB unmarshalling, indexes templates by NPC id.
	 */
	void afterUnmarshal(Unmarshaller u, Object parent) {
		for (BindPointTemplate bind : bplist) {
			bindplistData.put(bind.getNpcId(), bind);
		}
	}

	/**
	 * 返回已加载的绑定点数量。
	 * Returns the number of loaded bind points.
	 *
	 * template count
	 */
	public int size() {
		return bindplistData.size();
	}

	/**
	 * 按 NPC ID 获取绑定点模板。
	 * Returns the bind-point template for the given NPC id.
	 *
	 * npc id
	 *
	 * @param npcId
	 * @return 模板，不存在则为 null / template or null
	 */
	public BindPointTemplate getBindPointTemplate(int npcId) {
		return bindplistData.get(npcId);
	}
}
