package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.controllers.attack.AttackStatus;
import com.aionemu.gameserver.controllers.observer.AttackCalcObserver;
import com.aionemu.gameserver.controllers.observer.AttackStatusObserver;
import com.aionemu.gameserver.skillengine.model.Effect;

/**
 * 必定格挡效果：在限定次数内强制将攻击判定为 BLOCK。
 * Always-block effect: forces BLOCK attack status for a limited number of hits.
 *
 * @author ATracer
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "AlwaysBlockEffect")
public class AlwaysBlockEffect extends EffectTemplate {

	/**
	 * 将效果加入受影响者的效果控制器。
	 * Adds the effect to the effected creature's effect controller.
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	@Override
	public void applyEffect(Effect effect) {
		effect.addToEffectedController();
	}

	/**
	 * 注册攻击判定观察者，命中 BLOCK 时消耗次数并可能结束效果。
	 * Registers an attack-calc observer; BLOCK hits consume charges and may end the effect.
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	@Override
	public void startEffect(final Effect effect) {
		AttackCalcObserver acObserver = new AttackStatusObserver(calculateValue(effect.getSkillLevel()), AttackStatus.BLOCK) {

			@Override
			public boolean checkStatus(AttackStatus status) {
				if (status == AttackStatus.BLOCK) {
					if (consume && value > 0 && --value == 0) {
						effect.endEffect();
					}
					return true;
				} else {
					return false;
				}
			}

		};
		effect.getEffected().getObserveController().addAttackCalcObserver(acObserver);
		effect.setAttackStatusObserver(acObserver, position);
	}

	/**
	 * 移除攻击判定观察者。
	 * Removes the attack-calc observer.
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	@Override
	public void endEffect(Effect effect) {
		AttackCalcObserver acObserver = effect.getAttackStatusObserver(position);
		if (acObserver != null) {
			effect.getEffected().getObserveController().removeAttackCalcObserver(acObserver);
		}
	}
}
