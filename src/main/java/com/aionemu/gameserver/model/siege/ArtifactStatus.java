package com.aionemu.gameserver.model.siege;

/**
 * 神器状态枚举。
 * Artifact Status enumeration.
 *
 * @author MrPoke
 */
public enum ArtifactStatus {
	/** 空闲。 / Idle. */
	IDLE(0),
	/** 激活中。 / Activation. */
	ACTIVATION(1),
	/** 施法中。 / Casting. */
	CASTING(2),
	/** 已激活。 / Activated. */
	ACTIVATED(3);

	private int id;

	ArtifactStatus(int id) {
		this.id = id;
	}

	/** 获取值。 / Returns the value. */
	public int getValue() {
		return id;
	}
}
