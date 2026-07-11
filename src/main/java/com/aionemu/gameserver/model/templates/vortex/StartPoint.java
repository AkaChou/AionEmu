package com.aionemu.gameserver.model.templates.vortex;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.world.WorldPosition;

/**
 * Start 点模板（静态数据/XML）。
 * XML template. / XML template.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "StartPoint")
public class StartPoint {
	@XmlAttribute(name = "map")
	protected int map;

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
		return map;
	}

	/** 返回开始点 / Returns the start point*/
	public WorldPosition getStartPoint() {
		WorldPosition start = new WorldPosition(map);
		start.setMapId(map);
		start.setXYZH(x, y, z, h);
		return start;
	}
}
