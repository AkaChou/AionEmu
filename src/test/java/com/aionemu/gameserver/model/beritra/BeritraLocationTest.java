package com.aionemu.gameserver.model.beritra;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Map;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import org.junit.jupiter.api.Test;

class BeritraLocationTest {

	@Test
	void exposesPlayersAsJdkMap() {
		BeritraLocation location = new BeritraLocation();

		Map<Integer, Player> players = location.getPlayers();

		assertEquals(HashMap.class, players.getClass());
	}
}
