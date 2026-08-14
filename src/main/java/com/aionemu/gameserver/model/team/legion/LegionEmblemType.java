package com.aionemu.gameserver.model.team.legion;

/**
 * 军团徽章类型枚举。
 * Legion Emblem Type enumeration.
 *
 * @author cura
 */
public enum LegionEmblemType {
	/** 默认 / Default. */
	DEFAULT(0x00),
	/** 自定义 / Custom. */
	CUSTOM(0x80);

	private byte value;

	private LegionEmblemType(int value) {
		this.value = (byte) value;
	}

	/** 获取值。 / Returns the value. */
	public byte getValue() {
		return value;
	}
}
