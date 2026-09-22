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
 * Players 加成 PenaltyAttr 模板（静态数据/XML）。
 * Players bonus penalty attribute template (static data/XML).
 * @author Ranastic (Encom)
 */

@Getter
@Setter
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "PlayersBonusPenaltyAttr")
public class PlayersBonusPenaltyAttr {
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
