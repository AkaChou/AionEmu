package com.aionemu.gameserver.model.templates.factions;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 势力分类枚举。
 * Faction Category enumeration.
 *
 * @author vlog
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "FactionCategory")
public enum FactionCategory {

	/** 导师 / Mentor */
	MENTOR,
	/** 每日 / Daily */
	DAILY,
	/** 组合技能 / Combine skill */
	COMBINESKILL,
	/** 术古 / Shugo */
	SHUGO;
}
