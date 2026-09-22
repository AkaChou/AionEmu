package com.aionemu.gameserver.model.templates.housing;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlType;
import lombok.Getter;

/**
 * Building 类型枚举。
 * Building Type enumeration.
 * @author Rolandas
 */
@Getter
@XmlType(name = "BuildingType")
@XmlEnum
public enum BuildingType {
	/** 个人田地 / Personal Field */
	PERSONAL_FIELD(2),
	/** 个人住宅 / Personal Ins */
	PERSONAL_INS(1);

	/** 返回 ID / Returns the id */
	private final int id;

	BuildingType(int id) {
		this.id = id;
	}
}
