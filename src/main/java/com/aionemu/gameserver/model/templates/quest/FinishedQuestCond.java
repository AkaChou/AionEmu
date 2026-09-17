package com.aionemu.gameserver.model.templates.quest;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;
import lombok.Getter;

/**
 * 已完成任务条件模板（静态数据/XML）。
 * Finished quest condition template (static data / XML).
 *
 * @author antness
 */
@Getter
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "FinishedQuest", propOrder = { "questId", "reward" })
public class FinishedQuestCond {

	/** 返回任务 ID / Returns the quest id */
	@XmlAttribute(name = "quest_id", required = true)
	protected int questId;
	@XmlAttribute(name = "reward")
	protected int reward;

	/** 获取奖励。 / Returns the reward. */
	public Integer getReward() {
		return reward;
	}
}
