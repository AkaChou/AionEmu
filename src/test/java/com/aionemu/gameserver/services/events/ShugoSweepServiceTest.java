package com.aionemu.gameserver.services.events;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import com.aionemu.gameserver.model.gameobjects.player.Player;

class ShugoSweepServiceTest {

	private final ObjenesisStd objenesis = new ObjenesisStd();

	@Test
	void onLogoutHandlesNullPlayerOrNullSweepWithoutThrowing() {
		ShugoSweepService service = objenesis.newInstance(ShugoSweepService.class);

		assertDoesNotThrow(() -> service.onLogout(null));

		Player player = objenesis.newInstance(Player.class);
		assertDoesNotThrow(() -> service.onLogout(player));
	}
}
