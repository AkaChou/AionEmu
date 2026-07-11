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
 * 护盾效果：注册攻击护盾观察者，按层数/数值吸收伤害。
 * Shield effect: registers an attack-shield observer that absorbs damage by hit value.
 *
 * @author ATracer modified by Wakizashi, Sippolo, kecimis
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ShieldEffect")
public class ShieldEffect extends EffectTemplate {

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
	 */
	@Override
	public void applyEffect(Effect effect) {
		// 检查条件种族，skillId: 10317,10318 / check for condition race, skillId: 10317,10318
		if (condrace != null && effect.getEffected().getRace() != condrace) {
			return;
		}
		effect.addToEffectedController();
	}

	/**
	 * 直接标记本效果成功。
	 * Always marks this effect successful.
	 */
	@Override
	public void calculate(Effect effect) {
		effect.addSucessEffect(this);
	}

	/**
	 * 按技能等级计算护盾值，注册 AttackShieldObserver 并标记处于护盾中。
	 * Computes shield values, attaches AttackShieldObserver, and marks under-shield.
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
	 * 移除护盾观察者并取消处于护盾标记。
	 * Removes the shield observer and clears under-shield.
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
	 * 返回护盾类型：2 表示普通护盾（1 反射 / 8 保护）。
	 * Returns shield type 2 (normal shield; 1=reflector, 8=protect).
	 */
	public int getType() {
		return 2;
	}
}
