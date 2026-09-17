package com.aionemu.gameserver.model.templates.serial_guard;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.stats.container.StatEnum;
import com.aionemu.gameserver.skillengine.change.Func;
import lombok.Getter;
import lombok.Setter;

/**
 * 守卫军阶 PenaltyAttr 模板（静态数据/XML）。
 * XML template.
 */

@Getter
@Setter
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "GuardRankPenaltyAttr")
public class GuardRankPenaltyAttr {
	/** 获取属性。 / Returns the stat. */
	@XmlAttribute(required = true)
	protected StatEnum stat;

	/** 返回 func / Returns the func */
	@XmlAttribute(required = true)
	protected Func func;

	/** 获取值。 / Returns the value. */
	@XmlAttribute(required = true)
	protected int value;
}
