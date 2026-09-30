package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.dataholders.QuestsData;
import com.aionemu.gameserver.model.templates.QuestTemplate;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestMetadataFieldMappingTest {

	@Test
	void questTemplatesSynthesizedFromProductionCatalogCoverEveryField() {
		QuestCatalog catalog = ProductionQuestDefinitions.catalog();
		QuestsData questsData = QuestsData.fromCatalog(catalog);

		assertEquals(6224, questsData.size(), "unexpected synthesized quest count");

		int withRewards = 0;
		int withDrops = 0;
		int withKills = 0;
		int withCollects = 0;
		int withWorkItems = 0;
		int withStartConditions = 0;

		for (QuestTemplate template : questsData.getQuestsData()) {
			assertNotNull(template.getName(), "missing name for quest " + template.getId());
			if (template.getRewards() != null && !template.getRewards().isEmpty()) {
				withRewards++;
			}
			if (template.getQuestDrop() != null && !template.getQuestDrop().isEmpty()) {
				withDrops++;
			}
			if (template.getQuestKill() != null && !template.getQuestKill().isEmpty()) {
				withKills++;
			}
			if (template.getCollectItems() != null && !template.getCollectItems().getCollectItem().isEmpty()) {
				withCollects++;
			}
			if (template.getQuestWorkItems() != null && !template.getQuestWorkItems().getQuestWorkItem().isEmpty()) {
				withWorkItems++;
			}
			if (template.getXMLStartConditions() != null && !template.getXMLStartConditions().isEmpty()) {
				withStartConditions++;
			}
		}

		assertTrue(withRewards > 5000, "expected > 5000 quests with rewards, got " + withRewards);
		assertTrue(withDrops > 500, "expected > 500 quests with drops, got " + withDrops);
		assertTrue(withCollects > 500, "expected > 500 quests with collects, got " + withCollects);
		assertTrue(withWorkItems > 200, "expected > 200 quests with work items, got " + withWorkItems);
		assertTrue(withStartConditions >= 500, "expected >= 500 quests with start conditions, got " + withStartConditions);
	}
}
