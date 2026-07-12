package com.aionemu.gameserver.dataholders;

import java.util.List;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.aionemu.gameserver.model.templates.CubeExpandTemplate;

import com.aionemu.commons.utils.collections.IntObjectHashMap;

/**
 * 背包扩展 NPC 数据容器，按 NPC ID 索引扩展模板。
 * Cube expander data holder, indexing expand templates by NPC id.
 *
 * @author dragoon112
 */
@XmlRootElement(name = "cube_expander")
@XmlAccessorType(XmlAccessType.FIELD)
public class CubeExpandData {

	@XmlElement(name = "cube_npc")
	private List<CubeExpandTemplate> clist;
	private IntObjectHashMap<CubeExpandTemplate> npctlistData = new IntObjectHashMap<CubeExpandTemplate>();

	/**
	 * JAXB 反序列化完成后，按 NPC ID 建立索引。
	 * After JAXB unmarshalling, indexes templates by NPC id.
	 */
	void afterUnmarshal(Unmarshaller u, Object parent) {
		for (CubeExpandTemplate npc : clist) {
			npctlistData.put(npc.getNpcId(), npc);
		}
	}

	/**
	 * 返回已加载的扩展 NPC 数量。
	 * Returns the number of loaded expander NPCs.
	 *
	 * template count
	 */
	public int size() {
		return npctlistData.size();
	}

	/**
	 * 按 NPC ID 获取背包扩展模板。
	 * Returns the cube expand template for the given NPC id.
	 *
	 * npc id
	 *
	 * @param id
	 * @return 模板，不存在则为 null / template or null
	 */
	public CubeExpandTemplate getCubeExpandListTemplate(int id) {
		return npctlistData.get(id);
	}
}
