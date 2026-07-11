package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.skillengine.model.Effect;

/**
 * 回火保护效果：为装备强化提供保护（Buff 形态）。
 * Tempering protection effect: protects gear enhancement (buff form).
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "TemperingProtectionEffect")
public class TemperingProtectionEffect extends BuffEffect {

	@Override
	public void calculate(Effect effect) {
		effect.addSucessEffect(this);
	}

	@Override
	public void applyEffect(Effect effect) {
		effect.addToEffectedController();
	}
}
