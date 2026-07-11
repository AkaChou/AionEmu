package com.aionemu.gameserver.model.templates.serial_guard;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 守卫军阶 Restriction 模板（静态数据/XML）。
 * XML template. / XML template.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "GuardRankRestriction", propOrder = { "guardpenaltyAttr" })
public class GuardRankRestriction {
	@XmlElement(name = "guard_penalty_attr")
	protected List<GuardRankPenaltyAttr> guardpenaltyAttr;

	@XmlAttribute(name = "rank_num", required = true)
	protected int rankNum;

	/** 返回 guard penalty attr / Returns the guard penalty attr */
	public List<GuardRankPenaltyAttr> getGuardPenaltyAttr() {
		if (guardpenaltyAttr == null) {
			guardpenaltyAttr = new ArrayList<GuardRankPenaltyAttr>();
		}
		return this.guardpenaltyAttr;
	}

	/** 返回 rank num / Returns the rank num */
	public int getRankNum() {
		return rankNum;
	}

	/** 设置 rank num / Sets the rank num */
	public void setRankNum(int value) {
		this.rankNum = value;
	}
}
