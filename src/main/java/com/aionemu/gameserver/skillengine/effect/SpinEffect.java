package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.stats.container.StatEnum;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.skillengine.model.SpellStatus;

/**
 * 旋转效果：使目标进入旋转控制，打断技能并中止移动。
 * Spin effect: puts the target into spin control, cancelling skill and movement.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SpinEffect")
public class SpinEffect extends EffectTemplate {
	/**
	 * 加入效果控制器并移除麻痹类效果。
	 * Adds to the controller and removes paralyze effects.
	 */
	@Override
	public void applyEffect(Effect effect) {
		effect.addToEffectedController();
		effect.getEffected().getEffectController().removeParalyzeEffects();
	}

	/**
	 * 按旋转抗性结算，状态为 SPIN。
	 * Resolves against spin resistance with SpellStatus.SPIN.
	 */
	@Override
	public void calculate(Effect effect) {
		super.calculate(effect, StatEnum.SPIN_RESISTANCE, SpellStatus.SPIN);
	}

	/**
	 * 打断技能、中止移动并施加 SPIN 异常。
	 * Cancels skill, aborts move, and applies SPIN abnormal.
	 */
	@Override
	public void startEffect(Effect effect) {
		final Creature effected = effect.getEffected();
		effected.getController().cancelCurrentSkill();
		if (!(effected instanceof Npc)) {
			effected.getMoveController().abortMove();
		}
		effected.getEffectController().setAbnormal(AbnormalState.SPIN.getId());
		effect.setAbnormal(AbnormalState.SPIN.getId());
	}

	/**
	 * 清除 SPIN 异常。
	 * Clears the SPIN abnormal.
	 */
	@Override
	public void endEffect(Effect effect) {
		effect.getEffected().getEffectController().unsetAbnormal(AbnormalState.SPIN.getId());
	}
}
