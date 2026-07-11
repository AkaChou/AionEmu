package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.controllers.observer.AttackCalcObserver;
import com.aionemu.gameserver.controllers.observer.AttackShieldObserver;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.skillengine.model.Effect;

/**
 * MP 护盾效果：以 MP 吸收伤害的护盾观察者；可按种族条件限制生效。
 * MP shield effect: registers an MP-based damage-absorb shield observer; optional race condition.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "MpShieldEffect")
public class MpShieldEffect extends EffectTemplate {
	@XmlAttribute
	protected int hitdelta;
	@XmlAttribute
	protected int hitvalue;
	@XmlAttribute
	protected boolean percent;
	@XmlAttribute
	protected int radius = 0;
	@XmlAttribute
	protected int minradius = 0;
	@XmlAttribute
	protected Race condrace = null;

	/**
	 * 种族条件满足时将效果加入控制器。
	 * Adds the effect when the race condition (if any) is met.
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	@Override
	public void applyEffect(Effect effect) {
		if (condrace != null && effect.getEffected().getRace() != condrace) {
			return;
		}
		effect.addToEffectedController();
	}

	/**
	 * 标记本效果计算成功。
	 * Marks this effect calculation as successful.
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	@Override
	public void calculate(Effect effect) {
		effect.addSucessEffect(this);
	}

	/**
	 * 注册 MP 护盾观察者并标记受护盾状态。
	 * Registers the MP shield observer and marks under-shield state.
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	@Override
	public void startEffect(final Effect effect) {
		int skillLvl = effect.getSkillLevel();
		int valueWithDelta = value + delta * skillLvl;
		int hitValueWithDelta = hitvalue + hitdelta * skillLvl;
		AttackShieldObserver asObserver = new AttackShieldObserver(hitValueWithDelta, valueWithDelta, percent, effect,
				hitType, getType(), hitTypeProb);
		effect.getEffected().getObserveController().addAttackCalcObserver(asObserver);
		effect.setAttackShieldObserver(asObserver, position);
		effect.getEffected().getEffectController().setUnderShield(true);
	}

	/**
	 * 移除护盾观察者并清除受护盾状态。
	 * Removes the shield observer and clears under-shield state.
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	@Override
	public void endEffect(Effect effect) {
		AttackCalcObserver acObserver = effect.getAttackShieldObserver(position);
		if (acObserver != null) {
			effect.getEffected().getObserveController().removeAttackCalcObserver(acObserver);
		}
		effect.getEffected().getEffectController().setUnderShield(false);
	}

	/**
	 * 返回护盾类型标识（2 = MP 护盾）。
	 * Returns the shield type id (2 = MP shield).
	 */
	public int getType() {
		return 2;
	}
}
