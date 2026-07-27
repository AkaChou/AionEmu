package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.stats.container.StatEnum;
import com.aionemu.gameserver.skillengine.model.Effect;

/**
 * 禁飞效果：强制玩家落地并施加 NOFLY 异常状态。
 * No-fly effect: forces a player to land and applies the NOFLY abnormal state.
 *
 * @author Sippolo
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "NoFlyEffect")
public class NoFlyEffect extends EffectTemplate {

	/**
	 * 仅对玩家计算禁飞效果。
	 * Calculates the no-fly effect for players only.
	 */
	@Override
	public void calculate(Effect effect) {
		// 目前仅影响玩家（尚无飞行 NPC）。 / Affects only players (for now as we dont have flying Npc's)
		if (effect.getEffected() instanceof Player) {
			super.calculate(effect, StatEnum.NOFLY_RESISTANCE, null);
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
	 * 强制落地并设置 NOFLY 异常。
	 * Forces landing and sets the NOFLY abnormal state.
	 */
	@Override
	public void startEffect(Effect effect) {
		((Player) effect.getEffected()).getFlyController().endFly(true);

		effect.setAbnormal(AbnormalState.NOFLY.getId());
		effect.getEffected().getEffectController().setAbnormal(AbnormalState.NOFLY.getId());
	}

	/**
	 * 清除 NOFLY 异常状态。
	 * Clears the NOFLY abnormal state.
	 */
	@Override
	public void endEffect(Effect effect) {
		effect.getEffected().getEffectController().unsetAbnormal(AbnormalState.NOFLY.getId());
	}
}
