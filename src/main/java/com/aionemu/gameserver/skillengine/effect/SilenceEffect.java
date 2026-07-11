package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.stats.container.StatEnum;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.skillengine.model.SkillType;

/**
 * 沉默效果：禁止目标施放魔法技能，并打断当前魔法读条。
 * Silence effect: blocks magical casts and cancels an in-progress magical skill.
 *
 * @author ATracer
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SilenceEffect")
public class SilenceEffect extends EffectTemplate {

	/**
	 * 将效果加入目标的效果控制器。
	 * Adds this effect to the target effect controller.
	 */
	@Override
	public void applyEffect(Effect effect) {
		effect.addToEffectedController();
	}

	/**
	 * 按沉默抗性结算是否命中。
	 * Resolves hit chance against silence resistance.
	 */
	@Override
	public void calculate(Effect effect) {
		super.calculate(effect, StatEnum.SILENCE_RESISTANCE, null);
	}

	/**
	 * 施加 SILENCE 异常；若正在施放魔法则打断。
	 * Applies SILENCE abnormal and cancels a magical cast in progress.
	 */
	@Override
	public void startEffect(Effect effect) {
		final Creature effected = effect.getEffected();
		effect.setAbnormal(AbnormalState.SILENCE.getId());
		effected.getEffectController().setAbnormal(AbnormalState.SILENCE.getId());
		if (effected.getCastingSkill() != null
				&& effected.getCastingSkill().getSkillTemplate().getType() == SkillType.MAGICAL) {
			effected.getController().cancelCurrentSkill();
		}
	}

	/**
	 * 清除 SILENCE 异常。
	 * Clears the SILENCE abnormal.
	 */
	@Override
	public void endEffect(Effect effect) {
		effect.getEffected().getEffectController().unsetAbnormal(AbnormalState.SILENCE.getId());
	}
}
