package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.skillengine.model.Effect;

/**
 * DP 转移效果：将施法者当前 DP 转给目标玩家。
 * DP transfer effect: transfers the effector's current DP to the target player.
 *
 * @author Sippolo
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "DPTransferEffect")
public class DPTransferEffect extends EffectTemplate {

	/**
	 * 从目标扣除预留 DP，并加到施法者（方向以 reserved1 符号为准）。
	 * Subtracts reserved DP from the target and adds it to the effector (sign via reserved1).
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	@Override
	public void applyEffect(Effect effect) {
		((Player) effect.getEffected()).getCommonData().addDp(-effect.getReserved1());
		((Player) effect.getEffector()).getCommonData().addDp(effect.getReserved1());
	}

	/**
	 * 计算可转移 DP，写入 reserved1。
	 * Calculates transferable DP into reserved1.
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	@Override
	public void calculate(Effect effect) {
		if (!super.calculate(effect, null, null)) {
			return;
		}
		effect.setReserved1(-getCurrentStatValue(effect));
	}

	private int getCurrentStatValue(Effect effect) {
		return ((Player) effect.getEffector()).getCommonData().getDp();
	}

	@SuppressWarnings("unused")
	private int getEffectedCurrentStatValue(Effect effect) {
		return ((Player) effect.getEffected()).getCommonData().getDp();
	}

	@SuppressWarnings("unused")
	private int getMaxStatValue(Effect effect) {
		return ((Player) effect.getEffected()).getGameStats().getMaxDp().getCurrent();
	}
}
