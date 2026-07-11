package com.aionemu.gameserver.model.templates.abyss_op;

import jakarta.xml.bind.annotation.XmlEnum;

/**
 * 欧比斯 Op 类型枚举。
 * Abyss Op Type enumeration.
 *
 * @author Rinzler (Encom)
 */

@XmlEnum
public enum AbyssOpType {
	/** 基础。 / Base. */
	BASE, OBJECT, ARTIFACT, FORTRESS, WORLD_RAID, INIT_OBJECT, RAID_OBJECT, FORTRESS_RAID, OWNERSHIP_OBJECT;
}
