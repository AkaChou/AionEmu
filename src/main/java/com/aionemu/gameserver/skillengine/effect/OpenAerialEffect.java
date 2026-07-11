package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.stats.container.StatEnum;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.skillengine.model.SpellStatus;

/**
 * 开启空中状态效果：施加 OPENAERIAL 异常，使目标进入可被空中连击的状态。
 * Open-aerial effect: applies OPENAERIAL so the target can be juggled in air.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "OpenAerialEffect")
public class OpenAerialEffect extends EffectTemplate {
	/**
	 * 将效果应用到目标（加入控制器或立即结算）。
	 * Applies the effect to the target (controller attach or immediate settlement).
	 */
	@Override
	public void applyEffect(Effect effect) {
		if (!effect.getEffected().getEffectController().isAbnormalSet(AbnormalState.STUMBLE)) {
			effect.addToEffectedController();
			effect.getEffected().getEffectController().removeParalyzeEffects();
		}
	}

	/**
	 * 计算本效果是否成功命中/生效，并写入效果上下文。
	 * Calculates whether this effect succeeds and writes into the effect context.
	 */
	@Override
	public void calculate(Effect effect) {
		super.calculate(effect, StatEnum.OPENAREIAL_RESISTANCE, SpellStatus.OPENAERIAL);
	}

	/**
	 * 设置 OPENAERIAL 异常状态。
	 * Sets the OPENAERIAL abnormal state.
	 */
	@Override
	public void startEffect(Effect effect) {
		final Creature effected = effect.getEffected();
		effected.getController().cancelCurrentSkill();
		effected.getMoveController().abortMove();
		effected.getEffectController().setAbnormal(AbnormalState.OPENAERIAL.getId());
		effect.setAbnormal(AbnormalState.OPENAERIAL.getId());
	}

	/**
	 * 清除 OPENAERIAL 异常状态。
	 * Clears the OPENAERIAL abnormal state.
	 */
	@Override
	public void endEffect(Effect effect) {
		effect.getEffected().getEffectController().unsetAbnormal(AbnormalState.OPENAERIAL.getId());
	}
}
