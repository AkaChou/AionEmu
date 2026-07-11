package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.skillengine.action.DamageType;
import com.aionemu.gameserver.skillengine.change.Func;
import com.aionemu.gameserver.skillengine.model.Effect;

/**
 * 技能物理攻击瞬发：立即造成物理技能伤害。
 * Instant skill attack: deals physical skill damage immediately.
 *
 * @author ATracer
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SkillAttackInstantEffect")
public class SkillAttackInstantEffect extends DamageEffect {

	@XmlAttribute
	protected int rnddmg;
	@XmlAttribute
	protected boolean cannotmiss;

	/**
	 * 返回随机伤害配置值。
	 * Returns the random-damage configuration value.
	 */
	public int getRnddmg() {
		return rnddmg;
	}

	/**
	 * 返回伤害计算函数模式。
	 * Returns the damage calculation function mode.
	 */
	@Override
	public Func getMode() {
		return mode;
	}

	/**
	 * 按物理伤害类型计算。
	 * Calculates as physical damage.
	 */
	@Override
	public void calculate(Effect effect) {
		super.calculate(effect, DamageType.PHYSICAL);
	}

	/**
	 * 是否必定命中。
	 * Whether the attack cannot miss.
	 */
	public boolean isCannotmiss() {
		return cannotmiss;
	}
}
