package com.aionemu.gameserver.skillengine.model;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 飞行限制：技能在飞行/地面状态下的可用范围。
 * Flying restriction: whether skill works in fly, ground or both states.
 *
 * @author kecimis
 */
@XmlType(name = "FlyingRestriction")
@XmlEnum
public enum FlyingRestriction {

	/** 全部状态 / All states */
	ALL,
	/** 仅飞行 / Fly only */
	FLY,
	/** 仅地面 / Ground only */
	GROUND;
}
