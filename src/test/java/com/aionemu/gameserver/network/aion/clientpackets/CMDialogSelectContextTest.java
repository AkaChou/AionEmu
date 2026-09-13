package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.gameserver.questEngine.definition.QuestDialogAction;
import com.aionemu.gameserver.questEngine.definition.QuestDialogPage;
import com.aionemu.gameserver.questEngine.definition.QuestMetadata;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证通用任务选择页中的 NPC 对话请求不会被误当成任务页面动作。
 * Verifies that NPC selections from the generic quest-selection page are not mistaken for quest-page actions.
 */
class CMDialogSelectContextTest {
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
	}

	/**
	 * NPC 没有任务上下文时不能再绑定任务 owner，否则关闭“未满65级普通任务标记”后
	 * 会把任务页当作普通对话页下发，客户端触发 load fail。
	 * An NPC option without quest context must not bind a quest owner; otherwise, after the
	 * normal-quest marker is disabled, the server sends a quest page as a plain dialog page
	 * and the client reports load fail.
	 */
	@Test
	void treatsNpcSelectionsWithoutQuestContextAsPlainDialogs() {
		assertFalse(CM_DIALOG_SELECT.hasQuestDialogContext(true, 0));
		assertTrue(CM_DIALOG_SELECT.hasQuestDialogContext(true, 1370));
		assertTrue(CM_DIALOG_SELECT.hasQuestDialogContext(false, 0));
	}

	/**
	 * 第 10 页的非 31 动作即使客户端附带了候选任务 ID，也不能借用任务上下文。
	 * A non-31 action from page 10 cannot borrow quest context even when the client attached a candidate quest id.
	 */
	@Test
	void genericPageNonQuestActionsCannotBorrowQuestContext() {
		assertEquals(0, CM_DIALOG_SELECT.resolveRoutedQuestId(1370, 1370, true));
		assertEquals(1370, CM_DIALOG_SELECT.resolveRoutedQuestId(1370, 0, false));
		assertEquals(1370, CM_DIALOG_SELECT.resolveRoutedQuestId(0, 1370, false));
		assertEquals(0, CM_DIALOG_SELECT.resolveRoutedQuestId(0, 0, false));
	}

	private static QuestState state(QuestStatus status) {
		return new QuestState(1192, status, 0, 0, null, 0, null);
	}
}
