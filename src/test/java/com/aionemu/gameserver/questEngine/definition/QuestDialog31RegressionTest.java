package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestDialog31RegressionTest {
	@Test
	void migratedQuestHandlersKeepLegacyStartDialogRoutes() throws Exception {
		assertDialog("1131.xml", "shugo", "shugo", 203101, 2375);
		assertDialog("14010.xml", "started", "started", 203098, 1011);
		assertDialog("14020.xml", "started", "started", 203901, 1011);
		assertDialog("14040.xml", "started", "started", 278501, 10002);
		assertDialog("14050.xml", "started", "started", 204500, 10002);
		assertDialog("14014.xml", "s3", "s3", 802045, 2034,
			new QuestCondition.QuestVariableIs("var0", 3));
		assertDialog("21033.xml", "started", "started", 204734, 1352);
		assertDialog("21455.xml", "started", "started", 799240, 1352);
		assertDialog("24010.xml", "started", "started", 203557, 1011);
		assertDialog("24020.xml", "started", "started", 204301, 1011);
		assertDialog("24040.xml", "started", "started", 278001, 10002);
		assertDialog("24050.xml", "started", "started", 204702, 10002);
		assertDialog("26823.xml", "s2", "s2", 806289, 1694);
		assertDialog("30565.xml", "started", "started", 804879, 1011);
		assertDialog("30565.xml", "s1", "s1", 804879, 2375);
		assertDialog("80038.xml", "complete", "complete", 799780, 1011,
			new QuestCondition.HasItem(164002017, 5));
		assertDialog("80039.xml", "complete", "complete", 799780, 1011,
			new QuestCondition.HasItem(164002018, 1));
		assertDialog("1963.xml", "started", "started", 203851, 1352);
		assertDialog("1963.xml", "s1", "s1", 203726, 2375);
	}

	private static void assertDialog(String file, String source, String target, int npcId, int page,
			QuestCondition... conditions) throws Exception {
		CompiledQuestDefinition compiled = definition(file);
		assertTrue(compiled.definition().transitions().stream().anyMatch(transition ->
			transition.sourceNode().equals(source)
				&& transition.targetNode().equals(target)
				&& transition.event().equals(new QuestEvent.TalkToNpc(npcId, 31))
				&& transition.conditions().containsAll(List.of(conditions))
				&& transition.afterCommit().contains(new AfterCommitAction.ShowQuestDialog(page))),
			"missing dialog 31 route: " + file + " " + source + " npc=" + npcId + " page=" + page);
	}

	private static CompiledQuestDefinition definition(String file) throws Exception {
		try (InputStream input = QuestDialog31RegressionTest.class.getResourceAsStream(
			"/aion/data/static_data/quest_definition/quests/" + file)) {
			if (input == null) {
				throw new IllegalStateException("missing resource " + file);
			}
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
