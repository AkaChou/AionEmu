package com.aionemu.gameserver.model.templates.challenge;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;
import lombok.Getter;

/**
 * 挑战任务模板（静态数据/XML）。
 * XML template.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ChallengeQuest")
public class ChallengeQuestTemplate {
	/** 返回分数 / Returns the score */
	@Getter
	@XmlAttribute(required = true)
	protected int score;

	/** 返回重复次数 / Returns the repeat count */
	@Getter
	@XmlAttribute(name = "repeat_count", required = true)
	protected int repeatCount;

	/** 返回 ID / Returns the id */
	@Getter
	@XmlAttribute(required = true)
	protected int id;
}
