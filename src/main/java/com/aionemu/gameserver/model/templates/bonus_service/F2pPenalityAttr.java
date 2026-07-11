package com.aionemu.gameserver.model.templates.bonus_service;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.stats.container.StatEnum;
import com.aionemu.gameserver.skillengine.change.Func;

/**
 * F2pPenalityAttr 模板（静态数据/XML）。
 * XML template. / XML template.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "F2pPenalityAttr")
public class F2pPenalityAttr {

	@XmlAttribute(required = true)
	protected StatEnum stat;

	@XmlAttribute(required = true)
	protected Func func;

	@XmlAttribute(required = true)
	protected int value;

	/** 获取属性。 / Returns the stat. */
	public StatEnum getStat() {
		return stat;
	}

	/** 设置属性。 / Sets the stat. */
	public void setStat(StatEnum value) {
		stat = value;
	}

	/** 返回 func / Returns the func */
	public Func getFunc() {
		return func;
	}

	/** 设置 func / Sets the func */
	public void setFunc(Func value) {
		func = value;
	}

	/** 获取值。 / Returns the value. */
	public int getValue() {
		return value;
	}

	/** 设置值。 / Sets the value. */
	public void setValue(int value) {
		this.value = value;
	}
}
