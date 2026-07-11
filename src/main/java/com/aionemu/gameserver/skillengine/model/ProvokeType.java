package com.aionemu.gameserver.skillengine.model;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 激怒触发时机：攻击时或被攻击时。
 * Provoke timing: on attack or when attacked.
 *
 * @author ATracer
 */
@XmlType(name = "ProvokeType")
@XmlEnum
public enum ProvokeType {

	/** 攻击时 / On attack */
	ATTACK,
	/** 被攻击时 / When attacked */
	ATTACKED;
}
