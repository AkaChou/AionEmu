package com.aionemu.gameserver.model.siege;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import org.junit.jupiter.api.Test;

class SiegeLocationTest {

	@Test
	void exposesPlayersAsThreadSafeJdkMap() {
		SiegeLocation location = new SiegeLocation();

		Map<Integer, Player> players = location.getPlayers();

		assertEquals("java.util.Collections$SynchronizedMap", players.getClass().getName());
		assertTrue(players instanceof Map);
	}
}
