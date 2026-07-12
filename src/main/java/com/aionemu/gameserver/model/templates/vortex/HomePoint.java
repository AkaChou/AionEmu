package com.aionemu.gameserver.model.templates.vortex;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.world.WorldPosition;

/**
 * Home 点模板（静态数据/XML）。
 * XML template.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "HomePoint")
public class HomePoint {
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

	/** 返回 home point / Returns the home point */
	public WorldPosition getHomePoint() {
		WorldPosition home = new WorldPosition(map);
		home.setMapId(map);
		home.setXYZH(x, y, z, h);
		return home;
	}
}
