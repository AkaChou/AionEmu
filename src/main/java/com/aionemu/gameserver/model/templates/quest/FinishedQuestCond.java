package com.aionemu.gameserver.model.templates.quest;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 已完成任务条件模板（静态数据/XML）。
 * Finished quest condition template (static data / XML).
 *
 * @author antness
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "FinishedQuest", propOrder = { "questId", "reward" })
public class FinishedQuestCond {

	@XmlAttribute(name = "quest_id", required = true)
	protected int questId;
	@XmlAttribute(name = "reward")
	protected int reward;

	/** 返回任务 ID / Returns the quest id */
	public int getQuestId() {
		return questId;
	}

	/** 获取奖励。 / Returns the reward. */
	public Integer getReward() {
		return reward;
	}
}
