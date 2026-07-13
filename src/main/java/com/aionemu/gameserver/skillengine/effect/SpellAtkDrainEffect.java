package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.controllers.attack.AttackUtil;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ATTACK_STATUS.LOG;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ATTACK_STATUS.TYPE;
import com.aionemu.gameserver.skillengine.model.Effect;

/**
 * 法术攻击吸血/吸魔持续效果：周期性魔法伤害并按比例回复施法者。
 * Over-time spell attack with drain: periodic magical damage that heals the effector by percent.
 *
 * @author Sippolo
@author kecimis
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SpellAtkDrainEffect")
public class SpellAtkDrainEffect extends AbstractOverTimeEffect {

	@XmlAttribute(name = "hp_percent")
	protected int hp_percent;
	@XmlAttribute(name = "mp_percent")
	protected int mp_percent;

	/**
	 * 计算周期魔法伤害，造成攻击，并按百分比回复 HP/MP。
	 * Calculates periodic magical damage, applies the hit, and restores HP/MP by percent.
	 */
	@Override
	public void onPeriodicAction(Effect effect) {
		int valueWithDelta = value + delta * effect.getSkillLevel();
		int critAddDmg = critAddDmg2 + critAddDmg1 * effect.getSkillLevel();
		int damage = AttackUtil.calculateMagicalOverTimeSkillResult(effect, valueWithDelta, element, position,
				effect.getSkillTemplate().isMboostApplied(), mrResist, critProbMod2, critAddDmg);
		effect.getEffected().getController().onAttack(effect.getEffector(), effect.getSkillId(), TYPE.REGULAR, damage,
				true, LOG.SPELLATKDRAIN);
		if (effect.tryActivateGodstone()) {
			effect.getEffector().getObserveController().notifyAttackObservers(effect.getEffected(), effect.getSkillId());
		}

		// 吸取（治疗）造成伤害的一部分 / Drain (heal) portion of damage inflicted
		if (hp_percent != 0) {
			effect.getEffector().getLifeStats().increaseHp(TYPE.HP, damage * hp_percent / 100, effect.getSkillId(),
					LOG.SPELLATKDRAIN);
		}
		if (mp_percent != 0) {
			effect.getEffector().getLifeStats().increaseMp(TYPE.MP, damage * mp_percent / 100, effect.getSkillId(),
					LOG.SPELLATKDRAIN);
		}
	}
}
