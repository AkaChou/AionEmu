package com.aionemu.gameserver.skillengine.model;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 敌对判定类型：直接敌对、间接敌对或无。
 * Hostile type: direct, indirect, or none.
 */
@XmlType(name = "HostileType")
@XmlEnum
public enum HostileType {
	/** 直接敌对 / Direct hostility */
	DIRECT,
	/** 间接敌对 / Indirect hostility */
	INDIRECT,
	/** 无敌对 / No hostility */
	NONE
}
