package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAttribute;

import com.aionemu.gameserver.controllers.observer.AttackShieldObserver;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.skillengine.model.HealType;

/**
 * 伤害转治疗护盾：吸收伤害并按配置类型转化为治疗。
 * Convert-heal shield: absorbs damage and converts it into heal of the configured type.
 */
public class ConvertHealEffect extends ShieldEffect {
	@XmlAttribute
	protected HealType type;
	@XmlAttribute(name = "hitpercent")
	protected boolean hitPercent;

	/**
	 * 注册攻击护盾观察者，将伤害转化为治疗。
	 * Registers an attack-shield observer that converts damage into heal.
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	@Override
	public void startEffect(final Effect effect) {
		int skillLvl = effect.getSkillLevel();
		int valueWithDelta = value + delta * skillLvl;
		int hitValueWithDelta = hitvalue + hitdelta * skillLvl;
		AttackShieldObserver asObserver = new AttackShieldObserver(hitValueWithDelta, valueWithDelta, this.percent,
				this.hitPercent, effect, this.hitType, getType(), this.hitTypeProb, 0, 0, this.type, 0, 0);
		effect.getEffected().getObserveController().addAttackCalcObserver(asObserver);
		effect.setAttackShieldObserver(asObserver, position);
		effect.getEffected().getEffectController().setUnderShield(true);
	}

	/**
	 * 返回护盾类型标识。
	 * Returns the shield type id.
	 *
	 * type id
	 */
	public int getType() {
		return 0;
	}
}
