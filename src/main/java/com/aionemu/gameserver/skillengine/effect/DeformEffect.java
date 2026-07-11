package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.stats.container.StatEnum;
import com.aionemu.gameserver.skillengine.model.Effect;

/**
 * 变形效果：变身并标记 DEFORM 异常（受变形抗性影响）。
 * Deform effect: transforms the target and marks DEFORM (uses deform resistance).
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "DeformEffect")
public class DeformEffect extends TransformEffect {
	/**
	 * 按变形抗性计算是否命中。
	 * Calculates hit using deform resistance.
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	@Override
	public void calculate(Effect effect) {
		super.calculate(effect, StatEnum.DEFORM_RESISTANCE, null);
	}

	/**
	 * 启动变形并设置 DEFORM 异常。
	 * Starts transform with the DEFORM abnormal.
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	@Override
	public void startEffect(Effect effect) {
		super.startEffect(effect, AbnormalState.DEFORM);
	}

	/**
	 * 结束变形并清除 DEFORM 异常。
	 * Ends transform and clears the DEFORM abnormal.
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	@Override
	public void endEffect(Effect effect) {
		super.endEffect(effect, AbnormalState.DEFORM);
	}
}
