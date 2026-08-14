package com.aionemu.gameserver.model.templates.teleport;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.aionemu.gameserver.model.Race;

/**
 * 热点地点模板（静态数据/XML）。
 * Hotspot location template (static data/XML).
 *
 * @author Rinzler (Encom)
 */
@XmlRootElement(name = "hotspot_template")
@XmlAccessorType(XmlAccessType.NONE)
public class HotspotlocationTemplate {

	@XmlAttribute(name = "loc_id", required = true)
	private int locId;

	@XmlAttribute(name = "mapid", required = true)
	private int mapid = 0;

	@XmlAttribute(name = "name", required = true)
	private String name = "";

	@XmlAttribute(name = "name_id", required = true)
	private int nameId;

	@XmlAttribute(name = "price")
	private int price;

	@XmlAttribute(name = "race")
	private Race race = Race.PC_ALL;

	@XmlAttribute(name = "posX")
	private float x = 0;

	@XmlAttribute(name = "posY")
	private float y = 0;

	@XmlAttribute(name = "posZ")
	private float z = 0;

	@XmlAttribute(name = "heading")
	private int heading = 0;

	/** 返回 loc id / Returns the loc id */
	public int getLocId() {
		return locId;
	}

	/** 返回映射 ID / Returns the map id */
	public int getMapId() {
		return mapid;
	}

	/** 获取名称。 / Returns the name. */
	public String getName() {
		return name;
	}

	/** 返回名称 ID / Returns the name id */
	public int getNameId() {
		return nameId;
	}

	/** 获取价格。 / Returns the price. */
	public int getPrice() {
		return price;
	}

	/** 获取种族。 / Returns the race. */
	public Race getRace() {
		return race;
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

	/** 返回 heading / Returns the heading */
	public int getHeading() {
		return heading;
	}
}
