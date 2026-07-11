package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.state.CreatureState;
import com.aionemu.gameserver.model.stats.container.StatEnum;
import com.aionemu.gameserver.skillengine.model.Effect;

/**
 * 睡眠效果：使目标沉睡，受伤害时取消。
 * Sleep effect: puts the target to sleep; cancelled on damage.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SleepEffect")
public class SleepEffect extends EffectTemplate {
	/**
	 * 将效果加入目标的效果控制器。
	 * Adds this effect to the target effect controller.
	 */
	@Override
	public void applyEffect(Effect effect) {
		effect.addToEffectedController();
	}

	/**
	 * 按睡眠抗性结算是否命中。
	 * Resolves hit chance against sleep resistance.
	 */
	@Override
	public void calculate(Effect effect) {
		super.calculate(effect, StatEnum.SLEEP_RESISTANCE, null);
	}

	/**
	 * 中止移动与技能，施加 SLEEP，并设置受伤取消。
	 *
	 * @param effect Aborts move / skill, applies SLEEP, and marks cancel-on-damage.
	 */
	@Override
	public void startEffect(final Effect effect) {
		final Creature effected = effect.getEffected();
		if (effected.isInState(CreatureState.RESTING)) {
			effected.unsetState(CreatureState.RESTING);
		}
		effected.getMoveController().abortMove();
		effected.getController().cancelCurrentSkill();
		effect.setAbnormal(AbnormalState.SLEEP.getId());
		effected.getEffectController().setAbnormal(AbnormalState.SLEEP.getId());
		effect.setCancelOnDmg(true);
	}

	/**
	 * 清除 SLEEP 异常。
	 * Clears the SLEEP abnormal.
	 */
	@Override
	public void endEffect(Effect effect) {
		effect.getEffected().getEffectController().unsetAbnormal(AbnormalState.SLEEP.getId());
	}
}
