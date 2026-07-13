package com.aionemu.gameserver.network.aion.clientpackets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.model.EmotionType;

class CMEmotionTest {

	@Test
	void retailFlagPreventsOnlyJumpFromCancelingTheCurrentSkill() {
		assertFalse(CM_EMOTION.shouldCancelCurrentSkill(EmotionType.JUMP, true));
		assertTrue(CM_EMOTION.shouldCancelCurrentSkill(EmotionType.JUMP, false));
		assertTrue(CM_EMOTION.shouldCancelCurrentSkill(EmotionType.SIT, true));
	}
}
