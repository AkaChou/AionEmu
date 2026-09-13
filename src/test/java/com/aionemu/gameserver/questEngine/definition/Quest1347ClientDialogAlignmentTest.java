package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 锁定任务 1347 的报告 NPC（203966）、报告页（1352/SELECT2）与奖励归属合同。
 * Locks quest 1347's report NPC (203966), report page (1352/SELECT2) and reward owner contract.
 *
 * <p>legacy 合同（origin/history zz_retail_simple_quests.xml）写明：start_npc=203965、
 * end_npc=203966、report_open_action=31、report_page=SELECT2(1352)、report_source_status=START、
 * report_action=1009、report_target_status=REWARD、reward_page=5。客户端 QUEST_Q1347.html 同样只在
 * 203966 这条链上提供 1352 页（按钮 1009「报告结果。」）。此前 XML 把 NPC_REPORT 挂在 203965，
 * 导致在 203966 点击任务行（31）没有路由、客户端卡在任务列表页。</p>
 * <p>The legacy contract (origin/history zz_retail_simple_quests.xml) states start_npc=203965,
 * end_npc=203966, report_open_action=31, report_page=SELECT2(1352), report_source_status=START,
 * report_action=1009, report_target_status=REWARD, reward_page=5. The client html for 1347 only
 * provides page 1352 on that chain (button 1009). The XML used to hang NPC_REPORT on 203965, so the
 * quest-row click (31) at 203966 had no route and the client stayed on the quest-list page.</p>
 */
class Quest1347ClientDialogAlignmentTest {
	private static final Path QUEST_PATH = Path.of(
		"src/main/resources/aion/data/static_data/quest_definition/quests/1347.xml");
	private static final int START_NPC = 203965;
	private static final int REPORT_NPC = 203966;

	@Test
	void reportsAtTheLegacyEndNpcInsteadOfTheStartNpc() throws Exception {
		QuestDefinition definition = load().definition();

		QuestTransition startPage = singleTalkRoute(definition, "unaccepted", START_NPC,
			QuestDialogAction.QUEST_SELECT);
		assertEquals("unaccepted", startPage.targetNode());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT1.id())),
			startPage.afterCommit());
		assertTrue(talkRoutes(definition, "a7b3", START_NPC).isEmpty(),
			"the start NPC must not own the report chain");

		QuestTransition reportPage = singleTalkRoute(definition, "a7b3", REPORT_NPC,
			QuestDialogAction.QUEST_SELECT);
		assertEquals("a7b3", reportPage.targetNode());
		assertTrue(reportPage.actions().isEmpty());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT2.id())),
			reportPage.afterCommit());

		QuestTransition report = singleTalkRoute(definition, "a7b3", REPORT_NPC,
			QuestDialogAction.SELECT_QUEST_REWARD);
		assertEquals("reward", report.targetNode());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())),
			report.afterCommit());

		assertEquals(List.of(REPORT_NPC), completionNpcs(definition));
		assertEquals(QuestStatus.REWARD, node(definition, "reward").projection().status());
	}

	private static QuestTransition singleTalkRoute(QuestDefinition definition, String source, int npcId,
			QuestDialogAction action) {
		List<QuestTransition> routes = talkRoutes(definition, source, npcId, action);
		assertEquals(1, routes.size(), "quest 1347 " + source + " " + npcId + " " + action);
		return routes.getFirst();
	}

	private static List<QuestTransition> talkRoutes(QuestDefinition definition, String source, int npcId,
			QuestDialogAction action) {
		return definition.transitions().stream()
			.filter(transition -> source.equals(transition.sourceNode()))
			.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == npcId
				&& talk.dialogId() == action.id())
			.toList();
	}

	private static List<QuestTransition> talkRoutes(QuestDefinition definition, String source, int npcId) {
		return definition.transitions().stream()
			.filter(transition -> source.equals(transition.sourceNode()))
			.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == npcId)
			.toList();
	}

	private static List<Integer> completionNpcs(QuestDefinition definition) {
		return definition.transitions().stream()
			.filter(transition -> "reward".equals(transition.sourceNode())
				&& "complete".equals(transition.targetNode()))
			.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc)
			.map(transition -> ((QuestEvent.TalkToNpc) transition.event()).npcId())
			.distinct().toList();
	}

	private static QuestNode node(QuestDefinition definition, String label) {
		return definition.nodes().stream()
			.filter(candidate -> label.equals(candidate.label()))
			.findFirst().orElseThrow();
	}

	private static CompiledQuestDefinition load() throws Exception {
		try (InputStream input = Files.newInputStream(QUEST_PATH)) {
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
