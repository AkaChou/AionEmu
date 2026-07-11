package com.aionemu.gameserver.model.templates.road;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 道路 Exit 模板（静态数据/XML）。
 * XML template. / XML template.
 *
 * @author SheppeR
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "RoadExit")
public class RoadExit {

	@XmlAttribute(name = "mapid")
	private int mapId;

	@XmlAttribute(name = "x")
	private float x;

	@XmlAttribute(name = "y")
	private float y;

	@XmlAttribute(name = "z")
	private float z;

	/** 获取地图。 / Returns the map. */
	public int getMap() {
		return mapId;
	}

	/** 返回 x / Returns the x */
	public float getX() {
		return x;
	}

	/** 返回 y / Returns the y */
	public float getY() {
		return y;
	}

	/** 返回 z / Returns the z */
	public float getZ() {
		return z;
	}
}
