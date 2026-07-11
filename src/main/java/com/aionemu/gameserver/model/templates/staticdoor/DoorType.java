package com.aionemu.gameserver.model.templates.staticdoor;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlType;

/**
 * Door 类型枚举。
 * Door Type enumeration.
 *
 * @author Rolandas
 */
@XmlType(name = "DoorType")
@XmlEnum
public enum DoorType {
	/** 门 / Door. */
	DOOR, ABYSS, HOUSE
}
