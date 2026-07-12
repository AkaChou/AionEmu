package com.aionemu.gameserver.model.templates.curingzones;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 治疗模板（静态数据/XML）。
 * XML template.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "CuringTemplate")
public class CuringTemplate {

	@XmlAttribute(name = "map_id")
	protected int mapId;

	@XmlAttribute(name = "x")
	protected float x;

	@XmlAttribute(name = "y")
	protected float y;

	@XmlAttribute(name = "z")
	protected float z;

	@XmlAttribute(name = "range")
	protected float range;

	/** 返回映射 ID / Returns the map id */
	public int getMapId() {
		return mapId;
	}

	/** 设置 map id / Sets the map id */
	public void setMapId(int value) {
		mapId = value;
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

	/** 返回范围 / Returns the range*/
	public float getRange() {
		return range;
	}
}
