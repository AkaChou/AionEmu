package com.aionemu.gameserver.model.templates.Guides;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.XmlType;

import org.apache.commons.lang3.StringUtils;

import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.Race;

/**
 * 指南模板（静态数据/XML）。
 * Guide template (static data/XML).
 *
 * @author xTz
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "GuideTemplate")
public class GuideTemplate {

	@XmlAttribute(name = "level")
	private int level;
	@XmlAttribute(name = "classType")
	private PlayerClass classType;
	@XmlAttribute(name = "title")
	private String title;
	@XmlAttribute(name = "race")
	private Race race;
	@XmlElement(name = "reward_info")
	private String rewardInfo = StringUtils.EMPTY;
	@XmlElement(name = "message")
	private String message = StringUtils.EMPTY;
	@XmlElement(name = "select")
	private String select = StringUtils.EMPTY;
	@XmlElement(name = "survey")
	private List<SurveyTemplate> surveys;
	@XmlAttribute(name = "rewardCount")
	private int rewardCount;
	@XmlTransient
	private boolean isActivated = true;

	/**
	 * @return the level
	 */
	public int getLevel() {
		return this.level;
	}

	/**
	 * @return the classId
	 */
	public PlayerClass getPlayerClass() {
		return this.classType;
	}

	/**
	 * @return the title
	 */
	public String getTitle() {
		return this.title;
	}

	/**
	 * @return the race
	 */
	public Race getRace() {
		return this.race;
	}

	/**
	 * @return the surveys
	 */
	public List<SurveyTemplate> getSurveys() {
		return this.surveys;
	}

	/**
	 * @return the message
	 */
	public String getMessage() {
		return this.message;
	}

	/**
	 * @return the select
	 */
	public String getSelect() {
		return this.select;
	}

	/**
	 * @return the select
	 */
	public String getRewardInfo() {
		return this.rewardInfo;
	}

	/** 获取奖励计数。 / Returns the reward count. */
	public int getRewardCount() {
		return this.rewardCount;
	}

	/**
	 * @return the isActivated
	 */
	public boolean isActivated() {
		return isActivated;
	}

	/**
	 * @param isActivated the isActivated to set
	 */
	public void setActivated(boolean isActivated) {
		this.isActivated = isActivated;
	}
}
