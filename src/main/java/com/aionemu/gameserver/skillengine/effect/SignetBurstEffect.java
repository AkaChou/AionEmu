package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.controllers.attack.AttackUtil;
import com.aionemu.gameserver.skillengine.model.Effect;

/**
 * 印记爆发效果：消耗目标身上的印记，按层数放大魔法伤害。
 * Signet-burst effect: consumes the target signet and scales magical damage by its level.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SignetBurstEffect")
public class SignetBurstEffect extends DamageEffect {
	private static final int[][] BURST_DAMAGE_PERCENT = {
			{ 10, 20, 50, 100, 120, 150 },
			{ 12, 30, 50, 70, 90, 110 },
			{ 12, 50, 90, 110, 130, 150 },
			{ 12, 50, 90, 110, 130, 150 },
			{ 0, 50, 75, 100 },
			{ 0, 100, 100 }
	};

	@XmlAttribute
	protected int signetlvl;

	@XmlAttribute(name = "signet_type")
	protected int signetType;
	@XmlAttribute
	protected String signet;

	@XmlAttribute(name = "min_signet_level")
	protected int minSignetLevel;

	/**
	 * 查找印记效果，按层数调整伤害与命中修正，结算后结束印记。
	 *
	 * @param effect Finds the signet, scales damage / accuracy by level, calculates magical result, then ends the signet.
	 */
	@Override
	public void calculate(Effect effect) {
		Effect signetEffect = signetType == 0 ? effect.getEffected().getEffectController().getAnormalEffect(signet)
				: effect.getEffected().getEffectController().getAbnormalEffects().stream()
						.filter(active -> active.getEffectTemplates().stream()
								.anyMatch(template -> template instanceof SignetEffect signetTemplate
										&& signetTemplate.getSignetType() == signetType))
						.findFirst().orElse(null);
		if (!super.calculate(effect, null, null)) {
			if (signetEffect != null) {
				signetEffect.endEffect();
			}
			return;
		}
		int valueWithDelta = value + delta * effect.getSkillLevel();
		int critAddDmg = getCriticalAdditionalDamage(effect.getSkillLevel());
		int level = 0;
		if (signetEffect != null) {
			level = signetEffect.getEffectTemplates().stream()
					.filter(SignetEffect.class::isInstance).map(SignetEffect.class::cast)
					.filter(signetEffectTemplate -> signetType == 0 || signetEffectTemplate.getSignetType() == signetType)
					.mapToInt(SignetEffect::getSignetLevel).findFirst().orElse(signetEffect.getSkillLevel());
			if (level == 0) {
				level = signetEffect.getSkillLevel();
			}
		}
		int burstLevel = Math.min(level, signetlvl);
		if (minSignetLevel == 0 || level >= minSignetLevel) {
			valueWithDelta = scaleBurstDamage(valueWithDelta, getBurstDamagePercent(signetType, burstLevel));
		} else {
			effect.setSubEffectAborted(true);
		}
		effect.setSignetBurstedCount(burstLevel);
		effect.setLaunchSubEffect(signetEffect != null);
		AttackUtil.calculateMagicalSkillResult(effect, valueWithDelta, getActionModifiers(effect), getElement(), true, true, false,
				getMode(), getCriticalProbability(effect.getSkillLevel()), critAddDmg, shared, false);
		if (signetEffect != null) {
			signetEffect.endEffect();
		}
	}

	static int getBurstDamagePercent(int signetType, int level) {
		int typeIndex = signetType >= 1 && signetType <= BURST_DAMAGE_PERCENT.length ? signetType - 1 : 0;
		int[] percentages = BURST_DAMAGE_PERCENT[typeIndex];
		return percentages[Math.min(Math.max(0, level), percentages.length - 1)];
	}

	static int scaleBurstDamage(int damage, int percent) {
		return damage / 100 * percent;
	}
}
