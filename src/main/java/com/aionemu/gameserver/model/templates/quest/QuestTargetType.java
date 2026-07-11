package com.aionemu.gameserver.model.templates.quest;

import jakarta.xml.bind.annotation.XmlEnum;

/**
 * 任务目标类型枚举。
 * Quest target type enumeration.
 *
 * @author Rinzler (Encom)
 */
@XmlEnum
public enum QuestTargetType {
	/** 无。 / None. */
	NONE,
	/** 区域。 / Area. */
	AREA,
	/** 部队。 / Force. */
	FORCE,
	/** 联盟。 / Union. */
	UNION,
	/** 战斗群。 / Battlegroup. */
	BATTLEGROUP
}
