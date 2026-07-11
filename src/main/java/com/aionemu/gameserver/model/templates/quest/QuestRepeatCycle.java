package com.aionemu.gameserver.model.templates.quest;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 任务 RepeatCycle 枚举。
 * Quest Repeat Cycle enumeration.
 */

@XmlType(name = "QuestRepeatCycle")
@XmlEnum
public enum QuestRepeatCycle {
	/** 全部 / All. */
	ALL(0), MON(1), TUE(2), WED(3), THU(4), FRI(5), SAT(6), SUN(7);

	private int weekDay;

	private QuestRepeatCycle(int weekDay) {
		this.weekDay = weekDay;
	}

	/** 返回 day / Returns the day */
	public int getDay() {
		return weekDay;
	}
}
