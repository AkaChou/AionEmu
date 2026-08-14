package com.aionemu.gameserver.controllers.attack;

import com.aionemu.gameserver.skillengine.model.HitType;

/**
 * 单次攻击结果，封装伤害、命中状态、命中类型以及护盾/反射/保护等附加效果数据。
 * Single attack result holding damage, hit status, hit type and shield/reflect/protect side-effect data.
 *
 * @author ATracer, Sippolo, kecimis
 */
public class AttackResult {

	/** 伤害值（可保留小数） / Damage value (may keep fractions) */
	private float damage;

	/** 攻击状态 / Attack status */
	private AttackStatus attackStatus;

	/** 命中类型 / Hit type */
	private HitType hitType = HitType.EVERYHIT;

	/** 护盾类型位掩码 / Shield type bitmask */
	private int shieldType;
	/** 反射伤害 / Reflected damage */
	private int reflectedDamage = 0;
	/** 反射技能 ID / Reflected skill id */
	private int reflectedSkillId = 0;
	/** 保护技能 ID / Protected skill id */
	private int protectedSkillId = 0;
	/** 被保护减免的伤害 / Damage absorbed by protector */
	private int protectedDamage = 0;
	/** 保护者对象 ID / Protector object id */
	private int protectorId = 0;
	/** 护盾消耗的 MP / MP consumed by shield */
	private int shieldMp = 0;

	/** 是否触发子效果 / Whether to launch a sub-effect */
	private boolean launchSubEffect = true;

	/**
	 * 以整数伤害与攻击状态构造结果。
	 * Creates a result with integer damage and attack status.
	 *
	 * @param damage 伤害 / damage
	 * @param attackStatus 攻击状态 / attack status
	 */
	public AttackResult(int damage, AttackStatus attackStatus) {
		this.damage = damage;
		this.attackStatus = attackStatus;
	}

	/**
	 * 以浮点伤害与攻击状态构造结果。
	 * Creates a result with float damage and attack status.
	 *
	 * @param damage 伤害 / damage
	 * @param attackStatus 攻击状态 / attack status
	 */
	public AttackResult(float damage, AttackStatus attackStatus) {
		this.damage = damage;
		this.attackStatus = attackStatus;
	}

	/**
	 * 以整数伤害、攻击状态与命中类型构造结果。
	 * Creates a result with integer damage, attack status and hit type.
	 *
	 * @param damage 伤害 / damage
	 * @param attackStatus 攻击状态 / attack status
	 * @param type 命中类型 / hit type
	 */
	public AttackResult(int damage, AttackStatus attackStatus, HitType type) {
		this(damage, attackStatus);
		this.hitType = type;
	}

	/**
	 * 以浮点伤害、攻击状态与命中类型构造结果。
	 * Creates a result with float damage, attack status and hit type.
	 *
	 * @param damage 伤害 / damage
	 * @param attackStatus 攻击状态 / attack status
	 * @param type 命中类型 / hit type
	 */
	public AttackResult(float damage, AttackStatus attackStatus, HitType type) {
		this(damage, attackStatus);
		this.hitType = type;
	}

	/**
	 * 返回取整后的伤害。
	 * Returns damage as an int.
	 *
	 * @return 整数伤害 / integer damage
	 */
	public int getDamage() {
		return (int) damage;
	}

	/**
	 * 返回精确（浮点）伤害。
	 * Returns the exact float damage.
	 *
	 * @return 精确伤害 / exact damage
	 */
	public float getExactDamage() {
		return damage;
	}

	/**
	 * 设置整数伤害。
	 * Sets damage from an int.
	 *
	 * @param damage 伤害 / damage
	 */
	public void setDamage(int damage) {
		this.damage = damage;
	}

	/**
	 * 设置浮点伤害。
	 * Sets damage from a float.
	 *
	 * @param damage 伤害 / damage
	 */
	public void setDamage(float damage) {
		this.damage = damage;
	}

	/**
	 * 返回攻击状态。
	 * Returns the attack status.
	 *
	 * @return 攻击状态 / attack status
	 */
	public AttackStatus getAttackStatus() {
		return attackStatus;
	}

	/**
	 * 返回命中类型。
	 * Returns the hit type.
	 *
	 * @return 命中类型 / hit type
	 */
	public HitType getDamageType() {
		return hitType;
	}

	/**
	 * 设置命中类型。
	 * Sets the hit type.
	 *
	 * @param type 命中类型 / hit type
	 */
	public void setDamageType(HitType type) {
		this.hitType = type;
	}

	/**
	 * 返回护盾类型位掩码。
	 * Returns the shield type bitmask.
	 *
	 * @return 护盾类型 / shield type
	 */
	public int getShieldType() {
		return shieldType;
	}

	/**
	 * 按位或合并护盾类型标志。
	 * OR-merges a shield type flag into the bitmask.
	 *
	 * @param shieldType 待合并的护盾类型标志 / shield type flag to merge
	 */
	public void setShieldType(int shieldType) {
		this.shieldType |= shieldType;
	}

	/**
	 * 返回反射伤害。
	 * Returns reflected damage.
	 *
	 * @return 反射伤害 / reflected damage
	 */
	public int getReflectedDamage() {
		return this.reflectedDamage;
	}

	/**
	 * 设置反射伤害。
	 * Sets reflected damage.
	 *
	 * @param reflectedDamage 反射伤害 / reflected damage
	 */
	public void setReflectedDamage(int reflectedDamage) {
		this.reflectedDamage = reflectedDamage;
	}

	/**
	 * 返回反射技能 ID。
	 * Returns the reflected skill id.
	 *
	 * @return 反射技能 ID / reflected skill id
	 */
	public int getReflectedSkillId() {
		return this.reflectedSkillId;
	}

	/**
	 * 设置反射技能 ID。
	 * Sets the reflected skill id.
	 *
	 * @param skillId 技能 ID / skill id
	 */
	public void setReflectedSkillId(int skillId) {
		this.reflectedSkillId = skillId;
	}

	/**
	 * 返回保护技能 ID。
	 * Returns the protected skill id.
	 *
	 * @return 保护技能 ID / protected skill id
	 */
	public int getProtectedSkillId() {
		return this.protectedSkillId;
	}

	/**
	 * 设置保护技能 ID。
	 * Sets the protected skill id.
	 *
	 * @param skillId 技能 ID / skill id
	 */
	public void setProtectedSkillId(int skillId) {
		this.protectedSkillId = skillId;
	}

	/**
	 * 返回被保护减免的伤害。
	 * Returns damage absorbed by a protector.
	 *
	 * @return 被保护伤害 / protected damage
	 */
	public int getProtectedDamage() {
		return this.protectedDamage;
	}

	/**
	 * 设置被保护减免的伤害。
	 * Sets damage absorbed by a protector.
	 *
	 * @param protectedDamage 被保护伤害 / protected damage
	 */
	public void setProtectedDamage(int protectedDamage) {
		this.protectedDamage = protectedDamage;
	}

	/**
	 * 返回保护者对象 ID。
	 * Returns the protector object id.
	 *
	 * @return 保护者对象 ID / protector id
	 */
	public int getProtectorId() {
		return this.protectorId;
	}

	/**
	 * 设置保护者对象 ID。
	 * Sets the protector object id.
	 *
	 * @param protectorId 保护者对象 ID / protector id
	 */
	public void setProtectorId(int protectorId) {
		this.protectorId = protectorId;
	}

	/**
	 * 是否触发子效果。
	 * Returns whether a sub-effect should be launched.
	 *
	 * @return 是否触发子效果 / whether to launch sub-effect
	 */
	public boolean isLaunchSubEffect() {
		return launchSubEffect;
	}

	/**
	 * 设置是否触发子效果。
	 * Sets whether a sub-effect should be launched.
	 *
	 * @param launchSubEffect 是否触发子效果 / whether to launch
	 */
	public void setLaunchSubEffect(boolean launchSubEffect) {
		this.launchSubEffect = launchSubEffect;
	}

	/**
	 * 返回护盾消耗的 MP。
	 * Returns MP consumed by the shield.
	 *
	 * @return 护盾消耗 MP / shield MP
	 */
	public int getShieldMp() {
		return this.shieldMp;
	}

	/**
	 * 设置护盾消耗的 MP。
	 * Sets MP consumed by the shield.
	 *
	 * @param shieldMp 护盾消耗 MP / shield MP
	 */
	public void setShieldMp(int shieldMp) {
		this.shieldMp = shieldMp;
	}
}
