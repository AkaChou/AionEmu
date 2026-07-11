package com.aionemu.gameserver.model.templates.npcskill;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlType;

/**
 * Conjunction 类型枚举。
 * Conjunction Type enumeration.
 *
 * @author nrg
 */

@XmlType(name = "ConjunctionType")
@XmlEnum
public enum ConjunctionType {

	/** 且 / And. */
	AND, OR, XOR;

	/** 值。 / Value. */
	public String value() {
		return name();
	}

	/** 值 / From Value*/
	public static ConjunctionType fromValue(String v) {
		return valueOf(v);
	}
}
