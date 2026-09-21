package com.aionemu.gameserver.model.templates.serial_killer;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;
import lombok.Getter;
import lombok.Setter;

/**
 * 军阶限制模板（静态数据/XML）。
 * XML template.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "RankRestriction", propOrder = { "penaltyAttr" })
public class RankRestriction {
	@XmlElement(name = "penalty_attr")
	protected List<RankPenaltyAttr> penaltyAttr;

	/** 返回军阶编号 / Returns the rank num */
	@Getter
	@Setter
	@XmlAttribute(name = "rank_num", required = true)
	protected int rankNum;

	/** 返回惩罚属性列表 / Returns the penalty attr */
	public List<RankPenaltyAttr> getPenaltyAttr() {
		if (penaltyAttr == null) {
			penaltyAttr = new ArrayList<>();
		}
		return this.penaltyAttr;
	}
}
