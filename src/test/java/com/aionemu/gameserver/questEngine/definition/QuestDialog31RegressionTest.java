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
		// 30565 的客户端 HTML 只有 select_none/select_success（item_order 自动接取任务），
		// 无 1011/2375 页——旧客户端形状的两行断言移除（start 批已按契约删除该对话入口）。
		// 30565's client HTML only has select_none/select_success (item_order auto-start);
		// the stale 1011/2375 page assertions were removed when the contract-driven start
		// cleanup dropped that dialog entry.
		assertDialog("80038.xml", "complete", "complete", 799780, 1011,
			new QuestCondition.HasItem(164002017, 5));
		assertDialog("80039.xml", "complete", "complete", 799780, 1011,
			new QuestCondition.HasItem(164002018, 1));
		assertDialog("1963.xml", "started", "started", 203851, 1352);
		assertDialog("1963.xml", "s1", "s1", 203726, 2375);
		assertDialog("1964.xml", "started", "started", 203776, 1352);
		assertDialog("1964.xml", "s1", "s1", 203726, 2375);
		assertDialog("1900.xml", "started", "started", 203739, 1352);
		assertDialog("1900.xml", "s1", "s1", 203766, 1693);
		assertDialog("1900.xml", "s2", "s2", 203797, 2034);
		assertDialog("1900.xml", "s3", "s3", 203795, 2375);
		assertDialog("11106.xml", "started", "started", 798978, 1352);
		assertDialog("11106.xml", "s1", "s1", 798979, 1693);
	}

	private static void assertDialog(String file, String source, String target, int npcId, int page,
			QuestCondition... conditions) throws Exception {
		CompiledQuestDefinition compiled = definition(file);
		assertTrue(compiled.definition().transitions().stream().anyMatch(transition ->
			source.equals(transition.sourceNode())
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
