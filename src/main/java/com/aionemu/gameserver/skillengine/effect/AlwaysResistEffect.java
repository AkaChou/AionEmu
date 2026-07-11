package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.controllers.attack.AttackStatus;
import com.aionemu.gameserver.controllers.observer.AttackCalcObserver;
import com.aionemu.gameserver.controllers.observer.AttackStatusObserver;
import com.aionemu.gameserver.skillengine.model.Effect;

/**
 * 必定抵抗效果：在限定次数内强制将攻击判定为 RESIST。
 * Always-resist effect: forces RESIST attack status for a limited number of hits.
 *
 * @author ATracer
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "AlwaysResistEffect")
public class AlwaysResistEffect extends EffectTemplate {

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
	 * 注册攻击判定观察者，命中 RESIST 时消耗次数并可能结束效果。
	 * Registers an attack-calc observer; RESIST hits consume charges and may end the effect.
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	@Override
	public void startEffect(final Effect effect) {
		AttackCalcObserver acObserver = new AttackStatusObserver(value, AttackStatus.RESIST) {

			@Override
			public boolean checkStatus(AttackStatus status) {
				if (status == AttackStatus.RESIST) {
					if (value <= 1) {
						effect.endEffect();
					} else {
						value--;
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
