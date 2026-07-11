package com.aionemu.gameserver.skillengine.effect;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.controllers.attack.AttackUtil;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ATTACK_STATUS.LOG;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ATTACK_STATUS.TYPE;
import com.aionemu.gameserver.skillengine.effect.modifier.ActionModifier;
import com.aionemu.gameserver.skillengine.model.Effect;

/**
 * 延迟法术攻击效果：延迟后对敌对目标结算魔法伤害。
 * Delayed spell attack: after a delay, applies magical damage to an enemy target.
 *
 * @author ATracer
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "DelayedSpellAttackInstantEffect")
public class DelayedSpellAttackInstantEffect extends DamageEffect {

	@XmlAttribute
	protected int delay;

	/**
	 * 延迟后对敌对目标计算并应用伤害。
	 * After delay, calculates and applies damage to an enemy target.
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	@Override
	public void applyEffect(final Effect effect) {
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {

			@Override
			public void run() {
				if (effect.getEffector().isEnemy(effect.getEffected())) {
					calculateAndApplyDamage(effect);
				}
			}
		}, delay);
	}

	/**
	 * 计算魔法技能伤害并立即结算。
	 * Calculates magical skill damage and applies it immediately.
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	private void calculateAndApplyDamage(Effect effect) {
		int skillLvl = effect.getSkillLevel();
		int valueWithDelta = value + delta * skillLvl;
		ActionModifier modifier = getActionModifiers(effect);
		int critAddDmg = this.critAddDmg2 + this.critAddDmg1 * effect.getSkillLevel();
		AttackUtil.calculateMagicalSkillResult(effect, valueWithDelta, modifier, getElement(), true, true, false,
				getMode(), this.critProbMod2, critAddDmg, shared, false);
		effect.getEffected().getController().onAttack(effect.getEffector(), effect.getSkillId(), TYPE.DELAYDAMAGE,
				effect.getReserved1(), true, LOG.PROCATKINSTANT);
		effect.getEffector().getObserveController().notifyAttackObservers(effect.getEffected());
	}
}
