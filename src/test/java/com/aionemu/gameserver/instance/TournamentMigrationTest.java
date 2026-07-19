package com.aionemu.gameserver.instance;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.instance.handlers.InstanceID;
import com.aionemu.gameserver.instance.handlers.scripts.TournamentInstance;

class TournamentMigrationTest {
	@Test
	void oneHandlerOwnsAllRetailTournamentLobbyAndStageMaps() {
		InstanceID instanceId = TournamentInstance.class.getAnnotation(InstanceID.class);
		assertArrayEquals(new int[] { 900230000, 900210000, 302320000, 302310000, 302370000, 302360000,
				302390000, 302380000, 302420000, 302410000 }, instanceId.value());

		InstanceEngine engine = new InstanceEngine();
		engine.addInstanceHandlerClass(TournamentInstance.class);
		for (int worldId : instanceId.value()) {
			assertEquals(TournamentInstance.class, engine.getNewInstanceHandler(worldId).getClass());
		}
	}
}
