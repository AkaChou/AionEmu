package com.aionemu.gameserver.model.templates.item;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 装备类型：防具/武器/烙印之石/时装/饰品。
 * Equip type: armor/weapon/stigma/estima/accessory.
 *
 * @author ATracer
 */
@XmlType(name = "equipType")
@XmlEnum
public enum EquipType {

	/** 防具 / Armor */
	ARMOR,
	/** 武器 / Weapon */
	WEAPON,
	/** 烙印之石 / Stigma */
	STIGMA,
	/** 时装 / Estima */
	ESTIMA,
	/** 饰品 / Accessory */
	ACCESSORY,
	/** 无 / None */
	NONE;

	/** 返回枚举名。 / Returns the enum name. */
	public String value() {
		return name();
	}

	/** 按名称解析 / From value */
	public static EquipType fromValue(String v) {
		return valueOf(v);
	}
}
