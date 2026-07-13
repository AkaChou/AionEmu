package com.aionemu.gameserver.skillengine.effect;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.stats.container.PlayerGameStats;
import com.aionemu.gameserver.model.stats.container.StatEnum;
import com.aionemu.gameserver.skillengine.model.ActivationAttribute;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.skillengine.model.SkillTemplate;

class AbsoluteSnareEffectTest {

	@Test
	void capsGroundAndFlightSpeedForEffectLifetime() {
		Player player = new ObjenesisStd().newInstance(TestPlayer.class);
		player.setGameStats(new TestPlayerGameStats(player));
		AbsoluteSnareEffect template = new AbsoluteSnareEffect();
		setField(template, "value", 5000);
		SkillTemplate skill = new SkillTemplate();
		setField(skill, "activationAttribute", ActivationAttribute.ACTIVE);
		Effect effect = new Effect(player, player, skill, 1, 0);

		template.startEffect(effect);
		assertEquals(5000, player.getGameStats().getStat(StatEnum.SPEED, 7500).getCurrent());
		assertEquals(5000, player.getGameStats().getStat(StatEnum.FLY_SPEED, 9000).getCurrent());

		template.endEffect(effect);
		assertEquals(7500, player.getGameStats().getStat(StatEnum.SPEED, 7500).getCurrent());
		assertEquals(9000, player.getGameStats().getStat(StatEnum.FLY_SPEED, 9000).getCurrent());
	}

	private static void setField(Object target, String name, Object value) {
		try {
			Field field = target instanceof EffectTemplate
				? EffectTemplate.class.getDeclaredField(name)
				: target.getClass().getDeclaredField(name);
			field.setAccessible(true);
			field.set(target, value);
		} catch (ReflectiveOperationException e) {
			throw new AssertionError(e);
		}
	}

	private static final class TestPlayerGameStats extends PlayerGameStats {

		private TestPlayerGameStats(Player owner) {
			super(owner);
		}

		@Override
		protected void onStatsChange() {
		}
	}

	private static final class TestPlayer extends Player {

		private TestPlayer() {
			super(null, null, null, null);
		}

		@Override
		public byte isPlayer() {
			return 1;
		}
	}
}
