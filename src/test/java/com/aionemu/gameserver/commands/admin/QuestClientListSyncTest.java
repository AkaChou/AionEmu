package com.aionemu.gameserver.commands.admin;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 验证 GM 任务状态命令使用客户端可见性对应的插入或更新包语义。
 * Verifies that the GM quest-state command uses insert or update packet semantics matching client visibility.
 */
class QuestClientListSyncTest {

	@Test
	void insertsNewAndPreviouslyInvisibleQuestsIntoTheClientList() {
		assertTrue(Quest.addsQuestToClientList(null, QuestStatus.START));
		assertTrue(Quest.addsQuestToClientList(QuestStatus.NONE, QuestStatus.START));
		assertTrue(Quest.addsQuestToClientList(QuestStatus.COMPLETE, QuestStatus.REWARD));
	}

	@Test
	void updatesQuestsThatAreAlreadyVisible() {
		assertFalse(Quest.addsQuestToClientList(QuestStatus.START, QuestStatus.START));
		assertFalse(Quest.addsQuestToClientList(QuestStatus.START, QuestStatus.REWARD));
	}

	@Test
	void doesNotInsertInvisibleTerminalStates() {
		assertFalse(Quest.addsQuestToClientList(null, QuestStatus.NONE));
		assertFalse(Quest.addsQuestToClientList(QuestStatus.START, QuestStatus.COMPLETE));
	}
}
