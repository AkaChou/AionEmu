package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.skillengine.model.Effect;

/**
 * 无敌之翼效果：保护飞行中的玩家免受部分伤害/打断。
 * Invulnerable wing effect: protects a flying player from some damage/interrupts.
 *
 * @author VladimirZ
 * @author Sippolo
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "InvulnerableWingEffect")
public class InvulnerableWingEffect extends EffectTemplate {

	/**
	 * 计算无敌之翼是否生效。
	 * Calculates whether invulnerable wing applies.
	 */
	@Override
	public void calculate(Effect effect) {
		// 仅玩家 / Only for players
		if (effect.getEffected() instanceof Player) {
			super.calculate(effect, null, null);
		}
	}

	/**
	 * 应用无敌之翼到目标。
	 * Applies invulnerable wing to the target.
	 */
	@Override
	public void applyEffect(final Effect effect) {
		effect.addToEffectedController();
		((Player) effect.getEffected()).setInvulnerableWing(true);
	}

	/**
	 * 结束无敌之翼效果。
	 * Ends the invulnerable wing effect.
	 */
	@Override
	public void endEffect(Effect effect) {
		((Player) effect.getEffected()).setInvulnerableWing(false);
	}
}
