package com.aionemu.gameserver.skillengine.effect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.skillengine.model.SkillTemplate;

class PersistentUtilityEffectTest {

	@Test
	void mapsAndMarksPersistentUtilityEffects() throws Exception {
		for (var entry : List.of(
			new Entry(new ReturnCoolReduceEffect(), EffectType.RETURNCOOLREDUCE, "isReturnCoolReduce"),
			new Entry(new SprintFpReduceEffect(), EffectType.SPRINTFPREDUCE, "isSprintFpReduce"),
			new Entry(new OdellaRecoverIncreaseEffect(), EffectType.ODELLARECOVERINCREASE, "isOdellaRecoverIncrease"),
			new Entry(new DeathPenaltyReduceEffect(), EffectType.DEATHPENALTYREDUCE, "isDeathPenaltyReduce"))) {
			entry.template().afterUnmarshal(null, null);
			Effect effect = new Effect(null, null, new SkillTemplate(), 1, 0);
			entry.template().calculate(effect);
			Method marker = Effect.class.getMethod(entry.marker());

			assertEquals(entry.type(), entry.template().getEffectType());
			assertTrue((boolean) marker.invoke(effect));
		}
	}

	private record Entry(EffectTemplate template, EffectType type, String marker) {
	}
}
