package com.aionemu.gameserver.model.templates.road;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;
import lombok.Getter;

/**
 * 道路 Exit 模板（静态数据/XML）。
 * XML template.
 *
 * @author SheppeR
 */
@Getter
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "RoadExit")
public class RoadExit {

	@XmlAttribute(name = "mapid")
	private int mapId;

	/** 返回 x / Returns the x */
	@XmlAttribute(name = "x")
	private float x;

	/** 返回 y / Returns the y */
	@XmlAttribute(name = "y")
	private float y;

	/** 返回 z / Returns the z */
	@XmlAttribute(name = "z")
	private float z;

	/** 获取地图。 / Returns the map. */
	public int getMap() {
		return mapId;
	}
}
