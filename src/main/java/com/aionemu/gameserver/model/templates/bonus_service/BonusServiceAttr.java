package com.aionemu.gameserver.model.templates.bonus_service;

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
 * 加成服务 Attr 模板（静态数据/XML）。
 * Bonus service attribute template (static data/XML).
 *
 * @author Ranastic (Encom)
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "BonusServiceAttr", propOrder = { "bonusAttr" })
public class BonusServiceAttr {
	@XmlElement(name = "bonus_attr")
	protected List<BonusPenaltyAttr> bonusAttr;

	/** 返回增益 ID / Returns the buff id */
	@Getter
	@Setter
	@XmlAttribute(name = "buff_id", required = true)
	protected int buffId;

	/** 返回 penalty attr / Returns the penalty attr */
	public List<BonusPenaltyAttr> getPenaltyAttr() {
		if (bonusAttr == null) {
			bonusAttr = new ArrayList<BonusPenaltyAttr>();
		}
		return bonusAttr;
	}
}
