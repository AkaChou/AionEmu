package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.controllers.observer.AttackCalcObserver;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.skillengine.model.SkillType;

/**
 * 一次性技能攻击加成：在有限次数内提升物理/魔法技能伤害倍率。
 * One-time skill-attack boost: multiplies physical/magic skill damage for limited hits.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "OneTimeBoostSkillAttackEffect")
public class OneTimeBoostSkillAttackEffect extends BuffEffect {
	@XmlAttribute
	private int count;

	@XmlAttribute
	private SkillType type;

	/**
	 * 注册有限次数的技能伤害倍率加成。
	 * Registers limited-count skill damage multipliers.
	 */
	@Override
	public void startEffect(final Effect effect) {
		super.startEffect(effect);
		final int stopCount = count;
		final float percent = 1.0f + value / 100.0f;
		AttackCalcObserver observer = null;
		switch (type) {
		case MAGICAL:
			observer = new AttackCalcObserver() {
				private int count = 0;

				@Override
				public float getBaseMagicalDamageMultiplier() {
					if (count++ < stopCount) {
						return percent;
					} else {
						effect.getEffected().getEffectController().removeEffect(effect.getSkillId());
					}
					return 1.0f;
				}
			};
			break;
		case PHYSICAL:
			observer = new AttackCalcObserver() {
				private int count = 0;

				@Override
				public float getBasePhysicalDamageMultiplier(boolean isSkill) {
					if (!isSkill) {
						return 1f;
					}
					if (count++ < stopCount) {
						if (count == stopCount) {
							effect.getEffected().getEffectController().removeEffect(effect.getSkillId());
						}
						return percent;
					}
					return 1.0f;
				}
			};
			break;
		case ALL:
			observer = new AttackCalcObserver() {
				private int count = 0;

				@Override
				public float getBaseMagicalDamageMultiplier() {
					if (count++ < stopCount) {
						return percent;
					} else {
						effect.getEffected().getEffectController().removeEffect(effect.getSkillId());
					}
					return 1.0f;
				}

				@Override
				public float getBasePhysicalDamageMultiplier(boolean isSkill) {
					if (!isSkill) {
						return 1f;
					}
					if (count++ < stopCount) {
						if (count == stopCount) {
							effect.getEffected().getEffectController().removeEffect(effect.getSkillId());
						}
						return percent;
					}
					return 1.0f;
				}
			};
			break;
		}
		effect.getEffected().getObserveController().addAttackCalcObserver(observer);
		effect.setAttackStatusObserver(observer, position);
	}

	/**
	 * 移除一次性攻击加成。
	 * Removes the one-time attack boost.
	 */
	@Override
	public void endEffect(Effect effect) {
		super.endEffect(effect);
		AttackCalcObserver observer = effect.getAttackStatusObserver(position);
		effect.getEffected().getObserveController().removeAttackCalcObserver(observer);
	}
}
