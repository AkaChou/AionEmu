package com.aionemu.gameserver.model.templates.instance_bonusatrr;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 副本加成属性模板（静态数据/XML）。
 * Instance bonus attr template (static data/XML).
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "InstanceBonusAttr", propOrder = { "penaltyAttr" })
public class InstanceBonusAttr {
	@XmlElement(name = "penalty_attr")
	protected List<InstancePenaltyAttr> penaltyAttr;

	@XmlAttribute(name = "buff_id", required = true)
	protected int buffId;

	/** 返回属性修正列表。 / Returns the penalty attrs. */
	public List<InstancePenaltyAttr> getPenaltyAttr() {
		if (penaltyAttr == null) {
			penaltyAttr = new ArrayList<InstancePenaltyAttr>();
		}
		return this.penaltyAttr;
	}

	/** 返回增益 ID。 / Returns the buff id. */
	public int getBuffId() {
		return buffId;
	}

	/** 设置增益 ID。 / Sets the buff id. */
	public void setBuffId(int value) {
		this.buffId = value;
	}
}
