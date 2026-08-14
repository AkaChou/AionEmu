package com.aionemu.gameserver.dataholders;

import java.util.List;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.aionemu.gameserver.model.templates.factions.NpcFactionTemplate;

import com.aionemu.commons.utils.collections.IntObjectHashMap;

/**
 * NPC 势力数据容器，按势力 ID 与关联 NPC ID 索引 {@link NpcFactionTemplate}。
 * NPC faction data holder, indexing {@link NpcFactionTemplate} by faction id and related npc id.
 *
 * @author vlog
 */
@XmlRootElement(name = "npc_factions")
@XmlAccessorType(XmlAccessType.FIELD)
public class NpcFactionsData {

	@XmlElement(name = "npc_faction", required = true)
	protected List<NpcFactionTemplate> npcFactionsData;
	private IntObjectHashMap<NpcFactionTemplate> factionsById = new IntObjectHashMap<NpcFactionTemplate>();
	private IntObjectHashMap<NpcFactionTemplate> factionsByNpcId = new IntObjectHashMap<NpcFactionTemplate>();

	/**
	 * JAXB 反序列化完成后，按势力 ID 与 NPC ID 建立索引。
	 * After JAXB unmarshalling, indexes factions by faction id and npc id.
	 */
	void afterUnmarshal(Unmarshaller u, Object parent) {
		factionsById.clear();
		for (NpcFactionTemplate template : npcFactionsData) {
			factionsById.put(template.getId(), template);
			if (template.getNpcId() != 0) {
				factionsByNpcId.put(template.getNpcId(), template);
			}
		}
	}

	/**
	 * 按势力 ID 获取势力模板。
	 * Returns the faction template for the given faction id.
	 *
	 * @param id 势力 ID / faction id
	 * @return 势力模板或 null / faction template or null
	 */
	public NpcFactionTemplate getNpcFactionById(int id) {
		return factionsById.get(id);
	}

	/**
	 * 按关联 NPC ID 获取势力模板。
	 * Returns the faction template associated with the given npc id.
	 *
	 * @param id NPC ID / npc id
	 * @return 势力模板或 null / faction template or null
	 */
	public NpcFactionTemplate getNpcFactionByNpcId(int id) {
		return factionsByNpcId.get(id);
	}

	/**
	 * 返回原始势力模板列表。
	 * Returns the raw faction template list.
	 *
	 * @return 势力模板列表 / faction template list
	 */
	public List<NpcFactionTemplate> getNpcFactionsData() {
		return npcFactionsData;
	}

	/**
	 * 返回已加载的势力模板数量。
	 * Returns the number of loaded faction templates.
	 *
	 * @return 已加载的阵营模板数量 / Returns the number of loaded faction templates.
	 */
	public int size() {
		return npcFactionsData.size();
	}
}
