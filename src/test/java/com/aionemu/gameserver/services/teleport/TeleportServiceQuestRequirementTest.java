package com.aionemu.gameserver.services.teleport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.dataholders.TeleporterData;
import com.aionemu.gameserver.model.templates.teleport.TeleportLocation;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

import jakarta.xml.bind.JAXBContext;

class TeleportServiceQuestRequirementTest {

	private static final Path TELEPORTER_XML = Path.of("src/main/resources/aion/data/static_data/npc_teleporter.xml");

	@Test
	void completedQuestAlwaysMeetsRequirement() {
		assertTrue(TeleportService2.meetsQuestRequirement(questState(QuestStatus.COMPLETE, 0), 0));
	}

	@Test
	void configuredProgressStepAllowsActiveAndRewardQuest() {
		assertTrue(TeleportService2.meetsQuestRequirement(questState(QuestStatus.START, 4), 4));
		assertTrue(TeleportService2.meetsQuestRequirement(questState(QuestStatus.REWARD, 6), 4));
		assertFalse(TeleportService2.meetsQuestRequirement(questState(QuestStatus.START, 3), 4));
		assertFalse(TeleportService2.meetsQuestRequirement(questState(QuestStatus.START, 4), 0));
		assertFalse(TeleportService2.meetsQuestRequirement(null, 4));
	}

	@Test
	void capitalTeleportersAllowSanctuaryReturnFromInstanceRollbackStep() throws Exception {
		TeleporterData data = (TeleporterData) JAXBContext.newInstance(TeleporterData.class)
				.createUnmarshaller().unmarshal(TELEPORTER_XML.toFile());

		assertQuestGate(data, 203726, 444, 10520, 4);
		assertQuestGate(data, 204191, 438, 20520, 4);
		assertEquals(0, data.getTeleporterTemplateByNpcId(804561).getTeleLocIdData()
				.getTeleportLocation(444).getRequiredQuestStep());
	}

	private static void assertQuestGate(TeleporterData data, int npcId, int locationId, int questId, int questStep) {
		TeleportLocation location = data.getTeleporterTemplateByNpcId(npcId).getTeleLocIdData().getTeleportLocation(locationId);
		assertEquals(questId, location.getRequiredQuest());
		assertEquals(questStep, location.getRequiredQuestStep());
	}

	private static QuestState questState(QuestStatus status, int questVar) {
		return new QuestState(10520, status, questVar, 0, null, null, null);
	}
}
