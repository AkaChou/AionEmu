package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.controllers.attack.AttackUtil;
import com.aionemu.gameserver.skillengine.model.Effect;

/**
 * 不可减免的瞬时法术攻击：按配置直接结算魔法伤害（可百分比）。
 * Non-reducible instant spell attack: settles magic damage as configured (optional percent).
 *
 * @author Sippolo
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "NoReduceSpellATKInstantEffect")
public class NoReduceSpellATKInstantEffect extends DamageEffect {

	@XmlAttribute
	protected boolean percent;

	/**
	 * 计算不可减免的瞬时法术伤害。
	 * Calculates non-reducible instant spell damage.
	 */
	@Override
	public void calculate(Effect effect) {
		if (!super.calculate(effect, null, null)) {
			return;
		}

		int valueWithDelta = value + delta * effect.getSkillLevel();
		if (percent) {
			valueWithDelta = (int) (valueWithDelta / 100f * effect.getEffected().getLifeStats().getMaxHp());
		}
		int critAddDmg = this.critAddDmg2 + this.critAddDmg1 * effect.getSkillLevel();

		AttackUtil.calculateMagicalSkillResult(effect, valueWithDelta, null, getElement(), false, true, true, getMode(),
				this.critProbMod2, critAddDmg, shared, false);
	}
}
