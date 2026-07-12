package com.aionemu.gameserver.model.templates.zone;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 圆柱模板（静态数据/XML）。
 * XML template.
 *
 * @author MrPoke
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Cylinder")
public class Cylinder {

	@XmlAttribute
	protected Float top;
	@XmlAttribute
	protected Float bottom;
	@XmlAttribute
	protected Float x;
	@XmlAttribute
	protected Float y;
	@XmlAttribute
	protected Float r;

	public Cylinder() {
	}

	public Cylinder(float x, float y, float radius, float top, float bottom) {
		this.x = x;
		this.y = y;
		this.r = radius;
		this.top = top;
		this.bottom = bottom;
	}

	/** 返回 top / Returns the top */
	public Float getTop() {
		return top;
	}

	/** 返回 bottom / Returns the bottom */
	public Float getBottom() {
		return bottom;
	}

	/** 返回 x / Returns the x */
	public Float getX() {
		return x;
	}

	/** 返回 y / Returns the y */
	public Float getY() {
		return y;
	}

	/** 返回 r / Returns the r */
	public Float getR() {
		return r;
	}
}
