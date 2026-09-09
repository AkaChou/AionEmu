package com.aionemu.gameserver.model.templates.zone;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 圆柱模板（静态数据/XML）。
 * XML template.
 *
 * @author MrPoke
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Cylinder")
@NoArgsConstructor
public class Cylinder {

	/** 返回 top / Returns the top */
	@Getter
	@XmlAttribute
	protected Float top;
	/** 返回 bottom / Returns the bottom */
	@Getter
	@XmlAttribute
	protected Float bottom;
	/** 返回 x / Returns the x */
	@Getter
	@XmlAttribute
	protected Float x;
	/** 返回 y / Returns the y */
	@Getter
	@XmlAttribute
	protected Float y;
	/** 返回 r / Returns the r */
	@Getter
	@XmlAttribute
	protected Float r;

	public Cylinder(float x, float y, float radius, float top, float bottom) {
		this.x = x;
		this.y = y;
		this.r = radius;
		this.top = top;
		this.bottom = bottom;
	}
}
