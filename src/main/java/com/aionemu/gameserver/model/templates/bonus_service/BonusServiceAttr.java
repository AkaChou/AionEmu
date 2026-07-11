package com.aionemu.gameserver.model.templates.bonus_service;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 加成服务 Attr 模板（静态数据/XML）。
 * XML template. / XML template.
 *
 * @author Ranastic (Encom)
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "BonusServiceAttr", propOrder = { "bonusAttr" })
public class BonusServiceAttr {
	@XmlElement(name = "bonus_attr")
	protected List<BonusPenaltyAttr> bonusAttr;

	@XmlAttribute(name = "buff_id", required = true)
	protected int buffId;

	/** 返回 penalty attr / Returns the penalty attr */
	public List<BonusPenaltyAttr> getPenaltyAttr() {
		if (bonusAttr == null) {
			bonusAttr = new ArrayList<BonusPenaltyAttr>();
		}
		return bonusAttr;
	}

	/** 返回增益 ID / Returns the buff id */
	public int getBuffId() {
		return buffId;
	}

	/** 设置 buff id / Sets the buff id */
	public void setBuffId(int value) {
		buffId = value;
	}
}
