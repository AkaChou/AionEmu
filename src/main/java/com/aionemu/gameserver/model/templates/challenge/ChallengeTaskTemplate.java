package com.aionemu.gameserver.model.templates.challenge;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.Race;
import lombok.Getter;

/**
 * 挑战任务模板（静态数据/XML）。
 * XML template.
 */

@Getter
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ChallengeTask", propOrder = { "quest", "contrib", "reward" })
public class ChallengeTaskTemplate {
	@XmlElement(required = true)
	protected List<ChallengeQuestTemplate> quest;
	/** 返回贡献奖励列表 / Returns the contrib */
	protected List<ContributionReward> contrib;

	/** 获取奖励。 / Returns the reward. */
	@XmlElement(required = true)
	protected ChallengeReward reward;

	@XmlAttribute
	protected Boolean repeat;

	@XmlAttribute(name = "town_residence")
	protected Boolean townResidence;

	/** 返回名称 ID / Returns the name id */
	@XmlAttribute(name = "name_id")
	protected Integer nameId;

	/** 获取最大等级。 / Returns the max level. */
	@XmlAttribute(name = "max_level", required = true)
	protected int maxLevel;

	/** 获取最小等级。 / Returns the min level. */
	@XmlAttribute(name = "min_level", required = true)
	protected int minLevel;

	/** 返回前置任务 / Returns the prev task */
	@XmlAttribute(name = "prev_task")
	protected Integer prevTask;

	/** 获取种族。 / Returns the race. */
	@XmlAttribute(required = true)
	protected Race race;

	/** 获取类型。 / Returns the type. */
	@XmlAttribute(required = true)
	protected ChallengeType type;

	/** 返回 ID / Returns the id */
	@XmlAttribute(required = true)
	protected int id;

	/** 返回挑战任务列表 / Returns the quests */
	public List<ChallengeQuestTemplate> getQuests() {
		return this.quest;
	}

	/**
	 * 是否可重复完成。
	 * Whether the task is repeatable.
	 *
	 * @return 可重复时为 {@code true} / {@code true} if repeatable
	 */
	public boolean isRepeatable() {
		return this.repeat != null && this.repeat;
	}

	/**
	 * 是否为城镇居住任务。
	 * Whether the task is a town residence task.
	 *
	 * @return 是城镇居住任务时为 {@code true} / {@code true} if town residence
	 */
	public boolean isTownResidence() {
		return this.townResidence != null && this.townResidence;
	}
}
