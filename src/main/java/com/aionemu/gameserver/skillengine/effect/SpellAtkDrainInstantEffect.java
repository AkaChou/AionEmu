package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.network.aion.serverpackets.SM_ATTACK_STATUS.LOG;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ATTACK_STATUS.TYPE;
import com.aionemu.gameserver.skillengine.action.DamageType;
import com.aionemu.gameserver.skillengine.model.Effect;

/**
 * 法术攻击吸血/吸魔瞬发：造成魔法伤害并按比例回复施法者 HP/MP。
 * Instant spell attack with drain: deals magical damage and restores effector HP/MP by percent.
 *
 * @author ATracer
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SpellAtkDrainInstantEffect")
public class SpellAtkDrainInstantEffect extends DamageEffect {

	@XmlAttribute(name = "hp_percent")
	protected int hp_percent;
	@XmlAttribute(name = "hp_percent_delta")
	protected int hpPercentDelta;
	@XmlAttribute(name = "mp_percent")
	protected int mp_percent;
	@XmlAttribute(name = "mp_percent_delta")
	protected int mpPercentDelta;
	@XmlAttribute(name = "applymboost")
	protected Boolean applyMBoost;

	/**
	 * 先结算伤害，再按配置百分比回复 HP/MP。
	 * Applies damage first, then restores HP/MP from reserved damage by percent.
	 */
	@Override
	public void applyEffect(Effect effect) {
		super.applyEffect(effect);
		int hpPercent = calculateHpPercent(effect.getSkillLevel());
		int mpPercent = calculateMpPercent(effect.getSkillLevel());
		if (hpPercent != 0) {
			effect.getEffector().getLifeStats().increaseHp(TYPE.HP, effect.getReserved1() * hpPercent / 100,
					effect.getSkillId(), LOG.SPELLATKDRAININSTANT);
		}
		if (mpPercent != 0) {
			effect.getEffector().getLifeStats().increaseMp(TYPE.ABSORBED_MP, effect.getReserved1() * mpPercent / 100,
					effect.getSkillId(), LOG.SPELLATKDRAININSTANT);
		}
	}

	int calculateHpPercent(int skillLevel) {
		return hp_percent + hpPercentDelta * skillLevel;
	}

	int calculateMpPercent(int skillLevel) {
		return mp_percent + mpPercentDelta * skillLevel;
	}

	@Override
	protected boolean isMagicBoostApplied(Effect effect) {
		return applyMBoost == null ? super.isMagicBoostApplied(effect) : applyMBoost;
	}

	/**
	 * 按魔法伤害类型计算。
	 * Calculates as magical damage.
	 */
	@Override
	public void calculate(Effect effect) {
		super.calculate(effect, DamageType.MAGICAL);
	}
}
