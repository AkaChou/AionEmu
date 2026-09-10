package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.gameserver.questEngine.definition.QuestDialogAction;
import com.aionemu.gameserver.questEngine.definition.QuestDialogPage;
import com.aionemu.gameserver.questEngine.definition.QuestMetadata;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
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
	void identifiesNonQuestActionsFromTheGenericPageEvenWithoutAnNpcTarget() {
		assertTrue(CM_DIALOG_SELECT.isGenericQuestSelectionPage(QuestDialogAction.SELECT1_1.id(),
			QuestDialogPage.SELECT_QUEST.id()));
		assertTrue(CM_DIALOG_SELECT.isGenericQuestSelectionPage(QuestDialogAction.ASK_QUEST_ACCEPT.id(),
			QuestDialogPage.SELECT_QUEST.id()));
		assertTrue(CM_DIALOG_SELECT.isGenericQuestSelectionPage(QuestDialogAction.QUEST_ACCEPT_1.id(),
			QuestDialogPage.SELECT_QUEST.id()));
		assertTrue(CM_DIALOG_SELECT.isGenericQuestSelectionPage(QuestDialogAction.QUEST_ACCEPT_SIMPLE.id(),
			QuestDialogPage.SELECT_QUEST.id()));
	}

	@Test
	void onlyTheNpcQuestRowCanEstablishQuestContext() {
		assertTrue(CM_DIALOG_SELECT.isNpcQuestRowSelection(900_007, QuestDialogAction.QUEST_SELECT.id(),
			QuestDialogPage.SELECT_QUEST.id(), 1192));
		assertFalse(CM_DIALOG_SELECT.isNpcQuestRowSelection(900_007, QuestDialogAction.ASK_QUEST_ACCEPT.id(),
			QuestDialogPage.SELECT1.id(), 1192));
		assertFalse(CM_DIALOG_SELECT.isNpcQuestRowSelection(0, QuestDialogAction.QUEST_SELECT.id(),
			QuestDialogPage.SELECT_QUEST.id(), 1192));
	}

	@Test
	void treatsAllNonActiveNormalQuestStatesAsUnacceptedDialogCandidates() {
		QuestMetadata metadata = QuestMetadata.minimal("Verteron Reinforcements", 1102362, "QUEST");
		assertTrue(CM_DIALOG_SELECT.isNormalQuestOutsideActiveProgress(metadata, null));
		assertTrue(CM_DIALOG_SELECT.isNormalQuestOutsideActiveProgress(metadata, state(QuestStatus.NONE)));
		assertTrue(CM_DIALOG_SELECT.isNormalQuestOutsideActiveProgress(metadata, state(QuestStatus.LOCKED)));
		assertTrue(CM_DIALOG_SELECT.isNormalQuestOutsideActiveProgress(metadata, state(QuestStatus.COMPLETE)));
		assertFalse(CM_DIALOG_SELECT.isNormalQuestOutsideActiveProgress(metadata, state(QuestStatus.START)));
		assertFalse(CM_DIALOG_SELECT.isNormalQuestOutsideActiveProgress(metadata, state(QuestStatus.REWARD)));
		assertFalse(CM_DIALOG_SELECT.isNormalQuestOutsideActiveProgress(
			QuestMetadata.minimal("Faction", 1, "FACTION"), null));
	}

	@Test
	void keepsRealQuestSelectionAndNonGenericContextsOnTheQuestIngress() {
		assertFalse(CM_DIALOG_SELECT.isGenericQuestSelectionPage(QuestDialogAction.QUEST_SELECT.id(),
			QuestDialogPage.SELECT_QUEST.id()));
		assertFalse(CM_DIALOG_SELECT.isGenericQuestSelectionPage(QuestDialogAction.ASK_QUEST_ACCEPT.id(),
			QuestDialogPage.SELECT1.id()));
		assertFalse(CM_DIALOG_SELECT.isSimpleNpcDialogSelection(900_007, QuestDialogAction.QUEST_SELECT.id(),
			QuestDialogPage.SELECT_QUEST.id(), 1111));
		assertFalse(CM_DIALOG_SELECT.isSimpleNpcDialogSelection(900_007, QuestDialogAction.QUEST_SELECT.id(),
			QuestDialogPage.SELECT_QUEST.id(), 0));
		assertFalse(CM_DIALOG_SELECT.isSimpleNpcDialogSelection(900_007, QuestDialogAction.SELECT1_1.id(),
			QuestDialogPage.SELECT1.id(), 1111));
		assertFalse(CM_DIALOG_SELECT.isSimpleNpcDialogSelection(0, QuestDialogAction.SELECT1_1.id(),
			QuestDialogPage.SELECT_QUEST.id(), 1111));
	}

	private static QuestState state(QuestStatus status) {
		return new QuestState(1192, status, 0, 0, null, 0, null);
	}
}
