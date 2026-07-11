package com.aionemu.gameserver.skillengine.model;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 命中类型：普通/背击/魔法/物理等攻击命中分类。
 * Hit type: normal, backstab, magical or physical hit classification.
 */
@XmlType(name = "HitType")
@XmlEnum
public enum HitType {

	/** 每次命中 / Every hit */
	EVERYHIT,
	/** 普通攻击 / Normal attack */
	NMLATK,
	/** 背击 / Back attack */
	BACKATK,
	/** 魔法命中 / Magical hit */
	MAHIT,
	/** 物理命中 / Physical hit */
	PHHIT;
}
