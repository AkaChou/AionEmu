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

	@XmlAttribute(required = true)
	protected String signet;

	@XmlAttribute(required = true)
	protected final float prob = 100;

	private int nextSignetLevel = 1;

	/**
	 * 应用伤害，并按概率叠加下一级印记技能。
	 * Applies damage and, by chance, stacks the next signet skill level.
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	@Override
	public void applyEffect(Effect effect) {
		super.applyEffect(effect);
		if (Rnd.get(0, 100) > prob) {
			return;
		}
		if (signetlvl == 0) {
			signetlvl = 1;
		}
		Effect placedSignet = effect.getEffected().getEffectController().getAnormalEffect(signet);
		if (placedSignet != null) {
			placedSignet.endEffect();
		}
		nextSignetLevel = 1;
		if (placedSignet != null) {
			nextSignetLevel = placedSignet.getSkillId() - 8302 + 1;
			if (nextSignetLevel > signetlvl || nextSignetLevel > 5) {
				nextSignetLevel--;
			}
		}
		if (nextSignetLevel < signetlvlstart) {
			nextSignetLevel = signetlvlstart + 0;
		}
		effect.setCarvedSignet(nextSignetLevel);
		SkillTemplate template = DataManager.SKILL_DATA.getSkillTemplate(8302 + nextSignetLevel);
		Effect newEffect = new Effect(effect.getEffector(), effect.getEffected(), template, effect.getCarvedSignet(),
				0);
		newEffect.initialize();
		newEffect.applyEffect();
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
