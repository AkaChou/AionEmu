package com.aionemu.gameserver.model.templates.zone;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 球体模板（静态数据/XML）。
 * XML template.
 *
 * @author MrPoke
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Sphere")
@NoArgsConstructor
public class Sphere {

	/** 返回 x / Returns the x */
	@Getter
	@XmlAttribute
	protected Float x;
	/** 返回 y / Returns the y */
	@Getter
	@XmlAttribute
	protected Float y;
	/** 返回 z / Returns the z */
	@Getter
	@XmlAttribute
	protected Float z;
	/** 返回 r / Returns the r */
	@Getter
	@XmlAttribute
	protected Float r;

	public Sphere(float x, float y, float z, float radius) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.r = radius;
	}
}
