package com.aionemu.gameserver.skillengine.effect;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.stats.calc.functions.StatSetFunction;
import com.aionemu.gameserver.model.stats.container.PlayerGameStats;
import com.aionemu.gameserver.model.stats.container.StatEnum;
import com.aionemu.gameserver.model.templates.stats.ModifiersTemplate;
import com.aionemu.gameserver.skillengine.model.ActivationAttribute;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.skillengine.model.SkillTemplate;

class AbsoluteStatEffectTest {

	@Test
	void appliesConfiguredStatSetForEffectLifetime() {
		Player player = new ObjenesisStd().newInstance(TestPlayer.class);
		player.setGameStats(new TestPlayerGameStats(player));
		ModifiersTemplate modifiers = modifiers(new StatSetFunction(StatEnum.POWER, 77, false));
		TestAbsoluteStatEffect template = new TestAbsoluteStatEffect(modifiers);
		SkillTemplate skill = new SkillTemplate();
		setField(skill, "activationAttribute", ActivationAttribute.PASSIVE);
		Effect effect = new Effect(player, player, skill, 1, 0);

		template.startEffect(effect);
		assertEquals(77, player.getGameStats().getStat(StatEnum.POWER, 1).getCurrent());

		template.endEffect(effect);
		assertEquals(1, player.getGameStats().getStat(StatEnum.POWER, 1).getCurrent());
	}

	private static ModifiersTemplate modifiers(StatSetFunction modifier) {
		ModifiersTemplate template = new ModifiersTemplate();
		setField(template, "modifiers", List.of(modifier));
		return template;
	}

	private static void setField(Object target, String name, Object value) {
		try {
			Field field = target.getClass().getDeclaredField(name);
			field.setAccessible(true);
			field.set(target, value);
		} catch (ReflectiveOperationException e) {
			throw new AssertionError(e);
		}
	}

	private static final class TestAbsoluteStatEffect extends AbsoluteStatToPCBuffEffect {
		private final ModifiersTemplate modifiers;

		private TestAbsoluteStatEffect(ModifiersTemplate modifiers) {
			this.modifiers = modifiers;
		}

		@Override
		public ModifiersTemplate getModifiersSet() {
			return modifiers;
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
