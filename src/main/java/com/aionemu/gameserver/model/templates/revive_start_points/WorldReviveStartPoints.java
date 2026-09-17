package com.aionemu.gameserver.model.templates.revive_start_points;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.Race;
import lombok.Getter;
import lombok.Setter;

/**
 * 世界 ReviveStart 点模板（静态数据/XML）。
 * XML template.
 */

@Getter
@Setter
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "WorldReviveStartPoints")
public class WorldReviveStartPoints {
	@XmlAttribute(name = "world_id")
	protected int worldId;

	@XmlAttribute(name = "name")
	protected String name;

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

	@XmlAttribute(name = "max_level")
	protected int maxLevel;

	@XmlAttribute(name = "min_level")
	protected int minLevel;

	/** 返回 revive world / Returns the revive world */
	public int getReviveWorld() {
		return worldId;
	}

	/** 返回 maxlevel / Returns the maxlevel */
	public int getMaxlevel() {
		return maxLevel;
	}

	/** 返回 minlevel / Returns the minlevel */
	public int getMinlevel() {
		return minLevel;
	}
}
