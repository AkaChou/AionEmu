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
 * 流血 DoT 效果：周期造成伤害并标记 BLEED 异常。
 * Bleed DoT effect: deals periodic damage and marks the BLEED abnormal.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "BleedEffect")
public class BleedEffect extends AbstractOverTimeEffect {
	/**
	 * 按流血抗性计算是否命中。
	 * Calculates hit using bleed resistance.
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	@Override
	public void calculate(Effect effect) {
		super.calculate(effect, StatEnum.BLEED_RESISTANCE, null);
	}

	/**
	 * 预计算周期伤害并启动流血状态。
	 * Precomputes periodic damage and starts the bleed state.
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	@Override
	public void startEffect(final Effect effect) {
		int valueWithDelta = value + delta * effect.getSkillLevel();
		int critAddDmg = this.critAddDmg2 + this.critAddDmg1 * effect.getSkillLevel();
		int finalDamage = AttackUtil.calculateMagicalOverTimeSkillResult(effect, valueWithDelta, element, this.position,
				false, true, this.critProbMod2, critAddDmg);
		effect.setReservedInt(position, finalDamage);
		super.startEffect(effect, AbnormalState.BLEED);
	}

	/**
	 * 清除流血异常。
	 * Clears the bleed abnormal.
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	@Override
	public void endEffect(Effect effect) {
		// super.endEffect(effect, AbnormalState.BLEED);
		effect.getEffected().getEffectController().unsetAbnormal(AbnormalState.BLEED.getId());
	}

	/**
	 * 周期结算流血伤害。
	 * Applies one tick of bleed damage.
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	@Override
	public void onPeriodicAction(Effect effect) {
		Creature effected = effect.getEffected();
		Creature effector = effect.getEffector();
		effected.getController().onAttack(effector, effect.getSkillId(), TYPE.DAMAGE, effect.getReservedInt(position), false, LOG.BLEED);
		effected.getObserveController().notifyDotAttackedObservers(effector, effect);
	}
}
