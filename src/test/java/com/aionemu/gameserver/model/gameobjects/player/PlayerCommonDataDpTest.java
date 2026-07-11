package com.aionemu.gameserver.model.gameobjects.player;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.model.PlayerClass;

class PlayerCommonDataDpTest {

	@Test
	void storesDpWhilePlayerIsOffline() {
		PlayerCommonData commonData = new PlayerCommonData(1);
		commonData.setPlayerClass(PlayerClass.GLADIATOR);

		commonData.setDp(1234);

		assertEquals(1234, commonData.getDp());
	}
}
