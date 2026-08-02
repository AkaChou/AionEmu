package com.aionemu.gameserver.controllers;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aionemu.gameserver.model.gameobjects.Summon;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.state.CreatureState;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

class SummonControllerTest {

	@Test
	void mapDespawnDoesNotReleaseSummonAsIfMasterWereOutOfRange() {
		Player master = object(Player.class);
		Summon summon = object(Summon.class);
		summon.setMaster(master);

		assertFalse(SummonController.isMasterOutOfRange(summon, master, false));
		assertTrue(SummonController.isMasterOutOfRange(summon, master, true));

		master.setState(CreatureState.FLIGHT_TELEPORT);
		master.setFlightTeleportId(1);
		assertFalse(SummonController.isMasterOutOfRange(summon, master, true));
	}

	private static <T> T object(Class<T> type) {
		return new ObjenesisStd().newInstance(type);
	}
}
