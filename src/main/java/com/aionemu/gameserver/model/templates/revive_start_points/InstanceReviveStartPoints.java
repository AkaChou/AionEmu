package com.aionemu.gameserver.model.templates.revive_start_points;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;
import lombok.Getter;
import lombok.Setter;

/**
 * 副本 ReviveStart 点模板（静态数据/XML）。
 * XML template.
 */

@Getter
@Setter
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "InstanceReviveStartPoints")
public class InstanceReviveStartPoints {
	@XmlAttribute(name = "world_id")
	protected int worldId;

	@XmlAttribute(name = "name")
	protected String name;

	/** 返回 x / Returns the x */
	@XmlAttribute(name = "x")
	protected float x;

	/** 返回 y / Returns the y */
	@XmlAttribute(name = "y")
	protected float y;

	/** 返回 z / Returns the z */
	@XmlAttribute(name = "z")
	protected float z;

	/** 返回 h / Returns the h */
	@XmlAttribute(name = "h")
	protected byte h;

	/** 返回 revive world / Returns the revive world */
	public int getReviveWorld() {
		return worldId;
	}
}
