package com.aionemu.gameserver.model.templates.teleport;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Getter;

/**
 * 传送地点模板（静态数据/XML）。
 * Telelocation template (static data/XML).
 * @author orz
 */
@Getter
@XmlRootElement(name = "teleloc_template")
@XmlAccessorType(XmlAccessType.NONE)
public class TelelocationTemplate {

	/**
	 * 地点 ID / Location id
	 */
	@XmlAttribute(name = "loc_id", required = true)
	private int locId;

	@XmlAttribute(name = "mapid", required = true)
	private int mapid = 0;
	/**
	 * 地点名称 / location name
	 */
	@XmlAttribute(name = "name", required = true)
	private String name = "";

	/** 返回名称 ID / Returns the name id */
	@XmlAttribute(name = "name_id", required = true)
	private int nameId;

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
