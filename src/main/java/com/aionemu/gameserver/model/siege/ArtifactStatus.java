package com.aionemu.gameserver.model.siege;

/**
 * 神器状态枚举。
 * Artifact Status enumeration.
 *
 * @author MrPoke
 */
public enum ArtifactStatus {
	/** 空闲 / Idle. */
	IDLE(0), ACTIVATION(1), CASTING(2), ACTIVATED(3);

	private int id;

	ArtifactStatus(int id) {
		this.id = id;
	}

	/** 获取值。 / Returns the value. */
	public int getValue() {
		return id;
	}
}
