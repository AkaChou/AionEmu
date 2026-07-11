package com.aionemu.gameserver.skillengine.effect;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import jakarta.xml.bind.annotation.XmlAttribute;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.skillengine.model.Effect;

/**
 * 延迟 FP 攻击效果：延迟后削减敌对玩家的飞行点数。
 * Delayed FP attack: after a delay, reduces an enemy player's flight points.
 */
public class DelayedFpAtkInstantEffect extends EffectTemplate {

	@XmlAttribute
	protected int delay;

	@XmlAttribute
	protected boolean percent;

	/**
	 * 仅对玩家目标计算命中。
	 * Calculates hit only when the target is a player.
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	public void calculate(Effect effect) {
		if ((effect.getEffected() instanceof Player)) {
			super.calculate(effect, null, null);
		}
	}

	/**
	 * 延迟后对敌对玩家结算 FP 伤害。
	 * After delay, applies FP damage to an enemy player.
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	public void applyEffect(final Effect effect) {
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {

			public void run() {
				if (effect.getEffector().isEnemy(effect.getEffected())) {
					DelayedFpAtkInstantEffect.this.calculateAndApplyDamage(effect);
				}
			}
		}, delay);
	}

	/**
	 * 计算并削减目标 FP。
	 * Calculates and reduces the target's FP.
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	private void calculateAndApplyDamage(Effect effect) {
		if (!(effect.getEffected() instanceof Player)) {
			return;
		}
		int valueWithDelta = value + delta * effect.getSkillLevel();
		Player player = (Player) effect.getEffected();
		int maxFP = player.getLifeStats().getMaxFp();

		int newValue = valueWithDelta;

		if (percent) {
			newValue = maxFP * valueWithDelta / 100;
		}
		player.getLifeStats().reduceFp(newValue);
	}
}
