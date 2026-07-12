package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlEnum;

/**
 * 召唤物归属范围：个人/小队/军团/联盟。
 * group / legion / alliance.
 *
 * @author Rolandas
 */
@XmlEnum
public enum SummonOwner {

	/** 个人 / Private */
	PRIVATE,
	/** 小队 / Group */
	GROUP,
	/** 军团 / Legion */
	LEGION,
	/** 联盟 / Alliance */
	ALLIANCE
}
