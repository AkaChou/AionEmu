package com.aionemu.gameserver.ai;

import com.aionemu.gameserver.questEngine.definition.QuestDialogAction;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestItemNpcAI2Test {
	@Test
	void registeredDialogRoutesCanStartWithoutAPureActionGate() {
		assertTrue(QuestItemNpcAI2.canStartInteraction(true, false, false));
		assertTrue(QuestItemNpcAI2.canStartInteraction(false, true, false));
		assertTrue(QuestItemNpcAI2.canStartInteraction(false, false, true));
		assertFalse(QuestItemNpcAI2.canStartInteraction(false, false, false));
	}

	@Test
	void triesUseObjectBeforeFallingBackToTheStartDialog() {
		assertEquals(List.of(QuestDialogAction.USE_OBJECT.id(), QuestDialogAction.QUEST_SELECT.id()),
			QuestItemNpcAI2.dialogIds());
	}
}
