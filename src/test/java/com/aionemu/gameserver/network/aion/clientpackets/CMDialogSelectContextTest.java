package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.gameserver.questEngine.definition.QuestDialogAction;
import com.aionemu.gameserver.questEngine.definition.QuestDialogPage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证通用任务选择页中的 NPC 对话请求不会被误当成任务页面动作。
 * Verifies that NPC selections from the generic quest-selection page are not mistaken for quest-page actions.
 */
class CMDialogSelectContextTest {
	@Test
	void identifiesSimpleNpcDialogFromTheGenericPage() {
		assertTrue(CM_DIALOG_SELECT.isSimpleNpcDialogSelection(900_007, QuestDialogAction.SELECT1_1.id(),
			QuestDialogPage.SELECT_QUEST.id(), 1111));
		assertTrue(CM_DIALOG_SELECT.isSimpleNpcDialogSelection(900_007, QuestDialogAction.SELECT1_1.id(),
			QuestDialogPage.SELECT_QUEST.id(), 0));
	}

	@Test
	void keepsRealQuestSelectionAndNonGenericContextsOnTheQuestIngress() {
		assertFalse(CM_DIALOG_SELECT.isSimpleNpcDialogSelection(900_007, QuestDialogAction.QUEST_SELECT.id(),
			QuestDialogPage.SELECT_QUEST.id(), 1111));
		assertFalse(CM_DIALOG_SELECT.isSimpleNpcDialogSelection(900_007, QuestDialogAction.QUEST_SELECT.id(),
			QuestDialogPage.SELECT_QUEST.id(), 0));
		assertFalse(CM_DIALOG_SELECT.isSimpleNpcDialogSelection(900_007, QuestDialogAction.SELECT1_1.id(),
			QuestDialogPage.SELECT1.id(), 1111));
		assertFalse(CM_DIALOG_SELECT.isSimpleNpcDialogSelection(0, QuestDialogAction.SELECT1_1.id(),
			QuestDialogPage.SELECT_QUEST.id(), 1111));
	}
}
