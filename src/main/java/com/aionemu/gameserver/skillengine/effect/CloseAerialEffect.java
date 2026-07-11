package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.skillengine.model.SpellStatus;

/**
 * 解除浮空效果：在目标处于 OPENAERIAL 时移除对应浮空技能。
 * Close-aerial effect: removes open-aerial skills when the target is airborne.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "CloseAerialEffect")
public class CloseAerialEffect extends EffectTemplate {
	/**
	 * 移除浮空相关效果。
	 * Removes open-aerial related effects.
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	@Override
	public void applyEffect(Effect effect) {
		effect.getEffected().getEffectController().removeEffect(8224);
		effect.getEffected().getEffectController().removeEffect(8678);
	}

	/**
	 * 仅在目标处于 OPENAERIAL 时计算成功。
	 * Succeeds only if the target has the OPENAERIAL abnormal.
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	@Override
	public void calculate(Effect effect) {
		if (!effect.getEffected().getEffectController().isAbnormalSet(AbnormalState.OPENAERIAL)) {
			return;
		}
		super.calculate(effect, null, SpellStatus.CLOSEAERIAL);
	}
}
