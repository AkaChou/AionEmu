package com.aionemu.gameserver.model.templates.item.actions;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlType;

/**
 * UseTarget 枚举。
 * Use Target enumeration.
 *
 * @author Ranastic
 */

@XmlType(name = "UseTarget")
@XmlEnum
public enum UseTarget {
	/** 全部 / All. */
	ALL, WING, PLUME, OTHER, ARMOR, WEAPON, BRACELET, ACCESSORY, EQUIPMENT;

	private UseTarget() {
	}

	/** 值。 / Value. */
	public String value() {
		return name();
	}

	/** 值 / From Value*/
	public static UseTarget fromValue(String v) {
		return valueOf(v);
	}
}
