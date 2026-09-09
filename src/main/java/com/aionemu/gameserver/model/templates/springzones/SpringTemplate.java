package com.aionemu.gameserver.model.templates.springzones;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;
import lombok.Getter;
import lombok.Setter;

/**
 * 温泉模板（静态数据/XML）。
 * XML template.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SpringTemplate")
public class SpringTemplate {
	/** 返回映射 ID / Returns the map id */
	@Getter
	@Setter
	@XmlAttribute(name = "map_id")
	protected int mapId;

	/** 返回 x / Returns the x */
	@Getter
	@Setter
	@XmlAttribute(name = "x")
	protected float x;

	/** 返回 y / Returns the y */
	@Getter
	@Setter
	@XmlAttribute(name = "y")
	protected float y;

	/** 返回 z / Returns the z */
	@Getter
	@Setter
	@XmlAttribute(name = "z")
	protected float z;

	/** 返回范围 / Returns the range*/
	@Getter
	@XmlAttribute(name = "range")
	protected float range;
}
