package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.skillengine.model.Effect;

/**
 * 增益睡眠效果：无抗性检定的睡眠，打断当前技能。
 * Buff-sleep effect: sleep without resistance check; cancels current skill.
 *
 * @author kecimis
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "BuffSleepEffect")
public class BuffSleepEffect extends SleepEffect {

	/**
	 * 跳过抗性直接标记效果成功。
	 * Marks the effect successful without a resistance check.
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	@Override
	public void calculate(Effect effect) {
		effect.addSucessEffect(this);
	}

	/**
	 * 打断当前技能并设置睡眠异常。
	 * Cancels the current skill and sets the sleep abnormal.
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	@Override
	public void startEffect(final Effect effect) {
		final Creature effected = effect.getEffected();
		effected.getController().cancelCurrentSkill();
		effect.setAbnormal(AbnormalState.SLEEP.getId());
		effected.getEffectController().setAbnormal(AbnormalState.SLEEP.getId());
	}
}
