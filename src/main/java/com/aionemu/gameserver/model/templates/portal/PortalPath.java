package com.aionemu.gameserver.model.templates.portal;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.Race;
import lombok.Getter;
import lombok.Setter;

/**
 * 传送门路径模板（静态数据/XML）。
 * XML template.
 *
 * @author xTz
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "PortalPath")
public class PortalPath {

	/** 返回 portal req / Returns the portal req */
	@Getter
	@XmlElement(name = "portal_req")
	protected PortalReq portalReq;
	/** 获取对话。 / Returns the dialog. */
	@Getter
	@Setter
	@XmlAttribute(name = "dialog")
	protected int dialog;
	/** 返回 loc id / Returns the loc id */
	@Getter
	@Setter
	@XmlAttribute(name = "loc_id")
	protected int locId;
	/** 获取玩家计数。 / Returns the player count. */
	@Getter
	@Setter
	@XmlAttribute(name = "player_count")
	protected int playerCount;
	/** 是否副本。 / Whether Instance. */
	@Getter
	@Setter
	@XmlAttribute(name = "instance")
	protected boolean instance;
	/** 返回攻城 ID / Returns the siege id */
	@Getter
	@XmlAttribute(name = "siege_id")
	protected int siegeId;
	/** 获取种族。 / Returns the race. */
	@Getter
	@XmlAttribute(name = "race")
	protected Race race = Race.PC_ALL;
	/** 返回 err group / Returns the err group */
	@Getter
	@XmlAttribute(name = "err_group")
	protected int errGroup;
}
