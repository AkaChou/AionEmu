package com.aionemu.gameserver.model.templates.zone;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 区域类型枚举。
 * Area Type enumeration.
 *
 * @author MrPoke
 */
@XmlType(name = "AreaType")
@XmlEnum
public enum AreaType {

	/** 多边形。 / Polygon. */
	POLYGON, CYLINDER, SPHERE, SEMISPHERE;
}
