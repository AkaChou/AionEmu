package com.aionemu.gameserver.model.gameobjects.player;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import com.aionemu.gameserver.services.events.thievesguildservice.ThievesStatusList;

class PlayerThievesStateTest {

	@Test
	void storesStatusAndKeepsStealingSeparateFromRevengeDuel() {
		Player player = new ObjenesisStd().newInstance(Player.class);
		ThievesStatusList status = new ThievesStatusList();

		player.setThieves(status);
		player.setIsThieves(true);

		assertSame(status, player.getThieves());
		assertTrue(player.isThieves());
		assertFalse(player.isThievesDuel());

		player.setThievesDuel(true);

		assertTrue(player.isThievesDuel());
		assertTrue(player.isThieves());
	}
}
