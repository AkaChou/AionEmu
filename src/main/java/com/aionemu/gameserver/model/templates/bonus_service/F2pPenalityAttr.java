package com.aionemu.gameserver.model.templates.bonus_service;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.stats.container.StatEnum;
import com.aionemu.gameserver.skillengine.change.Func;
import lombok.Getter;
import lombok.Setter;

/**
 * F2pPenalityAttr 模板（静态数据/XML）。
 * F2P penalty attribute template (static data/XML).
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "F2pPenalityAttr")
public class F2pPenalityAttr {

	/** 获取属性。 / Returns the stat. */
	@Getter
	@Setter
	@XmlAttribute(required = true)
	protected StatEnum stat;

	/** 返回 func / Returns the func */
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
