package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.summons.UnsummonType;
import com.aionemu.gameserver.skillengine.model.Effect;

/**
 * 宠物解散命令效果：解除施法者当前召唤的宠物（无宠物时不生效）。
 * Pet unsummon order effect: releases the caster's currently summoned pet (no-op without a summon).
 */
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
