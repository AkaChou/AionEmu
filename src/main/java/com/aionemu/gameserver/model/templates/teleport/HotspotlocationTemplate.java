package com.aionemu.gameserver.model.templates.teleport;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.aionemu.gameserver.model.Race;
import lombok.Getter;

/**
 * 热点地点模板（静态数据/XML）。
 * Hotspot location template (static data/XML).
 * @author Rinzler (Encom)
 */
@Getter
@XmlRootElement(name = "hotspot_template")
@XmlAccessorType(XmlAccessType.NONE)
public class HotspotlocationTemplate {

	/** 返回 loc id / Returns the loc id */
	@XmlAttribute(name = "loc_id", required = true)
	private int locId;

	@XmlAttribute(name = "mapid", required = true)
	private int mapid = 0;

	/** 获取名称。 / Returns the name. */
	@XmlAttribute(name = "name", required = true)
	private String name = "";

	/** 返回名称 ID / Returns the name id */
	@XmlAttribute(name = "name_id", required = true)
	private int nameId;

	/** 获取价格。 / Returns the price. */
	@XmlAttribute(name = "price")
	private int price;

	/** 获取种族。 / Returns the race. */
	@XmlAttribute(name = "race")
	private Race race = Race.PC_ALL;

	/** 返回 x / Returns the x */
	@XmlAttribute(name = "posX")
	private float x = 0;

	/** 返回 y / Returns the y */
	@XmlAttribute(name = "posY")
	private float y = 0;

	/** 返回 z / Returns the z */
	@XmlAttribute(name = "posZ")
	private float z = 0;

	/** 返回 heading / Returns the heading */
	@XmlAttribute(name = "heading")
	private int heading = 0;

	/** 返回映射 ID / Returns the map id */
	public int getMapId() {
		return mapid;
	}
}
