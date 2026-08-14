package com.aionemu.gameserver.ai2.manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.ai.GeneralNpcAI2;
import com.aionemu.gameserver.ai2.AIState;
import com.aionemu.gameserver.ai2.AISubState;
import com.aionemu.gameserver.model.gameobjects.Npc;
import org.objenesis.ObjenesisStd;

class SkillAttackManagerBytecodeTest {

	@Test
	void scheduledSkillAttacksDoNotDependOnSeparateNestedActionClasses() throws IOException {
		String classResource = "com/aionemu/gameserver/ai2/manager/SkillAttackManager.class";
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		byte[] classBytes;
		try (var inputStream = classLoader.getResourceAsStream(classResource)) {
			assertNotNull(inputStream, classResource + " should be compiled before the test runs");
			classBytes = inputStream.readAllBytes();
		}

		String constantPoolText = new String(classBytes, StandardCharsets.ISO_8859_1);

		assertFalse(constantPoolText.contains("com/aionemu/gameserver/ai2/manager/SkillAttackManager$SkillAction"),
			"scheduled NPC skills should not require SkillAttackManager$SkillAction.class at runtime");
		assertFalse(constantPoolText.contains("com/aionemu/gameserver/ai2/manager/SkillAttackManager$1"),
			"NPC skill selection should not require a compiler-generated enum switch-map class at runtime");
	}

	@Test
	void scheduledSkillRunsOnlyForTheActiveCombatCast() {
		Npc target = new ObjenesisStd().newInstance(Npc.class);
		assertTrue(SkillAttackManager.canExecuteScheduledSkill(AIState.FIGHT, AISubState.CAST, target, target, 1, 1, 1, 1, 10, 10));
		assertFalse(SkillAttackManager.canExecuteScheduledSkill(AIState.IDLE, AISubState.CAST, target, target, 1, 1, 1, 1, 10, 10));
		assertFalse(SkillAttackManager.canExecuteScheduledSkill(AIState.FIGHT, AISubState.NONE, target, target, 1, 1, 1, 1, 10, 10));
		assertFalse(SkillAttackManager.canExecuteScheduledSkill(AIState.FIGHT, AISubState.CAST,
				new ObjenesisStd().newInstance(Npc.class), target, 1, 1, 1, 1, 10, 10));
		assertFalse(SkillAttackManager.canExecuteScheduledSkill(AIState.FIGHT, AISubState.CAST, target, target, 2, 1, 1, 1, 10, 10));
		assertFalse(SkillAttackManager.canExecuteScheduledSkill(AIState.FIGHT, AISubState.CAST, target, target, 1, 1, 2, 1, 10, 10));
		assertFalse(SkillAttackManager.canExecuteScheduledSkill(AIState.FIGHT, AISubState.CAST, target, target, 1, 1, 1, 1, 11, 10));
	}

	@Test
	void onlyCombatSkillCompletionContinuesTheAttackLoop() {
		RecordingNpcAI ai = new RecordingNpcAI();
		ai.setStateIfNot(AIState.IDLE);
		ai.setSubStateIfNot(AISubState.CAST);

		SkillAttackManager.afterUseSkill(ai);

		assertEquals(AISubState.NONE, ai.getSubState());
		assertEquals(0, ai.attackCompletions);

		ai.setStateIfNot(AIState.FIGHT);
		ai.setSubStateIfNot(AISubState.CAST);
		SkillAttackManager.afterUseSkill(ai);

		assertEquals(AISubState.NONE, ai.getSubState());
		assertEquals(1, ai.attackCompletions);
	}

	private static final class RecordingNpcAI extends GeneralNpcAI2 {

		private int attackCompletions;

		@Override
		protected void handleAttackComplete() {
			attackCompletions++;
		}
	}
}
