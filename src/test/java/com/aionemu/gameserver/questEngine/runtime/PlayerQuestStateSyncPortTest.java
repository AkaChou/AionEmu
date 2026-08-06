package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerQuestStateSyncPortTest {

	@Test
	void firstAcceptanceAndRepeatAcceptanceAddTheQuestToTheClientList() {
		assertTrue(PlayerQuestStateSyncPort.addsQuestToClientList(QuestStatus.NONE, QuestStatus.START));
		assertTrue(PlayerQuestStateSyncPort.addsQuestToClientList(QuestStatus.COMPLETE, QuestStatus.START));
	}

	@Test
	void activeQuestProgressUsesTheExistingQuestUpdateAction() {
		assertFalse(PlayerQuestStateSyncPort.addsQuestToClientList(QuestStatus.START, QuestStatus.START));
		assertFalse(PlayerQuestStateSyncPort.addsQuestToClientList(QuestStatus.START, QuestStatus.REWARD));
		assertFalse(PlayerQuestStateSyncPort.addsQuestToClientList(QuestStatus.REWARD, QuestStatus.COMPLETE));
	}
}
