package com.aionemu.gameserver.model.templates.mail_reward;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 邮件奖励模板（静态数据/XML）。
 * XML template. / XML template.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "RewardMail")
public class MailRewardTemplate {
	@XmlAttribute(name = "id")
	protected int id;

	@XmlAttribute(name = "name")
	protected String name;

	@XmlAttribute(name = "title")
	protected String title;

	@XmlAttribute(name = "tail")
	protected String tail;

	@XmlAttribute(name = "body")
	protected String body;

	@XmlAttribute(name = "item_id")
	protected int itemId;

	@XmlAttribute(name = "item_count")
	protected int itemCount;

	@XmlAttribute(name = "ap_count")
	protected int apCount;

	@XmlAttribute(name = "kinah_count")
	protected int kinahCount;

	@XmlAttribute(name = "sender")
	protected String sender;

	/** 返回 ID / Returns the id */
	public int getId() {
		return this.id;
	}

	/** 获取名称。 / Returns the name. */
	public String getName() {
		return this.name;
	}

	/** 返回 sender / Returns the sender */
	public String getSender() {
		return this.sender;
	}

	/** 获取称号。 / Returns the title. */
	public String getTitle() {
		return this.title;
	}

	/** 返回 tail / Returns the tail */
	public String getTail() {
		return this.tail;
	}

	/** 返回 body / Returns the body */
	public String getBody() {
		return this.body;
	}

	/** 返回物品 ID / Returns the item id */
	public int getItemId() {
		return this.itemId;
	}

	/** 获取物品计数。 / Returns the item count. */
	public int getItemCount() {
		return this.itemCount;
	}

	/** 获取欧比斯点数计数。 / Returns the ap count. */
	public int getApCount() {
		return this.apCount;
	}

	/** 获取基纳计数。 / Returns the kinah count. */
	public int getKinahCount() {
		return this.kinahCount;
	}
}
