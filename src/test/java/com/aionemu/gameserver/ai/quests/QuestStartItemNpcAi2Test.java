package com.aionemu.gameserver.ai.quests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.questEngine.model.QuestDialog;

class QuestStartItemNpcAi2Test {

	@Test
	void triesUseObjectBeforeFallingBackToTheStartDialog() {
		assertEquals(List.of(QuestDialog.USE_OBJECT.id()), QuestStartItemNpcAi2.dialogIdsFor(false));
		assertEquals(List.of(QuestDialog.USE_OBJECT.id(), QuestDialog.START_DIALOG.id()),
			QuestStartItemNpcAi2.dialogIdsFor(true));
	}
}
