package com.aionemu.gameserver.model.templates.road;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.utils3d.Point3D;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 道路点模板（静态数据/XML）。
 * XML template.
 *
 * @author SheppeR
 */
@Getter
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "RoadPoint")
@NoArgsConstructor
public class RoadPoint {

	/** 返回 x / Returns the x */
	@XmlAttribute(name = "x")
	private float x;

	/** 返回 y / Returns the y */
	@XmlAttribute(name = "y")
	private float y;

	/** 返回 z / Returns the z */
	@XmlAttribute(name = "z")
	private float z;

	public RoadPoint(Point3D p) {
		x = (float) p.x;
		y = (float) p.y;
		z = (float) p.z;
	}
}
