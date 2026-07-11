package com.aionemu.gameserver.skillengine.model;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 跳跃/位移数值来源：按伤害或技能等级计算 hop。
 * Hop value source: derive hop amount from damage or skill level.
 *
 * @author ATracer
 */
@XmlType(name = "HopType")
@XmlEnum
public enum HopType {

	/** 按伤害 / From damage */
	DAMAGE,
	/** 按技能等级 / From skill level */
	SKILLLV;
}
