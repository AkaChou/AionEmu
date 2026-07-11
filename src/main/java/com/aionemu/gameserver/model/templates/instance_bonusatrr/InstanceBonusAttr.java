package com.aionemu.gameserver.model.templates.instance_bonusatrr;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 副本加成 Attr 模板（静态数据/XML）。
 * XML template. / XML template.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "InstanceBonusAttr", propOrder = { "penaltyAttr" })
public class InstanceBonusAttr {
	@XmlElement(name = "penalty_attr")
	protected List<InstancePenaltyAttr> penaltyAttr;

	@XmlAttribute(name = "buff_id", required = true)
	protected int buffId;

	/** 返回 penalty attr / Returns the penalty attr */
	public List<InstancePenaltyAttr> getPenaltyAttr() {
		if (penaltyAttr == null) {
			penaltyAttr = new ArrayList<InstancePenaltyAttr>();
		}
		return this.penaltyAttr;
	}

	/** 返回增益 ID / Returns the buff id */
	public int getBuffId() {
		return buffId;
	}

	/** 设置 buff id / Sets the buff id */
	public void setBuffId(int value) {
		this.buffId = value;
	}
}
