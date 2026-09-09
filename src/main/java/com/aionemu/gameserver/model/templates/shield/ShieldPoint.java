package com.aionemu.gameserver.model.templates.shield;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.utils3d.Point3D;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 护盾点模板（静态数据/XML）。
 * XML template.
 *
 * @author M@xx, Wakizashi
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ShieldPoint")
@NoArgsConstructor
public class ShieldPoint {

	/** 返回 x / Returns the x */
	@Getter
	@XmlAttribute(name = "x")
	private float x;

	/** 返回 y / Returns the y */
	@Getter
	@XmlAttribute(name = "y")
	private float y;

	/** 返回 z / Returns the z */
	@Getter
	@XmlAttribute(name = "z")
	private float z;

	public ShieldPoint(Point3D p) {
		x = (float) p.x;
		y = (float) p.y;
		z = (float) p.z;
	}
}
