package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.summons.UnsummonType;
import com.aionemu.gameserver.skillengine.model.Effect;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "PetOrderUnSummonEffect")
public class PetOrderUnSummonEffect extends EffectTemplate {

	@Override
	public void applyEffect(Effect effect) {
		Player player = (Player) effect.getEffector();
		if (player.getSummon() != null) {
			player.getSummon().getController().release(UnsummonType.UNSPECIFIED);
		}
	}

	@Override
	public void calculate(Effect effect) {
		if (effect.getEffector() instanceof Player player && player.getSummon() != null) {
			super.calculate(effect, null, null);
		}
	}
}
