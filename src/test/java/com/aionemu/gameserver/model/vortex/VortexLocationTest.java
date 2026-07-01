package com.aionemu.gameserver.model.vortex;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Map;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import org.junit.jupiter.api.Test;

class VortexLocationTest {

	@Test
	void exposesPlayersAsJdkMap() {
		VortexLocation location = new VortexLocation();

		Map<Integer, Player> players = location.getPlayers();

		assertEquals(HashMap.class, players.getClass());
	}
}
