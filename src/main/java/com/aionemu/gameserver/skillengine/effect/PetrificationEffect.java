package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.stats.container.StatEnum;
import com.aionemu.gameserver.skillengine.model.Effect;

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
