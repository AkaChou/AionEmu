package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.skillengine.model.Effect;

/**
 * 飞行点瞬时削减效果：立即扣除目标 FP。
 * Instant FP attack effect: immediately reduces the target's flight points.
 *
 * @author Sippolo
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "FpAttackInstantEffect")
public class FpAttackInstantEffect extends EffectTemplate {

	@XmlAttribute
	protected boolean percent;

	/**
	 * 计算瞬时 FP 削减。
	 * Calculates instant FP drain.
	 */
	@Override
	public void calculate(Effect effect) {
		// 仅玩家有飞行时间 / Only players have FP
		if (effect.getEffected() instanceof Player) {
			super.calculate(effect, null, null);
		}
	}

	/**
	 * 立即扣除目标飞行点。
	 * Immediately drains the target's flight points.
	 */
	@Override
	public void applyEffect(Effect effect) {
		// 因其他生物无飞行时间而限制仅对玩家 / Restriction to players because lack of FP on other Creatures
		if (!(effect.getEffected() instanceof Player)) {
			return;
		}
		Player player = (Player) effect.getEffected();
		int maxFP = player.getLifeStats().getMaxFp();
		int newValue = value;
		// 支持百分比数值 / Support for values in percentage
		if (percent) {
			newValue = (int) ((maxFP * value) / 100);
		}
		player.getLifeStats().reduceFp(newValue);
	}
}
