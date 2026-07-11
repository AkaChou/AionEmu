package com.aionemu.gameserver.model.templates.challenge;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 挑战奖励模板（静态数据/XML）。
 * XML template. / XML template.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ChallengeReward")
public class ChallengeReward {
	@XmlAttribute(name = "msg_id")
	protected Integer msgId;

	@XmlAttribute
	protected Integer value;

	@XmlAttribute(required = true)
	protected RewardType type;

	/** 返回消息 ID / Returns the msg id */
	public Integer getMsgId() {
		return this.msgId;
	}

	/** 获取值。 / Returns the value. */
	public Integer getValue() {
		return this.value;
	}

	/** 获取类型。 / Returns the type. */
	public RewardType getType() {
		return this.type;
	}
}
