package com.aionemu.gameserver.model.templates.challenge;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 挑战任务模板（静态数据/XML）。
 * XML template. / XML template.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ChallengeQuest")
public class ChallengeQuestTemplate {
	@XmlAttribute(required = true)
	protected int score;

	@XmlAttribute(name = "repeat_count", required = true)
	protected int repeatCount;

	@XmlAttribute(required = true)
	protected int id;

	/** 返回 score / Returns the score */
	public int getScore() {
		return this.score;
	}

	/** 返回 repeat count / Returns the repeat count */
	public int getRepeatCount() {
		return this.repeatCount;
	}

	/** 返回 ID / Returns the id */
	public int getId() {
		return this.id;
	}
}
