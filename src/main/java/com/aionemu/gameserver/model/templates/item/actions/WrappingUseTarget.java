package com.aionemu.gameserver.model.templates.item.actions;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlType;

/**
 * WrappingUseTarget 枚举。
 * Wrapping Use Target enumeration.
 *
 * @author Ranastic
 */

@XmlType(name = "WrappingUseTarget")
@XmlEnum
public enum WrappingUseTarget {
	/** 全部 / All. */
	ALL, ARMOR, OTHER, WEAPON, ACCESSORY, EQUIPMENT;

	private WrappingUseTarget() {
	}

	/** 值。 / Value. */
	public String value() {
		return name();
	}

	/** 值 / From Value*/
	public static WrappingUseTarget fromValue(String v) {
		return valueOf(v);
	}
}
