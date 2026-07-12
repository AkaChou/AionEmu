package com.aionemu.gameserver.model.templates.serial_killer;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 军阶 Restriction 模板（静态数据/XML）。
 * XML template.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "RankRestriction", propOrder = { "penaltyAttr" })
public class RankRestriction {
	@XmlElement(name = "penalty_attr")
	protected List<RankPenaltyAttr> penaltyAttr;

	@XmlAttribute(name = "rank_num", required = true)
	protected int rankNum;

	/** 返回 penalty attr / Returns the penalty attr */
	public List<RankPenaltyAttr> getPenaltyAttr() {
		if (penaltyAttr == null) {
			penaltyAttr = new ArrayList<RankPenaltyAttr>();
		}
		return this.penaltyAttr;
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
