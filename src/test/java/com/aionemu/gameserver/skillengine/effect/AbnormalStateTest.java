package com.aionemu.gameserver.skillengine.effect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AbnormalStateTest {

	@Test
	void confuseBlocksAttacksButAllowsForcedMovement() {
		int confuse = AbnormalState.CONFUSE.getId();

		assertEquals(1 << 11, confuse);
		assertTrue((AbnormalState.CANT_ATTACK_STATE.getId() & confuse) != 0);
		assertFalse((AbnormalState.CANT_MOVE_STATE.getId() & confuse) != 0);
	}
}
