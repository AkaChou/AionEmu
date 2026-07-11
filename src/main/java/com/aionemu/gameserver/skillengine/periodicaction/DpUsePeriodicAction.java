package com.aionemu.gameserver.skillengine.periodicaction;

import jakarta.xml.bind.annotation.XmlAttribute;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.skillengine.model.Effect;

/**
 * 周期 DP 消耗：效果持续期间按间隔扣除施法玩家 DP，不足则结束效果。
 * Periodic DP cost: deducts caster DP each tick; ends the effect if insufficient.
 *
 * @author MATTY (ADev.Team)
 */
public class DpUsePeriodicAction extends PeriodicAction {

	/**
	 * 每次扣除的 DP 值。
	 * DP amount consumed per tick.
	 */
	@XmlAttribute(name = "value")
	protected int value;

	/**
	 * 扣除施法玩家 DP；不足时结束效果。
	 * Deducts caster DP; ends the effect if DP is insufficient.
	 *
	 * related effect
	 */
	@Override
	public void act(final Effect effect) {
		final Player effector = (Player) effect.getEffector();
		int currentDp = effector.getCommonData().getDp();
		if (currentDp <= 0 || currentDp < value) {
			effect.endEffect();
			return;
		}
		effector.getCommonData().setDp(currentDp - value);
	}
}
