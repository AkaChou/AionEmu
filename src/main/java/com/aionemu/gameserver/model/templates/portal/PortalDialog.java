package com.aionemu.gameserver.model.templates.portal;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;
import lombok.Getter;
import lombok.Setter;

/**
 * 传送门对话模板（静态数据/XML）。
 * XML template.
 * @author xTz
 */
@Getter
@Setter
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "PortalDialog", propOrder = { "portalPath" })
public class PortalDialog {

	/** 获取传送门路径。 / Returns the portal path. */
	@XmlElement(name = "portal_path")
	protected List<PortalPath> portalPath;
	/** 返回 NPC ID / Returns the npc id */
	@XmlAttribute(name = "npc_id")
	protected int npcId;
	/** 返回攻城 ID / Returns the siege id */
	@XmlAttribute(name = "siege_id")
	protected int siegeId;
	/** 返回 teleport dialog id / Returns the teleport dialog id */
	@XmlAttribute(name = "teleport_dialog_id")
	protected int teleportDialogId = 1011;
}
