package com.aionemu.gameserver.dataholders;

import java.util.List;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.templates.npc.NpcTemplate;
import com.aionemu.gameserver.model.templates.teleport.TeleporterTemplate;

import com.aionemu.commons.utils.collections.IntObjectHashMap;

/**
 * 传送 NPC 数据容器，按传送 ID 索引传送员模板，并支持按 NPC ID 查找。
 * Teleporter data holder, indexing teleporter templates by teleport id and lookup by npc id.
 *
 * @author orz
 */
@XmlRootElement(name = "npc_teleporter")
@XmlAccessorType(XmlAccessType.FIELD)
public class TeleporterData {

	@XmlElement(name = "teleporter_template")
	private List<TeleporterTemplate> tlist;

	/** 商店交易列表映射。 / Map of all trade list templates. */
	private IntObjectHashMap<TeleporterTemplate> npctlistData = new IntObjectHashMap<TeleporterTemplate>();

	/**
	 * JAXB 反序列化完成后，按传送 ID 索引传送员模板。
	 * After JAXB unmarshalling, indexes teleporter templates by teleport id.
	 */
	void afterUnmarshal(Unmarshaller u, Object parent) {
		for (TeleporterTemplate template : tlist) {
			npctlistData.put(template.getTeleportId(), template);
		}
	}

	/**
	 * 返回已加载的传送员模板数量。
	 * Returns the number of loaded teleporter templates.
	 *
	 * template count
	 */
	public int size() {
		return npctlistData.size();
	}

	/**
	 * 按 NPC ID 查找包含该 NPC 的传送员模板。
	 * Finds the teleporter template that contains the given npc id.
	 *
	 * npc id
	 *
	 * @param npcId @return 传送员模板，未找到则为 null / teleporter template or null
	 */
	public TeleporterTemplate getTeleporterTemplateByNpcId(int npcId) {
		for (TeleporterTemplate template : npctlistData.values()) {
			if (template.containNpc(npcId)) {
				return template;
			}
		}
		return null;
	}

	/**
	 * 按传送 ID 获取传送员模板。
	 * Returns the teleporter template for the given teleport id.
	 *
	 * teleport id
	 *
	 * @param teleportId @return 传送员模板，不存在则为 null / teleporter template or null
	 */
	public TeleporterTemplate getTeleporterTemplateByTeleportId(int teleportId) {
		return npctlistData.get(teleportId);
	}
}
