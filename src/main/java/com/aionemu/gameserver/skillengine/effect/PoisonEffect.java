package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.controllers.attack.AttackUtil;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.stats.container.StatEnum;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ATTACK_STATUS.LOG;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ATTACK_STATUS.TYPE;
import com.aionemu.gameserver.skillengine.model.Effect;

/**
 * 中毒效果：按周期造成魔法持续伤害，并写入 POISON 异常。
 * Poison effect: deals periodic magical DoT and sets the POISON abnormal state.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "PoisonEffect")
public class PoisonEffect extends AbstractOverTimeEffect {
	/**
	 * 按中毒抗性计算是否生效。
	 * Calculates success against poison resistance.
	 */
	@Override
	public void calculate(Effect effect) {
		super.calculate(effect, StatEnum.POISON_RESISTANCE, null);
	}

	/**
	 * 预结算持续伤害并设置 POISON 异常。
	 * Precomputes DoT and sets the POISON abnormal state.
	 */
	@Override
	public void startEffect(Effect effect) {
		int valueWithDelta = value + delta * effect.getSkillLevel();
		int critAddDmg = this.critAddDmg2 + this.critAddDmg1 * effect.getSkillLevel();
		int finalDamage = AttackUtil.calculateMagicalOverTimeSkillResult(effect, valueWithDelta, element, this.position, false, this.critProbMod2, critAddDmg);
		effect.setReservedInt(position, finalDamage);
		super.startEffect(effect, AbnormalState.POISON);
	}

	/**
	 * 清除 POISON 异常状态。
	 * Clears the POISON abnormal state.
	 */
	@Override
	public void endEffect(Effect effect) {
		// super.endEffect(effect, AbnormalState.POISON);
		effect.getEffected().getEffectController().unsetAbnormal(AbnormalState.POISON.getId());
	}

	/**
	 * 周期造成中毒伤害并通知 DoT 观察者。
	 * Deals poison damage and notifies DoT observers.
	 */
	@Override
	public void onPeriodicAction(Effect effect) {
		Creature effected = effect.getEffected();
		Creature effector = effect.getEffector();
		effected.getController().onAttack(effector, effect.getSkillId(), TYPE.DAMAGE, effect.getReservedInt(position), false, LOG.POISON);
		effected.getObserveController().notifyDotAttackedObservers(effector, effect);
	}
}
