package com.aionemu.gameserver.model.templates.teleport;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * 传送师模板（静态数据/XML）。
 * Teleporter template (static data/XML).
 *
 * @author orz
 */
@XmlRootElement(name = "teleporter_template")
@XmlAccessorType(XmlAccessType.NONE)
public class TeleporterTemplate {

	@XmlAttribute(name = "npc_ids")
	private List<Integer> npcIds;

	@XmlAttribute(name = "teleportId", required = true)
	private int teleportId = 0;

	@XmlElement(name = "locations")
	private TeleLocIdData teleLocIdData;

	/**
	 * 返回关联的 NPC ID 列表。
	 * Returns the bound npc ids.
	 *
	 * @return NPC ID 列表 / npc ids
	 */
	public List<Integer> getNpcIds() {
		return npcIds;
	}

	/**
	 * 判断给定 NPC ID 是否绑定此传送师。
	 * Whether the given npc id is bound to this teleporter.
	 *
	 * @param npcId NPC ID / npc id
	 * @return 包含则为 true / true if contained
	 */
	public boolean containNpc(int npcId) {
		return npcIds.contains(npcId);
	}

	/**
	 * 返回传送 ID。
	 * Returns the teleport id.
	 *
	 * @return 传送 ID / teleport id
	 */
	public int getTeleportId() {
		return teleportId;
	}

	/**
	 * 返回地点 ID 数据。
	 * Returns the tele-location-id data.
	 *
	 * @return 传送地点数据 / tele-location data
	 */
	public TeleLocIdData getTeleLocIdData() {
		return teleLocIdData;
	}
}
