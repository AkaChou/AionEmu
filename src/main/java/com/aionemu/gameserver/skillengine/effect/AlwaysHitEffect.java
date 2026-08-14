package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.controllers.observer.AttackCalcObserver;
import com.aionemu.gameserver.skillengine.model.Effect;

/**
 * 必中效果：使目标攻击必定命中，可按次数消耗。
 * Always-hit effect: ensures the target's attacks always hit, optionally for a limited number of uses.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "AlwaysHitEffect")
public class AlwaysHitEffect extends EffectTemplate {

	@Override
	public void applyEffect(Effect effect) {
		effect.addToEffectedController();
	}

	/**
	 * 注册攻击计算观察者，强制攻击命中并统计剩余次数。
	 * Registers an attack calc observer that forces hits and tracks remaining uses.
	 */
	@Override
	public void startEffect(Effect effect) {
		AttackCalcObserver observer = new AttackCalcObserver() {
			private int remaining = calculateValue(effect.getSkillLevel());

			@Override
			public boolean hasAlwaysHit() {
				return true;
			}

			@Override
			public boolean consumeAlwaysHit() {
				if (consume && remaining > 0 && --remaining == 0) {
					effect.endEffect();
				}
				return true;
			}
		};
		effect.getEffected().getObserveController().addAttackCalcObserver(observer);
		effect.setAttackStatusObserver(observer, position);
	}

	@Override
	public void endEffect(Effect effect) {
		AttackCalcObserver observer = effect.getAttackStatusObserver(position);
		if (observer != null) {
			effect.getEffected().getObserveController().removeAttackCalcObserver(observer);
		}
	}
}
