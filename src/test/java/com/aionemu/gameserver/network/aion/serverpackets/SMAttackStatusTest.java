package com.aionemu.gameserver.network.aion.serverpackets;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SMAttackStatusTest {

	@Test
	void usesTheRetailDrowningStatusType() {
		assertEquals(12, SM_ATTACK_STATUS.TYPE.DROWNING.getValue());
	}
}
