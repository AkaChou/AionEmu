package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.controllers.observer.AttackCalcObserver;
import com.aionemu.gameserver.skillengine.model.Effect;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "AlwaysNoResistEffect")
public class AlwaysNoResistEffect extends EffectTemplate {

	@Override
	public void applyEffect(Effect effect) {
		effect.addToEffectedController();
	}

	@Override
	public void startEffect(Effect effect) {
		AttackCalcObserver observer = new AttackCalcObserver() {
			private int remaining = calculateValue(effect.getSkillLevel());

			@Override
			public boolean hasAlwaysNoResist() {
				return true;
			}

			@Override
			public boolean consumeAlwaysNoResist() {
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
