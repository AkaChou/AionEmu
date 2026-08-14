package com.aionemu.gameserver.skillengine.model;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 治疗类型：生命/魔法/神圣力/飞行值。
 * Heal type: HP / MP / DP / FP.
 *
 * @author ATracer
 */
@XmlType(name = "HealType")
@XmlEnum
public enum HealType {

	/** 生命值 / Hit points */
	HP,
	/** 魔法值 / Mana points */
	MP,
	/** 神圣力 / Divine points */
	DP,
	/** 飞行值 / Flight points */
	FP;
}
