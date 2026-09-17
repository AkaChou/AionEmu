package com.aionemu.gameserver.model.templates.mail_reward;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;
import lombok.Getter;

/**
 * 邮件奖励模板（静态数据/XML）。
 * Mail reward template (static data/XML).
 */

@Getter
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "RewardMail")
public class MailRewardTemplate {
	/** 返回 ID / Returns the id */
	@XmlAttribute(name = "id")
	protected int id;

	/** 获取名称。 / Returns the name. */
	@XmlAttribute(name = "name")
	protected String name;

	/** 获取称号。 / Returns the title. */
	@XmlAttribute(name = "title")
	protected String title;

	/** 返回 tail / Returns the tail */
	@XmlAttribute(name = "tail")
	protected String tail;

	/** 返回 body / Returns the body */
	@XmlAttribute(name = "body")
	protected String body;

	/** 返回物品 ID / Returns the item id */
	@XmlAttribute(name = "item_id")
	protected int itemId;

	/** 获取物品计数。 / Returns the item count. */
	@XmlAttribute(name = "item_count")
	protected int itemCount;

	/** 获取欧比斯点数计数。 / Returns the ap count. */
	@XmlAttribute(name = "ap_count")
	protected int apCount;

	/** 获取基纳计数。 / Returns the kinah count. */
	@XmlAttribute(name = "kinah_count")
	protected int kinahCount;

	/** 返回 sender / Returns the sender */
	@XmlAttribute(name = "sender")
	protected String sender;
}
