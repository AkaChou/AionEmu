package com.aionemu.gameserver.skillengine.effect;

/**
 * 异常状态位掩码枚举：定义控制/持续负面状态及其组合位。
 * Abnormal state bit-mask enum: control/DoT states and composite masks.
 */
public enum AbnormalState {

	BUFF(0), POISON(1), BLEED(2), PARALYZE(4), SLEEP(8), ROOT(16), BLIND(32), UNKNOWN(64), DISEASE(128), SILENCE(256),
	FEAR(512), CURSE(1024), CONFUSE(2048), STUN(4096), PETRIFICATION(8192), STUMBLE(16384), STAGGER(32768),
	OPENAERIAL(65536), SNARE(131072), SLOW(262144), SPIN(524288), BIND(1048576), DEFORM(2097152), CANNOT_MOVE(4194304),
	NOFLY(8388608), KNOCKBACK(16777216), HIDE(536870912),
	STUNLIKE(STUN.id | STUMBLE.id | STAGGER.id | OPENAERIAL.id | SPIN.id),

	/** 无法攻击的状态组合 / States that prevent attacking */
	CANT_ATTACK_STATE(SPIN.id | SLEEP.id | STUN.id | PETRIFICATION.id | STUMBLE.id | STAGGER.id | OPENAERIAL.id | PARALYZE.id | FEAR.id
			| CANNOT_MOVE.id | CONFUSE.id),
	/** 无法移动的状态组合 / States that prevent movement */
	CANT_MOVE_STATE(SPIN.id | ROOT.id | SLEEP.id | STUMBLE.id | STUN.id | PETRIFICATION.id | STAGGER.id | OPENAERIAL.id | PARALYZE.id
			| CANNOT_MOVE.id),
	/** 强制下坐骑的状态组合 / States that force dismount */
	DISMOUT_RIDE(SPIN.id | ROOT.id | SLEEP.id | STUMBLE.id | STUN.id | PETRIFICATION.id | STAGGER.id | OPENAERIAL.id | PARALYZE.id
			| CANNOT_MOVE.id | FEAR.id | SNARE.id | CONFUSE.id);

	private int id;

	private AbnormalState(int id) {
		this.id = id;
	}

	/**
	 * 获取异常状态 ID（位掩码值）。
	 * Returns the abnormal state id (bit-mask value).
	 *
	 * state id
	 */
	public int getId() {
		return id;
	}

	/**
	 * 按枚举名查找异常状态。
	 * Looks up an abnormal state by enum name.
	 *
	 * state name
	 *
	 * @param name
	 * @return 匹配的状态，未找到返回 null / matching state, or null if not found
	 */
	public static AbnormalState getIdByName(String name) {
		for (AbnormalState id : values()) {
			if (id.name().equals(name)) {
				return id;
			}
		}
		return null;
	}

	/**
	 * 按 ID 查找异常状态。
	 * Looks up an abnormal state by id.
	 *
	 * @param id 状态 ID / state id
	 * @return 匹配的状态，未找到返回 null / matching state, or null if not found
	 */
	public static AbnormalState getStateById(int id) {
		for (AbnormalState as : values()) {
			if (as.getId() == id) {
				return as;
			}
		}
		return null;
	}
}
