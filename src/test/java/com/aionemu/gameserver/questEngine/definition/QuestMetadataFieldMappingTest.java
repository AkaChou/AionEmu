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

		// P8 重锚：typed 目录 = XML 保留行（实测 733；2026-10-03 b22e1e971 退役 7 件无原版/客户端登记的
		// 自造任务后由 740 收缩）；native 行（七族 ∨ DD 1467 行）的模板轴由
		// 原版 quest.xml 元数据（NativeQuestXmlTable / retailMetadataOf）承担，不在此目录内。
		// P8 re-anchor: the typed catalog holds only XML-retained rows (observed 733; shrank from 740 when
		// b22e1e971 retired 7 self-made quests with no retail/client registration); native rows'
		// template axis lives in the retail quest.xml metadata (NativeQuestXmlTable / retailMetadataOf).
		assertEquals(733, questsData.size(), "unexpected synthesized quest count");

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

		// P8 重锚：扫描域 = typed 目录的 XML 保留行（733）；native 行的模板轴
		// 由原版 quest.xml 元数据承担。以下下限全部按 XML 保留行人口重新冻结。
		// P8 re-anchor: the sweep domain is the typed catalog's XML-retained rows (733);
		// native rows' template axis lives in the retail quest.xml metadata.
		assertTrue(withRewards > 700, "expected > 700 quests with rewards, got " + withRewards);
		assertTrue(withDrops > 100, "expected > 100 quests with drops, got " + withDrops);
		assertTrue(withCollects > 100, "expected > 100 quests with collects, got " + withCollects);
		assertTrue(withWorkItems > 40, "expected > 40 quests with work items, got " + withWorkItems);
		assertTrue(withStartConditions >= 100, "expected >= 100 quests with start conditions, got " + withStartConditions);
	}
}
