package com.aionemu.gameserver.controllers.observer;

import java.util.List;

import com.aionemu.gameserver.controllers.attack.AttackResult;
import com.aionemu.gameserver.controllers.attack.AttackStatus;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.skillengine.model.Effect;

/**
 * 攻击结算观察者基类，可改写状态判定、护盾与伤害倍率。
 * Base attack-calculation observer that can override status checks, shields and damage multipliers.
 *
 * @author ATracer
 */
public class AttackCalcObserver {

	/**
	 * 检查防御方攻击状态是否命中（如格挡、闪避触发）。
	 * Check whether a defender attack status matches (e.g. block/dodge trigger).
	 *
	 * attack status
	 * default false
	 */
	public boolean checkStatus(AttackStatus status) {
		return false;
	}

	/**
	 * 检查并应用护盾/反射/保护等对攻击列表的影响。
	 * Check and apply shield/reflect/protect effects on the attack list.
	 *
	 * @param attackList 攻击结果列表 / attack result list
	 * associated effect
	 * attacker
	 */
	public void checkShield(List<AttackResult> attackList, Effect effect, Creature attacker) {

	}

	/**
	 * 检查攻击方攻击状态是否命中。
	 * Check whether an attacker attack status matches.
	 *
	 * attack status
	 * default false
	 */
	public boolean checkAttackerStatus(AttackStatus status) {
		return false;
	}

	/**
	 * 检查攻击方暴击状态。
	 * Check attacker critical status.
	 *
	 * attack status
	 *
	 * @param isSkill 是否技能攻击 / whether skill attack
	 * @param isSkill
	 * @return 暴击状态（默认失败） / critical status (default fail)
	 */
	public AttackerCriticalStatus checkAttackerCriticalStatus(AttackStatus status, boolean isSkill) {
		return new AttackerCriticalStatus(false);
	}

	/**
	 * 获取基础物理伤害倍率。
	 * Get base physical damage multiplier.
	 *
	 * @param isSkill 是否技能攻击 / whether skill attack
	 * @return 物理伤害倍率 / physical damage multiplier
	 */
	public float getBasePhysicalDamageMultiplier(boolean isSkill) {
		return 1f;
	}

	/**
	 * 获取基础魔法伤害倍率。
	 * Get base magical damage multiplier.
	 *
	 * @return 魔法伤害倍率 / magical damage multiplier
	 */
	public float getBaseMagicalDamageMultiplier() {
		return 1f;
	}
}
