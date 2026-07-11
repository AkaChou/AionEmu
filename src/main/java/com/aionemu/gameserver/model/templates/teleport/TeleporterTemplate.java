package com.aionemu.gameserver.model.templates.teleport;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Teleporter 模板（静态数据/XML）。
 * XML template. / XML template.
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
	 * @return the npcId
	 */
	public List<Integer> getNpcIds() {
		return npcIds;
	}

	/**
	 * @return the name of npc
	 */
	public boolean containNpc(int npcId) {
		return npcIds.contains(npcId);
	}

	/**
	 * @return the teleportId
	 */
	public int getTeleportId() {
		return teleportId;
	}

	/**
	 * @return the teleLocIdData
	 */
	public TeleLocIdData getTeleLocIdData() {
		return teleLocIdData;
	}
}
