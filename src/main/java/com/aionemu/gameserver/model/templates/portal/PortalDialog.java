package com.aionemu.gameserver.model.templates.portal;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 传送门对话模板（静态数据/XML）。
 * XML template.
 *
 * @author xTz
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "PortalDialog", propOrder = { "portalPath" })
public class PortalDialog {

	@XmlElement(name = "portal_path")
	protected List<PortalPath> portalPath;
	@XmlAttribute(name = "npc_id")
	protected int npcId;
	@XmlAttribute(name = "siege_id")
	protected int siegeId;
	@XmlAttribute(name = "teleport_dialog_id")
	protected int teleportDialogId = 1011;

	/** 获取传送门路径。 / Returns the portal path. */
	public List<PortalPath> getPortalPath() {
		return portalPath;
	}

	/** 返回 NPC ID / Returns the npc id */
	public int getNpcId() {
		return npcId;
	}

	/** 设置 npc id / Sets the npc id */
	public void setNpcId(int value) {
		this.npcId = value;
	}

	/** 返回攻城 ID / Returns the siege id */
	public int getSiegeId() {
		return siegeId;
	}

	/** 设置 siege id / Sets the siege id */
	public void setSiegeId(int value) {
		this.siegeId = value;
	}

	/** 返回 teleport dialog id / Returns the teleport dialog id */
	public int getTeleportDialogId() {
		return teleportDialogId;
	}
}
