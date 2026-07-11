package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.stats.container.StatEnum;
import com.aionemu.gameserver.skillengine.model.Effect;

/**
 * 麻痹效果：使目标无法行动，取消当前技能并停止移动。
 * Paralyze effect: immobilizes the target, cancels current skill and movement.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ParalyzeEffect")
public class ParalyzeEffect extends EffectTemplate {
	/**
	 * 将效果应用到目标（加入控制器或立即结算）。
	 * Applies the effect to the target (controller attach or immediate settlement).
	 */
	@Override
	public void applyEffect(Effect effect) {
		effect.addToEffectedController();
	}

	/**
	 * 按麻痹抗性计算是否生效。
	 * Calculates success against paralyze resistance.
	 */
	@Override
	public void calculate(Effect effect) {
		super.calculate(effect, StatEnum.PARALYZE_RESISTANCE, null);
	}

	/**
	 * 取消技能与移动并设置 PARALYZE 异常。
	 *
	 * @param effect Cancels skill / move and sets the PARALYZE abnormal state.
	 */
	@Override
	public void startEffect(Effect effect) {
		final Creature effected = effect.getEffected();
		effected.getController().cancelCurrentSkill();
		effected.getMoveController().abortMove();
		effect.setAbnormal(AbnormalState.PARALYZE.getId());
		effect.getEffected().getEffectController().setAbnormal(AbnormalState.PARALYZE.getId());
	}

	/**
	 * 清除 PARALYZE 异常状态。
	 * Clears the PARALYZE abnormal state.
	 */
	@Override
	public void endEffect(Effect effect) {
		effect.getEffected().getEffectController().unsetAbnormal(AbnormalState.PARALYZE.getId());
	}
}
