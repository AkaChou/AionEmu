package com.aionemu.gameserver.model.templates.zone;

import lombok.Getter;

/**
 * 区域类型枚举。
 * Zone Type enumeration.
 * @author MrPoke
 */
@Getter
public enum ZoneType {

	/**
	 * 区域类型：飞行 / 伤害区域 / 水域 / 攻城区域 / PvP 区域。
	 * Zone types: fly / damage / water / siege / pvp.
	 */
	FLY(0), DAMAGE(1), WATER(2), SIEGE(3), PVP(4);

	private final byte value;

	ZoneType(int value) {
		this.value = (byte) value;
	}
}
