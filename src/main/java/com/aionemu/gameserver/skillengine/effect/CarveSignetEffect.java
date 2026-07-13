package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.skillengine.action.DamageType;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.skillengine.model.SkillTemplate;

/**
 * 雕刻印记效果：造成物理伤害并叠加/刷新目标印记层数。
 * Carve-signet effect: deals physical damage and stacks/refreshes signet levels on the target.
 *
 * @author ATracer
 * @Rework MATTY (ADev.Team)
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "CarveSignetEffect")
public class CarveSignetEffect extends DamageEffect {
	@XmlAttribute(required = true)
	protected int signetlvlstart;

	@XmlAttribute(required = true)
	protected int signetlvl;

	@XmlAttribute(required = true)
	protected int signetid;

	@XmlAttribute(name = "signet_type")
	protected int signetType;
	@XmlAttribute
	protected String signet;

	@XmlAttribute
	protected int prob = 100;
	@XmlAttribute(name = "prob_delta")
	protected int probDelta;

	/**
	 * 应用伤害，并按概率叠加下一级印记技能。
	 * Applies damage and, by chance, stacks the next signet skill level.
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	@Override
	public void applyEffect(Effect effect) {
		super.applyEffect(effect);
		if (!Rnd.chance(prob + probDelta * effect.getSkillLevel())) {
			return;
		}
		Effect placedSignet = signetType == 0 ? effect.getEffected().getEffectController().getAnormalEffect(signet)
				: effect.getEffected().getEffectController().getAbnormalEffects().stream()
						.filter(active -> active.getEffectTemplates().stream()
								.anyMatch(template -> template instanceof SignetEffect signetEffect
										&& signetEffect.getSignetType() == signetType))
						.findFirst().orElse(null);
		int nextSignetLevel = Math.max(1, signetlvlstart);
		if (placedSignet != null) {
			for (EffectTemplate template : placedSignet.getEffectTemplates()) {
				if (template instanceof SignetEffect signetEffect
						&& (signetType == 0 || signetEffect.getSignetType() == signetType)) {
					int currentLevel = signetEffect.getSignetLevel() > 0
							? signetEffect.getSignetLevel() : placedSignet.getSkillLevel();
					nextSignetLevel = Math.max(nextSignetLevel, Math.min(signetlvl, currentLevel + 1));
					break;
				}
			}
			placedSignet.endEffect();
		}
		effect.setCarvedSignet(nextSignetLevel);
		SkillTemplate template = DataManager.SKILL_DATA.getSkillTemplate(nextSignetSkillId(signetid, signetlvl, nextSignetLevel));
		Effect newEffect = new Effect(effect.getEffector(), effect.getEffected(), template, effect.getCarvedSignet(),
				0);
		newEffect.initialize();
		newEffect.applyEffect();
	}

	static int nextSignetSkillId(int maxSignetSkillId, int maxSignetLevel, int nextSignetLevel) {
		return maxSignetSkillId - maxSignetLevel + nextSignetLevel;
	}

	/**
	 * 计算物理伤害是否命中。
	 * Calculates whether the physical damage hits.
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	@Override
	public void calculate(Effect effect) {
		if (!super.calculate(effect, DamageType.PHYSICAL)) {
			return;
		}
	}
}
