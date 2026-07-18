package com.aionemu.gameserver.quest.handlers.reshanta;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.world.zone.ZoneName;

class ReshantaQuestMigrationTest {

	@Test
	void emergencyPvpRequiresFivePlayerKills() {
		assertEquals(5, AbstractReshantaEmergencyPvp.REQUIRED_KILLS);
	}

	@Test
	void surveysKeepTheFixedClientRouteOrderAndAdvanceVar0Sequentially() {
		assertRoute(_1868IslandsNearTheLanding::new, "AHDGCFBE");
		assertRoute(_2868IslandsNearTheLanding::new, "DHAEBFCG");
	}

	private static void assertRoute(Supplier<AbstractReshantaSurvey> factory, String order) {
		List<String> names = order.chars()
			.mapToObj(letter -> "AB1_SENSORYAREA_Q1868" + (char) letter + "_400010000")
			.toList();
		names.forEach(ZoneName::createOrGet);
		ZoneName[] route = factory.get().route();
		assertEquals(names, Arrays.stream(route).map(ZoneName::name).toList());
		assertEquals(2, AbstractReshantaSurvey.nextStep(route, 1, route[0]));
		assertEquals(route.length + 1, AbstractReshantaSurvey.nextStep(route, route.length, route[route.length - 1]));
		assertEquals(-1, AbstractReshantaSurvey.nextStep(route, 1, route[1]));
	}
}
