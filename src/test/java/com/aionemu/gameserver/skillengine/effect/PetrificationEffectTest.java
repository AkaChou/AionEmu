package com.aionemu.gameserver.skillengine.effect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.lang.reflect.Field;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.stats.calc.StatOwner;
import com.aionemu.gameserver.model.stats.calc.functions.StatAddFunction;
import com.aionemu.gameserver.model.stats.calc.functions.IStatFunction;
import com.aionemu.gameserver.model.stats.container.PlayerGameStats;
import com.aionemu.gameserver.model.stats.container.StatEnum;
import com.aionemu.gameserver.skillengine.change.Change;
import com.aionemu.gameserver.skillengine.change.Func;
import com.aionemu.gameserver.skillengine.model.ActivationAttribute;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.skillengine.model.SkillTemplate;

class PetrificationEffectTest {

	@Test
	void blocksAttackMovementAndRide() {
		int petrification = AbnormalState.PETRIFICATION.getId();

		assertEquals(petrification, AbnormalState.CANT_ATTACK_STATE.getId() & petrification);
		assertEquals(petrification, AbnormalState.CANT_MOVE_STATE.getId() & petrification);
		assertEquals(petrification, AbnormalState.DISMOUT_RIDE.getId() & petrification);
	}

	@Test
	void appliesLevelScaledPhysicalDefensePercentForEffectLifetime() {
		Player player = new ObjenesisStd().newInstance(TestPlayer.class);
		player.setGameStats(new TestPlayerGameStats(player));
		TestPetrificationEffect template = new TestPetrificationEffect();
		setField(template, EffectTemplate.class, "change", List.of(change(StatEnum.PHYSICAL_DEFENSE, Func.PERCENT, 10, 10)));
		Effect effect = new Effect(player, player, activeSkill(), 2, 0);

		player.getGameStats().addEffect(effect, template.modifiers(effect));
		assertEquals(1300, player.getGameStats().getStat(StatEnum.PHYSICAL_DEFENSE, 1000).getCurrent());

		player.getGameStats().endEffect(effect);
		assertEquals(1000, player.getGameStats().getStat(StatEnum.PHYSICAL_DEFENSE, 1000).getCurrent());
	}

	@Test
	void treatsPetrificationResistanceAsAnAlteredState() {
		Player player = new ObjenesisStd().newInstance(TestPlayer.class);
		player.setGameStats(new TestPlayerGameStats(player));
		StatOwner resistance = new StatOwner() { };
		player.getGameStats().addEffect(resistance,
			List.of(new StatAddFunction(StatEnum.ABNORMAL_RESISTANCE_ALL, 9999, true)));
		Effect effect = new Effect(player, player, activeSkill(), 1, 0);

		assertFalse(new PetrificationEffect().calculateEffectResistRate(effect, StatEnum.PERIFICATION_RESISTANCE));
	}

	private static Change change(StatEnum stat, Func func, int value, int delta) {
		Change change = new Change();
		setField(change, Change.class, "stat", stat);
		setField(change, Change.class, "func", func);
		setField(change, Change.class, "value", value);
		setField(change, Change.class, "delta", delta);
		return change;
	}

	private static SkillTemplate activeSkill() {
		SkillTemplate skill = new SkillTemplate();
		setField(skill, SkillTemplate.class, "activationAttribute", ActivationAttribute.ACTIVE);
		return skill;
	}

	private static void setField(Object target, Class<?> owner, String name, Object value) {
		try {
			Field field = owner.getDeclaredField(name);
			field.setAccessible(true);
			field.set(target, value);
		} catch (ReflectiveOperationException e) {
			throw new AssertionError(e);
		}
	}

	private static final class TestPetrificationEffect extends PetrificationEffect {

		private List<IStatFunction> modifiers(Effect effect) {
			return getModifiers(effect);
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

		@Override
		public byte getLevel() {
			return 1;
		}
	}
}
