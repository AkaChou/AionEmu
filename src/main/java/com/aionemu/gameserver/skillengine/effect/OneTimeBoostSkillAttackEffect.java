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
	@XmlAttribute(name = "count_delta")
	private int countDelta;
	@XmlAttribute(name = "damage_flat_delta")
	private int damageFlatDelta;
	@XmlAttribute(name = "damage_flat_value")
	private int damageFlatValue;
	@XmlAttribute(name = "accuracy_delta")
	private int accuracyDelta;
	@XmlAttribute(name = "accuracy_value")
	private int accuracyValue;
	@XmlAttribute(name = "accuracy_flat_delta")
	private int accuracyFlatDelta;
	@XmlAttribute(name = "accuracy_flat_value")
	private int accuracyFlatValue;

	@XmlAttribute
	private SkillType type;

	/**
	 * 注册有限次数的技能伤害倍率加成。
	 * Registers limited-count skill damage multipliers.
	 */
	@Override
	public void startEffect(final Effect effect) {
		super.startEffect(effect);
		final int skillLevel = effect.getSkillLevel();
		AttackCalcObserver observer = new AttackCalcObserver() {
			private int remaining = calculateCount(skillLevel);

			private boolean supports(SkillType skillType) {
				return type == SkillType.ALL || type == skillType;
			}

			private float consume(SkillType skillType) {
				if (!supports(skillType) || remaining <= 0) {
					return 1f;
				}
				if (--remaining == 0) {
					effect.getEffected().getEffectController().removeEffect(effect.getSkillId());
				}
				return 1f + calculateValue(skillLevel) / 100f;
			}

			@Override
			public float getBasePhysicalDamageMultiplier(boolean isSkill) {
				return isSkill ? consume(SkillType.PHYSICAL) : 1f;
			}

			@Override
			public float getBaseMagicalDamageMultiplier() {
				return consume(SkillType.MAGICAL);
			}

			@Override
			public int getPhysicalSkillDamageBonus() {
				return supports(SkillType.PHYSICAL) ? calculateFlatDamage(skillLevel) : 0;
			}

			@Override
			public int getMagicalSkillDamageBonus() {
				return supports(SkillType.MAGICAL) ? calculateFlatDamage(skillLevel) : 0;
			}

			@Override
			public int getSkillAccuracyModifier(SkillType skillType) {
				if (!supports(skillType)) {
					return 0;
				}
				int base = skillType == SkillType.MAGICAL
						? effect.getEffected().getGameStats().getMAccuracy().getCurrent()
						: effect.getEffected().getGameStats().getMainHandPAccuracy().getCurrent();
				return calculateAccuracyModifier(base, skillLevel);
			}
		};
		effect.getEffected().getObserveController().addAttackCalcObserver(observer);
		effect.setAttackStatusObserver(observer, position);
	}

	int calculateCount(int skillLevel) {
		return Math.max(0, count + countDelta * skillLevel);
	}

	int calculateFlatDamage(int skillLevel) {
		return damageFlatValue + damageFlatDelta * skillLevel;
	}

	int calculateAccuracyModifier(int baseAccuracy, int skillLevel) {
		int percent = accuracyValue + accuracyDelta * skillLevel;
		int flat = accuracyFlatValue + accuracyFlatDelta * skillLevel;
		return Math.round(baseAccuracy * percent / 100f) + flat;
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
