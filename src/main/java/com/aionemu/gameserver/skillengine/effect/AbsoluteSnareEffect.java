package com.aionemu.gameserver.skillengine.effect;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.stats.calc.Stat2;
import com.aionemu.gameserver.model.stats.calc.functions.StatFunction;
import com.aionemu.gameserver.model.stats.container.StatEnum;
import com.aionemu.gameserver.skillengine.model.Effect;

/**
 * 绝对定身效果：作为 Buff 壳，通过修饰器强制限制移动速度。
 * Absolute snare effect: buff shell that forces movement-speed restriction via modifiers.
 *
 * @author Dtem
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "AbsoluteSnareEffect")
public class AbsoluteSnareEffect extends BuffEffect {

	@Override
	public void startEffect(Effect effect) {
		int maxSpeed = value + delta * effect.getSkillLevel();
		if (maxSpeed <= 0) {
			super.startEffect(effect);
			return;
		}
		if (effect.getEffected() instanceof Player player) {
			player.getGameStats().addEffect(effect, List.of(
				new MaximumSpeedFunction(StatEnum.SPEED, maxSpeed),
				new MaximumSpeedFunction(StatEnum.FLY_SPEED, maxSpeed)));
		}
	}

	private static final class MaximumSpeedFunction extends StatFunction {

		private MaximumSpeedFunction(StatEnum stat, int value) {
			super(stat, value, true);
		}

		@Override
		public void apply(Stat2 stat) {
			int excess = stat.getCurrent() - getValue();
			if (excess > 0) {
				stat.addToBonus(-excess);
			}
		}

		@Override
		public int getPriority() {
			return 70;
		}
	}
}
