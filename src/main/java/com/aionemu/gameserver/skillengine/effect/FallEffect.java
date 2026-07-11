package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.skillengine.model.Effect;

/**
 * 强制落地效果：仅对玩家生效，立即结束飞行状态。
 * Forced fall effect: applies to players only and immediately ends flight.
 *
 * @author Sippolo
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "FallEffect")
public class FallEffect extends EffectTemplate {

	/**
	 * 仅对玩家计算强制落地。
	 * Calculates forced fall for players only.
	 */
	@Override
	public void calculate(Effect effect) {
		// 目前仅影响玩家（尚无飞行 NPC）。 / Affects only players (for now as we dont have flying Npc's)
		if (effect.getEffected() instanceof Player) {
			super.calculate(effect, null, null);
		}
	}

	/**
	 * 将效果应用到目标（加入控制器或立即结算）。
	 * Applies the effect to the target (controller attach or immediate settlement).
	 */
	@Override
	public void applyEffect(Effect effect) {
		effect.addToEffectedController();
	}

	/**
	 * 立即结束目标飞行状态。
	 * Immediately ends the target's flight state.
	 */
	@Override
	public void startEffect(Effect effect) {
		((Player) effect.getEffected()).getFlyController().endFly(true);
	}
}
