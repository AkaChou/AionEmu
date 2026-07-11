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
	@XmlAttribute(name = "mp_percent")
	protected int mp_percent;

	/**
	 * 先结算伤害，再按配置百分比回复 HP/MP。
	 * Applies damage first, then restores HP/MP from reserved damage by percent.
	 */
	@Override
	public void applyEffect(Effect effect) {
		super.applyEffect(effect);
		if (hp_percent != 0) {
			effect.getEffector().getLifeStats().increaseHp(TYPE.HP, effect.getReserved1() * hp_percent / 100,
					effect.getSkillId(), LOG.SPELLATKDRAININSTANT);
		}
		if (mp_percent != 0) {
			effect.getEffector().getLifeStats().increaseMp(TYPE.ABSORBED_MP, effect.getReserved1() * mp_percent / 100,
					effect.getSkillId(), LOG.SPELLATKDRAININSTANT);
		}
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
