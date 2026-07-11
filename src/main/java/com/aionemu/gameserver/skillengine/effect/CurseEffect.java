package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.stats.container.StatEnum;
import com.aionemu.gameserver.skillengine.model.Effect;

/**
 * 诅咒效果：Buff 属性削弱并标记 CURSE 异常。
 * Curse effect: buff-based stat debuff that marks the CURSE abnormal.
 *
 * @author ATracer
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "CurseEffect")
public class CurseEffect extends BuffEffect {

	/**
	 * 按诅咒抗性计算是否命中。
	 * Calculates hit using curse resistance.
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	@Override
	public void calculate(Effect effect) {
		super.calculate(effect, StatEnum.CURSE_RESISTANCE, null);
	}

	/**
	 * 应用属性修正并设置诅咒异常。
	 * Applies stat modifiers and sets the curse abnormal.
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	@Override
	public void startEffect(Effect effect) {
		super.startEffect(effect);
		effect.setAbnormal(AbnormalState.CURSE.getId());
		effect.getEffected().getEffectController().setAbnormal(AbnormalState.CURSE.getId());
	}

	/**
	 * 移除属性修正并清除诅咒异常。
	 * Removes stat modifiers and clears the curse abnormal.
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	@Override
	public void endEffect(Effect effect) {
		super.endEffect(effect);
		effect.getEffected().getEffectController().unsetAbnormal(AbnormalState.CURSE.getId());
	}
}
