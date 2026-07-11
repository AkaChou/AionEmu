package com.aionemu.gameserver.model.templates.zone;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 球体模板（静态数据/XML）。
 * XML template. / XML template.
 *
 * @author MrPoke
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Sphere")
public class Sphere {

	@XmlAttribute
	protected Float x;
	@XmlAttribute
	protected Float y;
	@XmlAttribute
	protected Float z;
	@XmlAttribute
	protected Float r;

	public Sphere() {
	}

	public Sphere(float x, float y, float z, float radius) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.r = radius;
	}

	/** 返回 x / Returns the x */
	public Float getX() {
		return x;
	}

	/** 返回 y / Returns the y */
	public Float getY() {
		return y;
	}

	/** 返回 z / Returns the z */
	public Float getZ() {
		return z;
	}

	/** 返回 r / Returns the r */
	public Float getR() {
		return r;
	}
}
