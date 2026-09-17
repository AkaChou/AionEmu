package com.aionemu.gameserver.model.templates.portal;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.Race;
import lombok.Getter;
import lombok.Setter;

/**
 * 传送门 Use 模板（静态数据/XML）。
 * XML template.
 *
 * @author xTz
 */
@Getter
@Setter
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "PortalUse")
public class PortalUse {

	@XmlElement(name = "portal_path")
	protected List<PortalPath> portalPath;
	/** 返回 NPC ID / Returns the npc id */
	@XmlAttribute(name = "npc_id")
	protected int npcId;
	/** 返回攻城 ID / Returns the siege id */
	@XmlAttribute(name = "siege_id")
	protected int siegeId;

	/** 返回 portal paths / Returns the portal paths */
	public List<PortalPath> getPortalPaths() {
		return portalPath;
	}

	/** 获取传送门路径。 / Returns the portal path. */
	public PortalPath getPortalPath(Race race) {
		if (portalPath != null) {
			for (PortalPath path : portalPath) {
				if (path.getRace().equals(race) || path.getRace().equals(Race.PC_ALL)) {
					return path;
				}
			}
		}
		return null;
	}
}
