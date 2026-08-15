package com.aionemu.gameserver.ai.walkers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.state.CreatureState;
import com.aionemu.gameserver.model.stats.container.NpcGameStats;
import com.aionemu.gameserver.model.templates.npc.NpcTemplate;
import com.aionemu.gameserver.model.templates.stats.NpcStatsTemplate;

class WalkGeneralRunnerAI2Test {

	@Test
	void runModeSynchronizesEachChangedSpeedOnce() throws ReflectiveOperationException {
		Npc owner = new ObjenesisStd().newInstance(Npc.class);
		NpcStatsTemplate templateStats = new NpcStatsTemplate();
		templateStats.setWalkSpeed(2);
		templateStats.setRunSpeed(6);
		setField(templateStats, "runSpeedFight", 4.2f);
		NpcTemplate template = new NpcTemplate();
		template.setStatsTemplate(templateStats);
		owner.setObjectTemplate(template);
		TrackingNpcGameStats gameStats = new TrackingNpcGameStats(owner);
		owner.setGameStats(gameStats);
		owner.setState(CreatureState.WALKING);

		WalkGeneralRunnerAI2.setRunMode(owner, true);
		WalkGeneralRunnerAI2.setRunMode(owner, true);

		assertTrue(owner.isInState(CreatureState.WEAPON_EQUIPPED));
		assertEquals(List.of(4.2f), gameStats.syncedSpeeds);

		WalkGeneralRunnerAI2.setRunMode(owner, false);

		assertFalse(owner.isInState(CreatureState.WEAPON_EQUIPPED));
		assertEquals(List.of(4.2f, 2f), gameStats.syncedSpeeds);
	}

	private static void setField(Object target, String name, Object value) throws ReflectiveOperationException {
		Field field = target.getClass().getDeclaredField(name);
		field.setAccessible(true);
		field.set(target, value);
	}

	private static final class TrackingNpcGameStats extends NpcGameStats {
		private final List<Float> syncedSpeeds = new ArrayList<>();

		private TrackingNpcGameStats(Npc owner) {
			super(owner);
		}

		@Override
		public void updateSpeedInfo() {
			syncedSpeeds.add(getMovementSpeedFloat());
		}
	}
}
