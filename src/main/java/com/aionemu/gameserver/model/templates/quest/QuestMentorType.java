package com.aionemu.gameserver.model.templates.quest;

import jakarta.xml.bind.annotation.XmlEnum;

/**
 * 任务导师类型枚举。
 * Quest Mentor Type enumeration.
 *
 * @author MrPoke
 */
@XmlEnum
public enum QuestMentorType {
	/** 无 / None. */
	NONE, MENTOR, MENTE;
}
