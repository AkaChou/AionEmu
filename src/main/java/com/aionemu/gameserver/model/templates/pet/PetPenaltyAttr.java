package com.aionemu.gameserver.model.templates.pet;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.stats.container.StatEnum;
import com.aionemu.gameserver.skillengine.change.Func;
import lombok.Getter;
import lombok.Setter;

/**
 * 宠物惩罚属性模板（静态数据/XML）。
 * Pet penalty attribute template (static data / XML).
 */

@Getter
@Setter
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "PetPenaltyAttr")
public class PetPenaltyAttr {

	/** 获取属性。 / Returns the stat. */
	@XmlAttribute(required = true)
	protected StatEnum stat;

	/** 返回加成函数 / Returns the func */
	@XmlAttribute(required = true)
	protected Func func;

	/** 获取值。 / Returns the value. */
	@XmlAttribute(required = true)
	protected int value;
}
