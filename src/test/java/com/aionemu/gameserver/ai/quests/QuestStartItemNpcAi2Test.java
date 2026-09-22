package com.aionemu.gameserver.ai.quests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.questEngine.definition.QuestDialogAction;

class QuestStartItemNpcAi2Test {

	@Test
	void triesUseObjectBeforeFallingBackToTheStartDialog() {
		assertEquals(List.of(QuestDialogAction.USE_OBJECT.id()), QuestStartItemNpcAi2.dialogIdsFor(false));
		assertEquals(List.of(QuestDialogAction.USE_OBJECT.id(), QuestDialogAction.QUEST_SELECT.id()),
			QuestStartItemNpcAi2.dialogIdsFor(true));
	}
}
