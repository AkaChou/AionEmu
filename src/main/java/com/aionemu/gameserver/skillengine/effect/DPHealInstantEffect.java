package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.skillengine.model.HealType;

/**
 * DP 瞬时治疗效果：立即恢复神圣力。
 * Instant DP heal effect: restores divine power immediately.
 *
 * @author ATracer
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "DPHealInstantEffect")
public class DPHealInstantEffect extends AbstractHealEffect {

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
	 * 应用 DP 治疗。
	 * Applies DP heal.
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	@Override
	public void applyEffect(Effect effect) {
		super.applyEffect(effect, HealType.DP);
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
