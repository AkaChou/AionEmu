package com.aionemu.gameserver.model.siege;

import java.util.Map;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SiegeLocationTest {

	@Test
	void exposesPlayersAsThreadSafeJdkMap() {
		SiegeLocation location = new SiegeLocation();

		Map<Integer, Player> players = location.getPlayers();

		assertEquals("java.util.Collections$SynchronizedMap", players.getClass().getName());
		assertInstanceOf(Map.class, players);
	}
}
