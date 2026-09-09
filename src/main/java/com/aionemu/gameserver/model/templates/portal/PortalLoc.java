package com.aionemu.gameserver.model.templates.portal;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;
import lombok.Getter;
import lombok.Setter;

/**
 * 传送门 Loc 模板（静态数据/XML）。
 * XML template.
 *
 * @author xTz
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "PortalLoc")
public class PortalLoc {

	/** 返回世界 ID / Returns the world id */
	@Getter
	@Setter
	@XmlAttribute(name = "world_id")
	protected int worldId;
	/** 返回 loc id / Returns the loc id */
	@Getter
	@Setter
	@XmlAttribute(name = "loc_id")
	protected int locId;
	/** 返回 x / Returns the x */
	@Getter
	@Setter
	@XmlAttribute(name = "x")
	protected float x;
	/** 返回 y / Returns the y */
	@Getter
	@Setter
	@XmlAttribute(name = "y")
	protected float y;
	/** 返回 z / Returns the z */
	@Getter
	@Setter
	@XmlAttribute(name = "z")
	protected float z;
	/** 返回 h / Returns the h */
	@Getter
	@Setter
	@XmlAttribute(name = "h")
	protected byte h;
}
