package com.aionemu.gameserver.ai2.handler;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aionemu.gameserver.ai2.AIState;
import com.aionemu.gameserver.ai2.NpcAI2;
import org.junit.jupiter.api.Test;

class AttackEventHandlerTest {

	@Test
	void onlyFirstAttackEventEntersFight() {
		NpcAI2 ai = new NpcAI2();

		assertTrue(AttackEventHandler.tryEnterFight(ai));
		assertFalse(AttackEventHandler.tryEnterFight(ai));
	}

	@Test
	void attackDoesNotRestartAnActiveReturnState() {
		NpcAI2 ai = new NpcAI2();
		ai.setStateIfNot(AIState.RETURNING);

		assertFalse(AttackEventHandler.tryEnterFight(ai));
		assertTrue(ai.isInState(AIState.RETURNING));
	}
}
