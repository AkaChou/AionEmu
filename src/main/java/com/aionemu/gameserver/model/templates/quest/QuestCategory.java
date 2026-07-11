package com.aionemu.gameserver.model.templates.quest;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 任务分类枚举。
 * Quest Category enumeration.
 */

@XmlType(name = "QuestCategory")
@XmlEnum
public enum QuestCategory {
	/** 任务。 / Quest. */
	QUEST(0), EVENT(1), MISSION(0), SIGNIFICANT(0), IMPORTANT(0), NON_COUNT(0), SEEN_MARKER(0), TASK(0), FACTION(0),
	/** 挑战任务。 / Challenge Task. */
	CHALLENGE_TASK(0), PUBLIC(0), LEGION(0), PRIMARY(0);

	private int id;

	private QuestCategory(int id) {
		this.id = id;
	}

	/** 返回 ID / Returns the id */
	public int getId() {
		return id;
	}
}
