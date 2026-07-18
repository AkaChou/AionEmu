package com.aionemu.gameserver.services;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.dataholders.QuestsData;
import com.aionemu.gameserver.model.DialogAction;
import com.aionemu.gameserver.model.templates.QuestTemplate;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

import jakarta.xml.bind.JAXBContext;

class QuestReportEligibilityTest {

	@Test
	void onlyRewardReadyReportableQuestsCanFinishWithoutNpc() throws Exception {
		QuestsData data = (QuestsData) JAXBContext.newInstance(QuestsData.class).createUnmarshaller()
			.unmarshal(new StringReader("<quests><quest id=\"1\" can_report=\"true\"/><quest id=\"2\"/></quests>"));
		QuestTemplate reportable = data.getQuestById(1);
		QuestTemplate regular = data.getQuestById(2);

		int autoReward = DialogAction.AUTO_REWARD.id();
		assertTrue(QuestService.canFinishReportedQuest(reportable, state(1, QuestStatus.REWARD), autoReward));
		assertFalse(QuestService.canFinishReportedQuest(reportable, state(1, QuestStatus.START), autoReward));
		assertFalse(QuestService.canFinishReportedQuest(regular, state(2, QuestStatus.REWARD), autoReward));
		assertFalse(QuestService.canFinishReportedQuest(null, state(1, QuestStatus.REWARD), autoReward));
		assertFalse(QuestService.canFinishReportedQuest(reportable, null, autoReward));
		assertFalse(QuestService.canFinishReportedQuest(reportable, state(1, QuestStatus.REWARD), 109));
	}

	@Test
	void mapsAutoRewardSelectionsToExistingRewardDialogs() {
		assertEquals(0, QuestService.reportedRewardDialogId(DialogAction.AUTO_REWARD.id()));
		assertEquals(8, QuestService.reportedRewardDialogId(DialogAction.QUEST_AUTO_REWARD_1.id()));
		assertEquals(22, QuestService.reportedRewardDialogId(DialogAction.QUEST_AUTO_REWARD_15.id()));
		assertEquals(-1, QuestService.reportedRewardDialogId(109));
		assertTrue(QuestService.isReportedRewardAction(DialogAction.AUTO_REWARD.id()));
		assertFalse(QuestService.isReportedRewardAction(109));
	}

	private static QuestState state(int questId, QuestStatus status) {
		return new QuestState(questId, status, 0, 0, null, 0, null);
	}
}
