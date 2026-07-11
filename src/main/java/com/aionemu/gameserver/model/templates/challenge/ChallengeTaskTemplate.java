package com.aionemu.gameserver.model.templates.challenge;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.Race;

/**
 * 挑战任务模板（静态数据/XML）。
 * XML template. / XML template.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ChallengeTask", propOrder = { "quest", "contrib", "reward" })
public class ChallengeTaskTemplate {
	@XmlElement(required = true)
	protected List<ChallengeQuestTemplate> quest;
	protected List<ContributionReward> contrib;

	@XmlElement(required = true)
	protected ChallengeReward reward;

	@XmlAttribute
	protected Boolean repeat;

	@XmlAttribute(name = "town_residence")
	protected Boolean townResidence;

	@XmlAttribute(name = "name_id")
	protected Integer nameId;

	@XmlAttribute(name = "max_level", required = true)
	protected int maxLevel;

	@XmlAttribute(name = "min_level", required = true)
	protected int minLevel;

	@XmlAttribute(name = "prev_task")
	protected Integer prevTask;

	@XmlAttribute(required = true)
	protected Race race;

	@XmlAttribute(required = true)
	protected ChallengeType type;

	@XmlAttribute(required = true)
	protected int id;

	/** 返回 quests / Returns the quests */
	public List<ChallengeQuestTemplate> getQuests() {
		return this.quest;
	}

	/** 返回 contrib / Returns the contrib */
	public List<ContributionReward> getContrib() {
		return this.contrib;
	}

	/** 获取奖励。 / Returns the reward. */
	public ChallengeReward getReward() {
		return this.reward;
	}

	/**
	 * @return Whether repeatable / Whether repeatable
	 */
	public boolean isRepeatable() {
		return this.repeat != null && this.repeat == true;
	}

	/**
	 * @return Whether town residence / Whether town residence
	 */
	public boolean isTownResidence() {
		return this.townResidence != null && this.townResidence == true;
	}

	/** 返回名称 ID / Returns the name id */
	public Integer getNameId() {
		return this.nameId;
	}

	/** 获取最大等级。 / Returns the max level. */
	public int getMaxLevel() {
		return this.maxLevel;
	}

	/** 获取最小等级。 / Returns the min level. */
	public int getMinLevel() {
		return this.minLevel;
	}

	/** 返回 prev task / Returns the prev task */
	public Integer getPrevTask() {
		return this.prevTask;
	}

	/** 获取种族。 / Returns the race. */
	public Race getRace() {
		return this.race;
	}

	/** 获取类型。 / Returns the type. */
	public ChallengeType getType() {
		return this.type;
	}

	/** 返回 ID / Returns the id */
	public int getId() {
		return this.id;
	}
}
