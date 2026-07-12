package com.aionemu.gameserver.model.templates;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

/**
 * Ride 边界模板（静态数据/XML）。
 * XML template.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "RideBound")
public class RideBound extends BoundRadius {
	@XmlAttribute
	private Float altitude;

	public RideBound() {
	}

	public RideBound(float front, float side, float upper, float altitude) {
		super(front, side, upper);
		this.altitude = altitude;
	}

	/** 返回 altitude / Returns the altitude */
	public Float getAltitude() {
		return altitude;
	}
}
