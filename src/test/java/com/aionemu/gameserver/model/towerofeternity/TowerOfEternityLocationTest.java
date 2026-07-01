package com.aionemu.gameserver.model.towerofeternity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Map;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import org.junit.jupiter.api.Test;

class TowerOfEternityLocationTest {

	@Test
	void exposesPlayersAsJdkMap() {
		TowerOfEternityLocation location = new TowerOfEternityLocation();

		Map<Integer, Player> players = location.getPlayers();

		assertEquals(HashMap.class, players.getClass());
	}
}
