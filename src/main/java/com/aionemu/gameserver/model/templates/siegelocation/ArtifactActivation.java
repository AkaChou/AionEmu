package com.aionemu.gameserver.model.templates.siegelocation;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

/**
 * ArtifactActivation 模板（静态数据/XML）。
 * XML template.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ArtifactActivation")
public class ArtifactActivation {
	@XmlAttribute(name = "itemid")
	protected int itemId;

	@XmlAttribute(name = "count")
	protected int count;

	@XmlAttribute(name = "skill")
	protected int skill;

	@XmlAttribute(name = "cd")
	protected int cd;

	/** 返回物品 ID / Returns the item id */
	public int getItemId() {
		return itemId;
	}

	/** 获取计数。 / Returns the count. */
	public int getCount() {
		return count;
	}

	/** 返回技能 ID / Returns the skill id */
	public int getSkillId() {
		return skill;
	}

	/** 返回 cd / Returns the cd */
	public long getCd() {
		return (long) (cd * 1000);
	}
}
