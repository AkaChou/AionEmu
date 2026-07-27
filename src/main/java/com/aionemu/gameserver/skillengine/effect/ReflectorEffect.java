package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.controllers.observer.AttackCalcObserver;
import com.aionemu.gameserver.controllers.observer.AttackShieldObserver;
import com.aionemu.gameserver.skillengine.model.Effect;

/**
 * 反射效果：注册反射型攻击护盾观察者，将部分伤害反弹给攻击者。
 * Reflector effect: registers a reflective attack-shield observer that returns damage.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ReflectorEffect")
public class ReflectorEffect extends ShieldEffect {

	/**
	 * 按 hitvalue/hitdelta 注册反射护盾观察者。
	 * Registers a reflective shield observer from hitvalue/hitdelta.
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	@Override
	public void startEffect(final Effect effect) {
		int hit = hitvalue + hitdelta * effect.getSkillLevel();
		int reflectPercent = value + delta * effect.getSkillLevel();
		AttackShieldObserver asObserver = new AttackShieldObserver(hit, reflectPercent, percent, false, effect, hitType,
				this.getType(), this.hitTypeProb, minradius, radius, null, 0, 0);
		effect.getEffected().getObserveController().addAttackCalcObserver(asObserver);
		effect.setAttackShieldObserver(asObserver, position);
	}

	/**
	 * 移除反射护盾观察者。
	 * Removes the reflective shield observer.
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	@Override
	public void endEffect(Effect effect) {
		AttackCalcObserver acObserver = effect.getAttackShieldObserver(position);
		if (acObserver != null) {
			effect.getEffected().getObserveController().removeAttackCalcObserver(acObserver);
		}
	}

	/**
	 * 返回护盾类型标识（1 = 反射）。
	 * Returns the shield type id (1 = reflector).
	 */
	@Override
	public int getType() {
		return 1;
	}
}
