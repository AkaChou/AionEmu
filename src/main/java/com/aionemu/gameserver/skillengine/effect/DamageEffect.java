package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.controllers.attack.AttackUtil;
import com.aionemu.gameserver.skillengine.action.DamageType;
import com.aionemu.gameserver.skillengine.change.Func;
import com.aionemu.gameserver.skillengine.effect.modifier.ActionModifier;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.skillengine.model.SkillType;

/**
 * 伤害效果基类：按物理/魔法类型结算并应用伤害。
 * Damage effect base: resolves and applies damage for physical/magical types.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "DamageEffect")
public abstract class DamageEffect extends EffectTemplate {
	@XmlAttribute
	protected Func mode = Func.ADD;
	@XmlAttribute
	protected boolean shared;
	@XmlAttribute(name = "flat_value")
	protected int flatValue;
	@XmlAttribute(name = "flat_delta")
	protected int flatDelta;
	@XmlAttribute(name = "percent_value")
	protected int percentValue;
	@XmlAttribute(name = "percent_delta")
	protected int percentDelta;

	/**
	 * 对目标结算一次攻击伤害并通知攻击观察者。
	 * Applies one attack hit to the target and notifies attack observers.
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	@Override
	public void applyEffect(Effect effect) {
		effect.getEffected().getController().onAttack(effect.getEffector(), effect.getSkillId(), effect.getReserved1(),
				true);
		if (effect.tryActivateGodstone()) {
			effect.getEffector().getObserveController().notifyAttackObservers(effect.getEffected(), effect.getSkillId());
		}
	}

	/**
	 * 按伤害类型计算技能伤害结果。
	 * Calculates skill damage result by damage type.
	 *
	 * @param effect 运行时效果 / runtime effect
	 * damage type
	 *
	 * @return 是否命中成功 / whether the hit succeeded
	 */
	public boolean calculate(Effect effect, DamageType damageType) {
		if (!super.calculate(effect, null, null)) {
			return false;
		}
		int skillLvl = effect.getSkillLevel();
		int valueWithDelta = value + delta * skillLvl;
		int flatDamage = calculateFlatDamage(skillLvl);
		int percentDamage = calculatePercentDamage(skillLvl);
		if (this instanceof ProcAtkInstantEffect proc && proc.getWeaponBoost() > 0) {
			valueWithDelta = Math.round(valueWithDelta * proc.getWeaponBoost() / 100f);
		}
		ActionModifier modifier = getActionModifiers(effect);
		SkillType accuracyType = damageType == DamageType.MAGICAL ? SkillType.MAGICAL : SkillType.PHYSICAL;
		int accMod = this.accMod2 + this.accMod1 * skillLvl
				+ effect.getEffector().getObserveController().getSkillAccuracyModifier(accuracyType);
		int critAddDmg = this.critAddDmg2 + this.critAddDmg1 * skillLvl;
		Object effector = effect.getEffector().getController().getOwner();
		switch (damageType) {
		case PHYSICAL:
			boolean cannotMiss = false;
			if (this instanceof SkillAttackInstantEffect) {
				cannotMiss = ((SkillAttackInstantEffect) this).isCannotmiss();
			}
			int rndDmg = (this instanceof SkillAttackInstantEffect ? ((SkillAttackInstantEffect) this).getRnddmg() : 0);
			AttackUtil.calculateSkillResult(effect, valueWithDelta, modifier, this.getMode(), flatDamage, percentDamage,
					rndDmg, accMod,
					this.critProbMod2, critAddDmg, cannotMiss, shared, false, false);
			break;
		case MAGICAL:
			boolean useKnowledge = true;
			if (this instanceof ProcAtkInstantEffect) {
				useKnowledge = false;
			}
			AttackUtil.calculateMagicalSkillResult(effect, valueWithDelta, modifier, getElement(),
					isMagicBoostApplied(effect), useKnowledge, false, this.getMode(), flatDamage, percentDamage,
					this.critProbMod2, critAddDmg, shared, false);
			break;
		default:
			AttackUtil.calculateSkillResult(effect, 0, null, this.getMode(), 0, accMod, 100, 0, false, shared, false,
					false);
		}
		return true;
	}

	int calculateFlatDamage(int skillLevel) {
		return flatValue + flatDelta * skillLevel;
	}

	int calculatePercentDamage(int skillLevel) {
		return percentValue + percentDelta * skillLevel;
	}

	protected boolean isMagicBoostApplied(Effect effect) {
		return effect.getSkillTemplate().isMboostApplied();
	}

	/**
	 * 返回伤害结算模式（加值/百分比等）。
	 * Returns the damage resolution mode (add/percent/etc.).
	 *
	 * resolution mode
	 */
	public Func getMode() {
		return mode;
	}
}
