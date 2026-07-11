package com.aionemu.gameserver.skillengine.model;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 充能加成类型：物理/魔法充能增益分类。
 * Bonus charge type: physical/magical charge bonus classification.
 *
 * @author Rinzler
 */
@XmlType(name = "bonusChargeType")
@XmlEnum
public enum BonusChargeType {

	/** 无 / None */
	NONE,
	/** 物理 / Physical */
	PHYSICAL,
	/** 魔法 / Magical */
	MAGICAL;
}
