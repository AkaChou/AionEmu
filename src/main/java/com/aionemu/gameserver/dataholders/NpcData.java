package com.aionemu.gameserver.dataholders;

import java.util.List;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.templates.npc.NpcTemplate;

import com.aionemu.commons.utils.collections.IntObjectHashMap;

/**
 * NPC 模板数据容器，持有并索引全部 {@link NpcTemplate}。
 * 每个 {@link Npc} 实例对应某一 NPC 类别，同类 NPC 共享同一 id、名称、物品与属性，
 * 其数据定义于 {@link NpcTemplate} 并以 NPC ID 唯一标识。
 * Container holding and serving all {@link NpcTemplate} instances.
 * Every {@link Npc} instance represents a class of NPCs sharing the same id, name,
 * items and statistics. That class data is defined in {@link NpcTemplate} and uniquely
 * identified by npc id.
 *
 * @author Luno
 */
@XmlRootElement(name = "npc_templates")
@XmlAccessorType(XmlAccessType.FIELD)
public class NpcData {

	@XmlElement(name = "npc_template")
	private List<NpcTemplate> npcs;

	/** 全部 NPC 模板映射 / map containing all npc templates */
	private IntObjectHashMap<NpcTemplate> npcData = new IntObjectHashMap<NpcTemplate>();

	/**
	 * JAXB 反序列化完成后，按模板 ID 建立索引并释放列表。
	 * After JAXB unmarshalling, indexes templates by id and clears the list.
	 */
	void afterUnmarshal(Unmarshaller u, Object parent) {
		for (NpcTemplate npc : npcs) {
			npcData.put(npc.getTemplateId(), npc);
		}
		npcs.clear();
		npcs = null;
	}

	/**
	 * 返回已加载的 NPC 模板数量。
	 * Returns the number of loaded NPC templates.
	 *
	 * @return 已加载的NPC 模板数量 / Returns the number of loaded NPC templates.
	 */
	public int size() {
		return npcData.size();
	}

	/**
	 * 按 ID 返回 NPC 模板。
	 * Returns the {@link NpcTemplate} for the given id.
	 *
	 * @param id NPC 模板 ID / npc template id
	 * @return 模板或 null / template or null
	 */
	public NpcTemplate getNpcTemplate(int id) {
		return npcData.get(id);
	}

	/**
	 * 返回全部 NPC 模板映射。
	 * Returns the full NPC template map.
	 *
	 * @return 模板 ID 到 NPC 模板的映射 / map of template id to NpcTemplate
	 */
	public IntObjectHashMap<NpcTemplate> getNpcData() {
		return npcData;
	}
}
