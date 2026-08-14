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

	/**
	 * 区域形状类型：多边形 / 圆柱体 / 球体 / 半球体。
	 * Area shape types: polygon / cylinder / sphere / semisphere.
	 */
	POLYGON, CYLINDER, SPHERE, SEMISPHERE;
}
