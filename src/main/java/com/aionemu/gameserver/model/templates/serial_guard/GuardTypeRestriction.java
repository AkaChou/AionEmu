package com.aionemu.gameserver.model.templates.serial_guard;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 守卫类型 Restriction 模板（静态数据/XML）。
 * XML template. / XML template.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "GuardTypeRestriction", propOrder = { "guardpenaltyAttr" })
public class GuardTypeRestriction {
	@XmlElement(name = "guard_penalty_attr")
	protected List<GuardTypePenaltyAttr> guardpenaltyAttr;

	@XmlAttribute(name = "type_num", required = true)
	protected int typeNum;

	/** 返回 guard penalty attr / Returns the guard penalty attr */
	public List<GuardTypePenaltyAttr> getGuardPenaltyAttr() {
		if (guardpenaltyAttr == null) {
			guardpenaltyAttr = new ArrayList<GuardTypePenaltyAttr>();
		}
		return this.guardpenaltyAttr;
	}

	/** 返回 type num / Returns the type num */
	public int getTypeNum() {
		return typeNum;
	}

	/** 设置 type num / Sets the type num */
	public void setTypeNum(int value) {
		this.typeNum = value;
	}
}
