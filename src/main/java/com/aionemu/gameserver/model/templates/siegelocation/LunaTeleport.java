package com.aionemu.gameserver.model.templates.siegelocation;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.Race;
import lombok.Getter;
import lombok.Setter;

/**
 * 月华传送模板（静态数据/XML）。
 * XML template.
 */

@Getter
@Setter
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "LunaTeleport")
public class LunaTeleport {
	/** 返回世界 ID / Returns the world id */
	@XmlAttribute(name = "world_id")
	protected int worldId;

	/** 获取种族。 / Returns the race. */
	@XmlAttribute(name = "race")
	protected Race race = Race.PC_ALL;

	/** 返回 x / Returns the x */
	@XmlAttribute(name = "x")
	protected float x;

	/** 返回 y / Returns the y */
	@XmlAttribute(name = "y")
	protected float y;

	/** 返回 z / Returns the z */
	@XmlAttribute(name = "z")
	protected float z;

	/** 返回 h / Returns the h */
	@XmlAttribute(name = "h")
	protected byte h;
}
