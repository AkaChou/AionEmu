package com.aionemu.gameserver.model.templates.pet;

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
 * 宠物加成属性模板（静态数据/XML）。
 * Pet bonus attribute template (static data / XML).
 */

@Getter
@Setter
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "PetBonusAttr", propOrder = { "penaltyAttr" })
public class PetBonusAttr {

	@XmlElement(name = "penalty_attr")
	protected List<PetPenaltyAttr> penaltyAttr;

	/** 返回增益 ID / Returns the buff id */
	@XmlAttribute(name = "buff_id", required = true)
	protected int buffId;

	/** 返回食物数量 / Returns the food count */
	@XmlAttribute(name = "food_count", required = true)
	protected int foodCount;

	/** 返回惩罚属性列表 / Returns the penalty attr */
	public List<PetPenaltyAttr> getPenaltyAttr() {
		if (penaltyAttr == null) {
			penaltyAttr = new ArrayList<>();
		}
		return this.penaltyAttr;
	}
}
