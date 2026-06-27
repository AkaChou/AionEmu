package com.aionemu.gameserver.model.svs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Map;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import org.junit.jupiter.api.Test;

class SvsLocationTest {

	@Test
	void exposesPlayersAsJdkMap() {
		SvsLocation location = new SvsLocation();

		Map<Integer, Player> players = location.getPlayers();

		assertEquals(HashMap.class, players.getClass());
	}
}
