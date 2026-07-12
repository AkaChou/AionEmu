package com.aionemu.gameserver.model.templates.housing;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 房屋 Address 模板（静态数据/XML）。
 * XML template.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "")
@XmlRootElement(name = "address")
public class HouseAddress {
	@XmlAttribute(name = "exit_z")
	protected Float exitZ;
	@XmlAttribute(name = "exit_y")
	protected Float exitY;
	@XmlAttribute(name = "exit_x")
	protected Float exitX;
	@XmlAttribute(name = "exit_map")
	protected Integer exitMap;
	@XmlAttribute(required = true)
	protected float z;
	@XmlAttribute(required = true)
	protected float y;
	@XmlAttribute(required = true)
	protected float x;
	@XmlAttribute(name = "town", required = true)
	private int townId;
	@XmlAttribute(required = true)
	protected int map;
	@XmlAttribute(required = true)
	protected int id;

	/** 返回 exit z / Returns the exit z */
	public Float getExitZ() {
		return exitZ;
	}

	/** 返回 exit y / Returns the exit y */
	public Float getExitY() {
		return exitY;
	}

	/** 返回 exit x / Returns the exit x */
	public Float getExitX() {
		return exitX;
	}

	/** 返回 exit map id / Returns the exit map id */
	public Integer getExitMapId() {
		return exitMap;
	}

	/** 返回 z / Returns the z */
	public float getZ() {
		return z;
	}

	/** 返回 y / Returns the y */
	public float getY() {
		return y;
	}

	/** 返回 x / Returns the x */
	public float getX() {
		return x;
	}

	/** 返回映射 ID / Returns the map id */
	public int getMapId() {
		return map;
	}

	/** 返回 ID / Returns the id */
	public int getId() {
		return id;
	}

	/** 返回城镇 ID / Returns the town id */
	public int getTownId() {
		return townId;
	}
}
