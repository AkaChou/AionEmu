package com.aionemu.gameserver.model.templates.challenge;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;
import lombok.Getter;

/**
 * 挑战奖励模板（静态数据/XML）。
 * XML template.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ChallengeReward")
public class ChallengeReward {
	/** 返回消息 ID / Returns the msg id */
	@Getter
	@XmlAttribute(name = "msg_id")
	protected Integer msgId;

	/** 获取值。 / Returns the value. */
	@Getter
	@XmlAttribute
	protected Integer value;

	/** 获取类型。 / Returns the type. */
	@Getter
	@XmlAttribute(required = true)
	protected RewardType type;
}
