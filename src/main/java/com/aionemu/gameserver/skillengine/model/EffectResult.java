package com.aionemu.gameserver.skillengine.model;

import lombok.Getter;

/**
 * 效果结算结果：普通命中、被吸收或冲突覆盖。
 * Effect result: normal hit, absorbed, or conflict override.
 * @author Cheatkiller
 */
@Getter
public enum EffectResult {

	/** 普通 / Normal */
	NORMAL(0),
	/** 被吸收 / Absorbed */
	ABSORBED(1),
	/** 冲突（覆盖） / Conflict (override) */
	CONFLICT(2);

	/**
	 * 获取协议 ID。
	 * Gets protocol id.
	 */
	private final int id;

	EffectResult(int id) {
		this.id = id;
	}
}
