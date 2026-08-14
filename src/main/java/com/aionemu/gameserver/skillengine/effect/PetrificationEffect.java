package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.stats.container.StatEnum;
import com.aionemu.gameserver.skillengine.model.Effect;

/**
 * 石化效果：使目标石化，中断当前施法，非 NPC 目标中止移动，效果结束时解除石化状态。
 * Petrification effect: petrifies the target, cancels its current skill and aborts movement for non-NPC targets; the petrification state is removed when the effect ends.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "PetrificationEffect")
public class PetrificationEffect extends BuffEffect {

	@Override
	public void calculate(Effect effect) {
		super.calculate(effect, StatEnum.PERIFICATION_RESISTANCE, null);
	}

	@Override
	public void startEffect(Effect effect) {
		super.startEffect(effect);
		Creature effected = effect.getEffected();
		effected.getController().cancelCurrentSkill();
		if (!(effected instanceof Npc)) {
			effected.getMoveController().abortMove();
		}
		effected.getEffectController().setAbnormal(AbnormalState.PETRIFICATION.getId());
		effect.setAbnormal(AbnormalState.PETRIFICATION.getId());
	}

	@Override
	public void endEffect(Effect effect) {
		effect.getEffected().getEffectController().unsetAbnormal(AbnormalState.PETRIFICATION.getId());
		super.endEffect(effect);
	}
}
