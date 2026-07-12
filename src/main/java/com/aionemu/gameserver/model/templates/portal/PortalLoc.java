package com.aionemu.gameserver.model.templates.portal;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 传送门 Loc 模板（静态数据/XML）。
 * XML template.
 *
 * @author xTz
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "PortalLoc")
public class PortalLoc {

	@XmlAttribute(name = "world_id")
	protected int worldId;
	@XmlAttribute(name = "loc_id")
	protected int locId;
	@XmlAttribute(name = "x")
	protected float x;
	@XmlAttribute(name = "y")
	protected float y;
	@XmlAttribute(name = "z")
	protected float z;
	@XmlAttribute(name = "h")
	protected byte h;

	/** 返回世界 ID / Returns the world id */
	public int getWorldId() {
		return worldId;
	}

	/** 设置 world id / Sets the world id */
	public void setWorldId(int value) {
		this.worldId = value;
	}

	/** 返回 loc id / Returns the loc id */
	public int getLocId() {
		return locId;
	}

	/** 设置 loc id / Sets the loc id */
	public void setLocId(int value) {
		this.locId = value;
	}

	/** 返回 x / Returns the x */
	public float getX() {
		return x;
	}

	/** 设置 x / Sets the x */
	public void setX(float value) {
		this.x = value;
	}

	/** 返回 y / Returns the y */
	public float getY() {
		return y;
	}

	/** 设置 y / Sets the y */
	public void setY(float value) {
		this.y = value;
	}

	/** 返回 z / Returns the z */
	public float getZ() {
		return z;
	}

	/** 设置 z / Sets the z */
	public void setZ(float value) {
		this.z = value;
	}

	/** 返回 h / Returns the h */
	public byte getH() {
		return h;
	}

	/** 设置 h / Sets the h */
	public void setH(byte value) {
		this.h = value;
	}
}
