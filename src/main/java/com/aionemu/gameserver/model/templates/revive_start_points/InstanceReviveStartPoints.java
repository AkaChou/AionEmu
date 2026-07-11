package com.aionemu.gameserver.model.templates.revive_start_points;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 副本 ReviveStart 点模板（静态数据/XML）。
 * XML template. / XML template.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "InstanceReviveStartPoints")
public class InstanceReviveStartPoints {
	@XmlAttribute(name = "world_id")
	protected int worldId;

	@XmlAttribute(name = "name")
	protected String name;

	@XmlAttribute(name = "x")
	protected float x;

	@XmlAttribute(name = "y")
	protected float y;

	@XmlAttribute(name = "z")
	protected float z;

	@XmlAttribute(name = "h")
	protected byte h;

	/** 返回 revive world / Returns the revive world */
	public int getReviveWorld() {
		return worldId;
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
}
