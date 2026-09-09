package com.aionemu.gameserver.model.templates.npc;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;
import lombok.Getter;

/**
 * MassiveLooting 模板（静态数据/XML）。
 * XML template.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "MassiveLooting")
public class MassiveLooting {
	/** 返回物品 ID / Returns the itemid */
	@Getter
	@XmlAttribute
	protected int itemid;

	/** 返回 looting num / Returns the looting num */
	@Getter
	@XmlAttribute(name = "looting_num")
	protected int lootingNum;

	/** 获取最小等级。 / Returns the min level. */
	@Getter
	@XmlAttribute(name = "min_level")
	protected int minLevel;

	/** 获取最大等级。 / Returns the max level. */
	@Getter
	@XmlAttribute(name = "max_level")
	protected int maxLevel;
}
