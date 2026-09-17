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
import lombok.Getter;
import lombok.Setter;

/**
 * 指南模板（静态数据/XML）。
 * Guide template (static data/XML).
 *
 * @author xTz
 */
@Getter
@Setter
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "GuideTemplate")
public class GuideTemplate {

	/**
	 * @return the level
	 */
	@XmlAttribute(name = "level")
	private int level;
	@XmlAttribute(name = "classType")
	private PlayerClass classType;
	/**
	 * @return the title
	 */
	@XmlAttribute(name = "title")
	private String title;
	/**
	 * @return the race
	 */
	@XmlAttribute(name = "race")
	private Race race;
	/**
	 * @return the select
	 */
	@XmlElement(name = "reward_info")
	private String rewardInfo = StringUtils.EMPTY;
	/**
	 * @return the message
	 */
	@XmlElement(name = "message")
	private String message = StringUtils.EMPTY;
	/**
	 * @return the select
	 */
	@XmlElement(name = "select")
	private String select = StringUtils.EMPTY;
	/**
	 * @return the surveys
	 */
	@XmlElement(name = "survey")
	private List<SurveyTemplate> surveys;
	/** 获取奖励计数。 / Returns the reward count. */
	@XmlAttribute(name = "rewardCount")
	private int rewardCount;
	/**
	 * @return the isActivated
	 */
	@XmlTransient
	private boolean isActivated = true;

	/**
	 * @return the classId
	 */
	public PlayerClass getPlayerClass() {
		return this.classType;
	}
}
