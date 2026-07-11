package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.stats.container.StatEnum;
import com.aionemu.gameserver.skillengine.model.Effect;

/**
 * 减速效果：对目标施加 SLOW 异常，并按减速抗性结算。
 * Slow effect: applies SLOW abnormal and resolves against slow resistance.
 *
 * @author ATracer
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SlowEffect")
public class SlowEffect extends BuffEffect {

	/**
	 * 将效果加入受影响者的效果控制器。
	 * Adds the effect to the effected creature's effect controller.
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	@Override
	public void applyEffect(Effect effect) {
		effect.addToEffectedController();
	}

	/**
	 * 按 SLOW_RESISTANCE 计算是否生效。
	 * Calculates success against SLOW_RESISTANCE.
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	@Override
	public void calculate(Effect effect) {
		super.calculate(effect, StatEnum.SLOW_RESISTANCE, null);
	}

	/**
	 * 设置 SLOW 异常状态。
	 * Sets the SLOW abnormal state.
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	@Override
	public void startEffect(Effect effect) {
		super.startEffect(effect);
		effect.setAbnormal(AbnormalState.SLOW.getId());
		effect.getEffected().getEffectController().setAbnormal(AbnormalState.SLOW.getId());
	}

	/**
	 * 清除 SLOW 异常状态。
	 * Clears the SLOW abnormal state.
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	@Override
	public void endEffect(Effect effect) {
		super.endEffect(effect);
		effect.getEffected().getEffectController().unsetAbnormal(AbnormalState.SLOW.getId());
	}
}
