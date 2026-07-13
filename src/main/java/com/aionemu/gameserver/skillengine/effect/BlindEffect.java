package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.controllers.attack.AttackStatus;
import com.aionemu.gameserver.controllers.observer.AttackCalcObserver;
import com.aionemu.gameserver.controllers.observer.AttackStatusObserver;
import com.aionemu.gameserver.model.stats.container.StatEnum;
import com.aionemu.gameserver.skillengine.model.Effect;

/**
 * 失明效果：移除隐身，并按概率使攻击者判定为闪避。
 * Blind effect: removes hide and makes attacker hits miss by chance.
 *
 * @author ATracer
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "BlindEffect")
public class BlindEffect extends EffectTemplate {

	/**
	 * 移除隐身效果并加入效果控制器。
	 * Removes hide effects and adds this effect to the controller.
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	@Override
	public void applyEffect(Effect effect) {
		effect.getEffected().getEffectController().removeHideEffects();
		effect.addToEffectedController();
	}

	/**
	 * 按失明抗性计算是否命中。
	 * Calculates hit using blind resistance.
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	@Override
	public void calculate(Effect effect) {
		super.calculate(effect, StatEnum.BLIND_RESISTANCE, null);
	}

	/**
	 * 设置失明异常并注册攻击判定观察者。
	 * Sets blind abnormal and registers an attack-calc observer.
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	@Override
	public void startEffect(Effect effect) {
		effect.setAbnormal(AbnormalState.BLIND.getId());
		effect.getEffected().getEffectController().setAbnormal(AbnormalState.BLIND.getId());
		int chance = calculateValue(effect.getSkillLevel());
		AttackCalcObserver acObserver = new AttackStatusObserver(chance, AttackStatus.DODGE) {

			@Override
			public boolean checkAttackerStatus(AttackStatus status) {
				return Rnd.get(0, 100) <= chance;
			}
		};
		effect.getEffected().getObserveController().addAttackCalcObserver(acObserver);
		effect.setAttackStatusObserver(acObserver, position);
	}

	/**
	 * 移除观察者并清除失明异常。
	 * Removes the observer and clears the blind abnormal.
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	@Override
	public void endEffect(Effect effect) {
		AttackCalcObserver acObserver = effect.getAttackStatusObserver(position);
		if (acObserver != null) {
			effect.getEffected().getObserveController().removeAttackCalcObserver(acObserver);
		}
		effect.getEffected().getEffectController().unsetAbnormal(AbnormalState.BLIND.getId());
	}
}
