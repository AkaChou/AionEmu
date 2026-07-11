package com.aionemu.gameserver.controllers.attack;

/**
 * 攻击结果状态枚举，覆盖主/副手命中、闪避、招架、格挡、抵抗与暴击组合。
 * Attack result status enum covering main/off-hand hit, dodge, parry, block, resist and critical combinations.
 */
public enum AttackStatus {
	/** 主手闪避 / Main-hand dodge */
	DODGE(0, true, false),
	/** 副手闪避 / Off-hand dodge */
	OFFHAND_DODGE(1, true, false),
	/** 主手招架 / Main-hand parry */
	PARRY(2, true, false),
	/** 副手招架 / Off-hand parry */
	OFFHAND_PARRY(3, true, false),
	/** 主手格挡 / Main-hand block */
	BLOCK(4, true, false),
	/** 副手格挡 / Off-hand block */
	OFFHAND_BLOCK(5, true, false),
	/** 主手抵抗 / Main-hand resist */
	RESIST(6, true, false),
	/** 副手抵抗 / Off-hand resist */
	OFFHAND_RESIST(7),
	/** Main-hand BUF / Main-hand BUF */
	BUF(8),
	/** Off-hand BUF / Off-hand BUF */
	OFFHAND_BUF(9),
	/** 主手普通命中 / Main-hand normal hit */
	NORMALHIT(10),
	/** 副手普通命中 / Off-hand normal hit */
	OFFHAND_NORMALHIT(11),
	/** 主手暴击闪避 / Main-hand critical dodge */
	CRITICAL_DODGE(192, true, true),
	/** 主手暴击招架 / Main-hand critical parry */
	CRITICAL_PARRY(194, true, true),
	/** 主手暴击格挡 / Main-hand critical block */
	CRITICAL_BLOCK(196, true, true),
	/** 主手暴击抵抗 / Main-hand critical resist */
	CRITICAL_RESIST(198, false, true),
	/** 主手暴击 / Main-hand critical */
	CRITICAL(202, false, true),
	/** 副手暴击闪避 / Off-hand critical dodge */
	OFFHAND_CRITICAL_DODGE(209, true, true),
	/** 副手暴击招架 / Off-hand critical parry */
	OFFHAND_CRITICAL_PARRY(211, true, true),
	/** 副手暴击格挡 / Off-hand critical block */
	OFFHAND_CRITICAL_BLOCK(213, true, true),
	/** 副手暴击抵抗 / Off-hand critical resist */
	OFFHAND_CRITICAL_RESIST(215, false, true),
	/** 副手暴击 / Off-hand critical */
	OFFHAND_CRITICAL(219, false, true);

	/** 状态类型 ID / Status type id */
	private final int type;
	/** 是否属于反击技能类状态 / Whether this is a counter-skill status */
	private final boolean counterSkill;
	/** 是否为暴击 / Whether this is a critical hit */
	private final boolean isCritical;

	/**
	 * 仅指定类型 ID 的构造（非反击、非暴击）。
	 * Constructs a status with only a type id (non-counter, non-critical).
	 *
	 * type id
	 */
	private AttackStatus(int type) {
		this(type, false, false);
	}

	/**
	 * 完整构造攻击状态。
	 * Fully constructs an attack status.
	 *
	 * type id
	 * @param counterSkill 是否反击技能状态 / whether counter-skill
	 * whether critical
	 */
	private AttackStatus(int type, boolean counterSkill, boolean isCritical) {
		this.type = type;
		this.counterSkill = counterSkill;
		this.isCritical = isCritical;
	}

	/**
	 * 返回状态类型 ID。
	 * Returns the status type id.
	 *
	 * type id
	 */
	public final int getId() {
		return type;
	}

	/**
	 * 是否属于可触发反击技能的防御状态。
	 * Returns whether this status can trigger a counter skill.
	 *
	 * @return 是否反击技能状态 / whether counter-skill
	 */
	public final boolean isCounterSkill() {
		return counterSkill;
	}

	/**
	 * 是否为暴击类状态。
	 * Returns whether this status is critical.
	 *
	 * whether critical
	 */
	public final boolean isCritical() {
		return isCritical;
	}

	/**
	 * 将主手状态映射为对应的副手状态。
	 * Maps a main-hand status to its off-hand counterpart.
	 *
	 * main-hand status
	 * off-hand status
	 *
	 * @param mainHandStatus @throws IllegalArgumentException 无法映射时 / if the status cannot be mapped
	 */
	public static final AttackStatus getOffHandStats(AttackStatus mainHandStatus) {
		switch (mainHandStatus) {
		case DODGE:
			return OFFHAND_DODGE;
		case PARRY:
			return OFFHAND_PARRY;
		case BLOCK:
			return OFFHAND_BLOCK;
		case RESIST:
			return OFFHAND_RESIST;
		case BUF:
			return OFFHAND_BUF;
		case NORMALHIT:
			return OFFHAND_NORMALHIT;
		case CRITICAL:
			return OFFHAND_CRITICAL;
		case CRITICAL_DODGE:
			return OFFHAND_CRITICAL_DODGE;
		case CRITICAL_PARRY:
			return OFFHAND_CRITICAL_PARRY;
		case CRITICAL_BLOCK:
			return OFFHAND_CRITICAL_BLOCK;
		case CRITICAL_RESIST:
			return OFFHAND_CRITICAL_RESIST;
		default:
			break;
		}
		throw new IllegalArgumentException("Invalid mainHandStatus " + mainHandStatus);
	}

	/**
	 * 将任意变体状态归一为基本防御/命中状态（忽略主副手与暴击）。
	 * Normalizes any variant status to its base defensive/hit status (ignores hand and critical).
	 *
	 * original status
	 * base status
	 */
	public static final AttackStatus getBaseStatus(AttackStatus status) {
		switch (status) {
		case DODGE:
		case CRITICAL_DODGE:
		case OFFHAND_DODGE:
		case OFFHAND_CRITICAL_DODGE:
			return AttackStatus.DODGE;
		case PARRY:
		case CRITICAL_PARRY:
		case OFFHAND_PARRY:
		case OFFHAND_CRITICAL_PARRY:
			return AttackStatus.PARRY;
		case BLOCK:
		case CRITICAL_BLOCK:
		case OFFHAND_BLOCK:
		case OFFHAND_CRITICAL_BLOCK:
			return AttackStatus.BLOCK;
		case RESIST:
		case CRITICAL_RESIST:
		case OFFHAND_RESIST:
		case OFFHAND_CRITICAL_RESIST:
			return AttackStatus.RESIST;
		default:
			return status;
		}
	}

	/**
	 * 将基本状态提升为对应的暴击状态。
	 * Elevates a base status to its critical counterpart.
	 *
	 * original status
	 * critical status
	 */
	public static final AttackStatus getCriticalStatusFor(AttackStatus status) {
		switch (status) {
		case DODGE:
			return AttackStatus.CRITICAL_DODGE;
		case OFFHAND_DODGE:
			return AttackStatus.OFFHAND_CRITICAL_DODGE;
		case PARRY:
			return AttackStatus.CRITICAL_PARRY;
		case OFFHAND_PARRY:
			return AttackStatus.OFFHAND_CRITICAL_PARRY;
		case BLOCK:
			return AttackStatus.CRITICAL_BLOCK;
		case OFFHAND_BLOCK:
			return AttackStatus.OFFHAND_CRITICAL_BLOCK;
		case RESIST:
			return AttackStatus.CRITICAL_RESIST;
		case OFFHAND_RESIST:
			return AttackStatus.OFFHAND_CRITICAL_RESIST;
		case NORMALHIT:
			return AttackStatus.CRITICAL;
		case OFFHAND_NORMALHIT:
			return AttackStatus.OFFHAND_CRITICAL;
		default:
			return status;
		}
	}
}
