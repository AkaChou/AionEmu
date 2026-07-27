package com.aionemu.gameserver.ai2.handler;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aionemu.gameserver.ai2.AIState;
import com.aionemu.gameserver.ai2.AISubState;
import com.aionemu.gameserver.ai2.NpcAI2;
import org.junit.jupiter.api.Test;

class TargetEventHandlerTest {

	@Test
	void switchingTargetClearsOnlyLostSubState() {
		NpcAI2 ai = new NpcAI2();
		ai.setSubStateIfNot(AISubState.TARGET_LOST);
		TargetEventHandler.clearTargetLostState(ai);
		assertTrue(ai.isInSubState(AISubState.NONE));

		ai.setSubStateIfNot(AISubState.CAST);
		TargetEventHandler.clearTargetLostState(ai);
		assertTrue(ai.isInSubState(AISubState.CAST));
	}

	@Test
	void leavingFightClearsLostTargetState() {
		NpcAI2 ai = new NpcAI2();
		ai.setStateIfNot(AIState.FIGHT);
		ai.setSubStateIfNot(AISubState.TARGET_LOST);

		ai.setStateIfNot(AIState.WALKING);

		assertTrue(ai.isInSubState(AISubState.NONE));
	}
}
