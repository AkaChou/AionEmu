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

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "LunaTeleport")
public class LunaTeleport {
	/** 返回世界 ID / Returns the world id */
	@Getter
	@Setter
	@XmlAttribute(name = "world_id")
	protected int worldId;

	/** 获取种族。 / Returns the race. */
	@Getter
	@XmlAttribute(name = "race")
	protected Race race = Race.PC_ALL;

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
