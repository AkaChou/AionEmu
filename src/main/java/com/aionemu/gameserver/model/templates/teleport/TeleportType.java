package com.aionemu.gameserver.model.templates.teleport;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 传送类型枚举。
 * Teleport Type enumeration.
 *
 * @author ATracer
 */
@XmlType(name = "type")
@XmlEnum
public enum TeleportType {
	/** 常规 / Regular. */
	REGULAR, FLIGHT;
}
