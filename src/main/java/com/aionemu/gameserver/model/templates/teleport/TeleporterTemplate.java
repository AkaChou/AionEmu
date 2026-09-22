package com.aionemu.gameserver.model.templates.teleport;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Getter;

/**
 * 传送师模板（静态数据/XML）。
 * Teleporter template (static data/XML).
 * @author orz
 */
@Getter
@XmlRootElement(name = "teleporter_template")
@XmlAccessorType(XmlAccessType.NONE)
public class TeleporterTemplate {

	/**
	 * 返回关联的 NPC ID 列表。
	 * Returns the bound npc ids.
	 * NPC ID 列表 / npc ids
	 */
	@XmlAttribute(name = "npc_ids")
	private List<Integer> npcIds;

	/**
	 * 返回传送 ID。
	 * Returns the teleport id.
	 */
	@XmlAttribute(name = "teleportId", required = true)
	private int teleportId = 0;

	/**
	 * 返回地点 ID 数据。
	 * Returns the tele-location-id data.
	 */
	@XmlElement(name = "locations")
	private TeleLocIdData teleLocIdData;

	/**
	 * 判断给定 NPC ID 是否绑定此传送师。
	 * Whether the given npc id is bound to this teleporter.
	 * @param npcId NPC ID / npc id
	 * @return 包含则为 true / true if contained
	 */
	public boolean containNpc(int npcId) {
		return npcIds.contains(npcId);
	}
}
