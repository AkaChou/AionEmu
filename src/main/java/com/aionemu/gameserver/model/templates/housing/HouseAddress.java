package com.aionemu.gameserver.model.templates.housing;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import lombok.Getter;

/**
 * 房屋 Address 模板（静态数据/XML）。
 * XML template.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "")
@XmlRootElement(name = "address")
public class HouseAddress {
	/** 返回 exit z / Returns the exit z */
	@Getter
	@XmlAttribute(name = "exit_z")
	protected Float exitZ;
	/** 返回 exit y / Returns the exit y */
	@Getter
	@XmlAttribute(name = "exit_y")
	protected Float exitY;
	/** 返回 exit x / Returns the exit x */
	@Getter
	@XmlAttribute(name = "exit_x")
	protected Float exitX;
	@XmlAttribute(name = "exit_map")
	protected Integer exitMap;
	/** 返回 z / Returns the z */
	@Getter
	@XmlAttribute(required = true)
	protected float z;
	/** 返回 y / Returns the y */
	@Getter
	@XmlAttribute(required = true)
	protected float y;
	/** 返回 x / Returns the x */
	@Getter
	@XmlAttribute(required = true)
	protected float x;
	/** 返回城镇 ID / Returns the town id */
	@Getter
	@XmlAttribute(name = "town", required = true)
	private int townId;
	@XmlAttribute(required = true)
	protected int map;
	/** 返回 ID / Returns the id */
	@Getter
	@XmlAttribute(required = true)
	protected int id;

	/** 返回 exit map id / Returns the exit map id */
	public Integer getExitMapId() {
		return exitMap;
	}

	/** 返回映射 ID / Returns the map id */
	public int getMapId() {
		return map;
	}
}
