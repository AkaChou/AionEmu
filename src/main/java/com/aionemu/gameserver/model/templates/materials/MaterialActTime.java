package com.aionemu.gameserver.model.templates.materials;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 材料 ActTime 枚举。
 * Material Act Time enumeration.
 *
 * @author Rolandas
 */
@XmlType(name = "DayTime")
@XmlEnum
public enum MaterialActTime {

	/** 天 / Day. */
	DAY, NIGHT;

	/** 值。 / Value. */
	public String value() {
		return name();
	}

	/** 值 / From Value*/
	public static MaterialActTime fromValue(String value) {
		return valueOf(value);
	}
}
