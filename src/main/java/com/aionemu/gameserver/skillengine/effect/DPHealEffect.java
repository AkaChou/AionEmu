package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.skillengine.model.HealType;

/**
 * DP 持续治疗效果：周期恢复神圣力。
 * DP over-time heal effect: periodically restores divine power.
 *
 * @author kecimis
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "DPHealEffect")
public class DPHealEffect extends HealOverTimeEffect {

	/**
	 * 按 DP 治疗类型计算。
	 * Calculates as DP heal.
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	@Override
	public void calculate(Effect effect) {
		super.calculate(effect, HealType.DP);
	}

	/**
	 * 周期结算 DP 治疗。
	 * Applies one tick of DP heal.
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	@Override
	public void onPeriodicAction(Effect effect) {
		super.onPeriodicAction(effect, HealType.DP);
	}

	/**
	 * 返回当前 DP。
	 * Returns current DP.
	 *
	 * @param effect 运行时效果 / runtime effect
	 * current DP
	 */
	@Override
	protected int getCurrentStatValue(Effect effect) {
		return ((Player) effect.getEffected()).getCommonData().getDp();
	}

	/**
	 * 返回 DP 上限。
	 * Returns max DP.
	 *
	 * @param effect 运行时效果 / runtime effect
	 * max DP
	 */
	@Override
	protected int getMaxStatValue(Effect effect) {
		return ((Player) effect.getEffected()).getGameStats().getMaxDp().getCurrent();
	}
}
