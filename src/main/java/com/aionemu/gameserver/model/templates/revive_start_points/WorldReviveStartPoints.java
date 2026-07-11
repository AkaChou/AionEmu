package com.aionemu.gameserver.model.templates.revive_start_points;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.Race;

/**
 * 世界 ReviveStart 点模板（静态数据/XML）。
 * XML template. / XML template.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "WorldReviveStartPoints")
public class WorldReviveStartPoints {
	@XmlAttribute(name = "world_id")
	protected int worldId;

	@XmlAttribute(name = "name")
	protected String name;

	@XmlAttribute(name = "race")
	protected Race race = Race.PC_ALL;

	@XmlAttribute(name = "x")
	protected float x;

	@XmlAttribute(name = "y")
	protected float y;

	@XmlAttribute(name = "z")
	protected float z;

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

	/** 获取种族。 / Returns the race. */
	public Race getRace() {
		return race;
	}

	/** 设置种族。 / Sets the race. */
	public void setRace(Race value) {
		race = value;
	}

	/** 返回 x / Returns the x */
	public float getX() {
		return x;
	}

	/** 设置 x / Sets the x */
	public void setX(float value) {
		x = value;
	}

	/** 返回 y / Returns the y */
	public float getY() {
		return y;
	}

	/** 设置 y / Sets the y */
	public void setY(float value) {
		y = value;
	}

	/** 返回 z / Returns the z */
	public float getZ() {
		return z;
	}

	/** 设置 z / Sets the z */
	public void setZ(float value) {
		z = value;
	}

	/** 返回 h / Returns the h */
	public byte getH() {
		return h;
	}

	/** 设置 h / Sets the h */
	public void setH(byte value) {
		h = value;
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
