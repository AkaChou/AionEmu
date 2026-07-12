package com.aionemu.gameserver.model.templates.road;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.utils3d.Point3D;

/**
 * 道路点模板（静态数据/XML）。
 * XML template.
 *
 * @author SheppeR
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "RoadPoint")
public class RoadPoint {

	@XmlAttribute(name = "x")
	private float x;

	@XmlAttribute(name = "y")
	private float y;

	@XmlAttribute(name = "z")
	private float z;

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

	public RoadPoint() {
	}

	public RoadPoint(Point3D p) {
		x = (float) p.x;
		y = (float) p.y;
		z = (float) p.z;
	}
}
