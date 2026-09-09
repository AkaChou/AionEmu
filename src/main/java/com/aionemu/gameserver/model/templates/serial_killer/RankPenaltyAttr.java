package com.aionemu.gameserver.model.templates.serial_killer;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.stats.container.StatEnum;
import com.aionemu.gameserver.skillengine.change.Func;
import lombok.Getter;
import lombok.Setter;

/**
 * 军阶惩罚属性模板（静态数据/XML）。
 * XML template.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "RankPenaltyAttr")
public class RankPenaltyAttr {
	/** 获取属性。 / Returns the stat. */
	@Getter
	@Setter
	@XmlAttribute(required = true)
	protected StatEnum stat;

	/** 返回修正方式 / Returns the func */
	@Getter
	@Setter
	@XmlAttribute(required = true)
	protected Func func;

	/** 获取值。 / Returns the value. */
	@Getter
	@Setter
	@XmlAttribute(required = true)
	protected int value;
}
