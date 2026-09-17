package com.aionemu.gameserver.model.team.legion;

import lombok.Getter;

/**
 * 军团徽章类型枚举。
 * Legion Emblem Type enumeration.
 *
 * @author cura
 */
@Getter
public enum LegionEmblemType {
	/** 默认 / Default. */
	DEFAULT(0x00),
	/** 自定义 / Custom. */
	CUSTOM(0x80);

	/** 获取值。 / Returns the value. */
	private final byte value;

	LegionEmblemType(int value) {
		this.value = (byte) value;
	}
}
