package com.aionemu.gameserver.controllers.attack;

/**
 * 反击技能状态枚举，对应客户端可触发的防御类反击类型。
 * Counter-skill status enum for client-side defensive counter types.
 */
public enum CounterSkillStatus {

	/** 格挡 / Block */
	BLOCK(32),
	/** 招架 / Parry */
	PARRY(64),
	/** 闪避 / Dodge */
	DODGE(128),
	/** 魔法抵抗 / Resist */
	RESIST(256);

	/** 状态类型 ID / Status type id */
	private final int type;

	/**
	 * 构造反击技能状态。
	 * Constructs a counter-skill status.
	 *
	 * @param type 类型 ID / type id
	 */
	private CounterSkillStatus(int type) {
		this.type = type;
	}

	/**
	 * 返回状态类型 ID。
	 * Returns the status type id.
	 *
	 * @return 类型 ID / type id
	 */
	public final int getId() {
		return type;
	}
}
