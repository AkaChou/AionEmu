package com.aionemu.gameserver.skillengine.effect;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.skillengine.model.Effect;

/**
 * 延迟飞行值攻击即时效果：延迟后按固定值或百分比削减目标 FP。
 * Delayed FP attack instant effect: after delay, reduces target FP by fixed or percent value.
 *
 * @author Sippolo
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "DelayedFPAttackInstantEffect")
public class DelayedFPAttackInstantEffect extends EffectTemplate {

	@XmlAttribute
	protected int delay;
	@XmlAttribute
	protected boolean percent;

	/**
	 * 仅对玩家目标计算 FP 削减量并写入 reserved2。
	 * Calculates FP reduction for player targets into reserved2.
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	@Override
	public void calculate(Effect effect) {
		if (!(effect.getEffected() instanceof Player)) {
			return;
		}
		if (!super.calculate(effect, null, null)) {
			return;
		}
		int maxFP = ((Player) effect.getEffected()).getLifeStats().getMaxFp();
		int newValue = (percent) ? (int) ((maxFP * value) / 100) : value;

		effect.setReserved2(newValue);
	}

	/**
	 * 延迟 delay 毫秒后削减目标 FP。
	 * Schedules FP reduction after the configured delay milliseconds.
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	@Override
	public void applyEffect(final Effect effect) {
		final Player effected = (Player) effect.getEffected();
		final int newValue = effect.getReserved2();

		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				effected.getLifeStats().reduceFp(newValue);
			}
		}, delay);
	}
}
