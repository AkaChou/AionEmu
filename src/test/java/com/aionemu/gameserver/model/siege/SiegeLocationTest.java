package com.aionemu.gameserver.model.siege;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Map;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import org.junit.jupiter.api.Test;

class SiegeLocationTest {

	@Test
	void exposesPlayersAsJdkMap() {
		SiegeLocation location = new SiegeLocation();

		Map<Integer, Player> players = location.getPlayers();

		assertEquals(HashMap.class, players.getClass());
	}
}
