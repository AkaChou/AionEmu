package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlan;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlanner;
import com.aionemu.gameserver.questEngine.runtime.QuestSnapshot;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 锁定任务 1346 的最终击杀报告 NPC 和奖励归属合同。
 * Locks quest 1346's final-kill report NPC and reward-owner contract.
 */
class Quest1346ClientDialogAlignmentTest {
	private static final Path QUEST_PATH = Path.of(
		"src/main/resources/aion/data/static_data/quest_definition/quests/1346.xml");
	private static final int START_NPC = 203966;
	private static final int REPORT_NPC = 203965;

	@Test
	void followsTheRetailReportOwnerFromTheFinalKillNode() throws Exception {
		CompiledQuestDefinition compiled = load();
		QuestDefinition definition = compiled.definition();

		assertEquals(new NodeProjection(QuestStatus.START, Map.of("var0", 9)), node(definition, "k9").projection());
		assertEquals(new NodeProjection(QuestStatus.REWARD, Map.of("var0", 9)), node(definition, "reward").projection());
		assertTrue(talkRoutes(definition, "unaccepted", REPORT_NPC).isEmpty());
		assertTrue(talkRoutes(definition, "started", REPORT_NPC).isEmpty());
		assertTrue(talkRoutes(definition, "started", START_NPC, QuestDialogAction.QUEST_SELECT).isEmpty());

		QuestTransition reportPage = singleTalkRoute(definition, "k9", REPORT_NPC,
			QuestDialogAction.QUEST_SELECT);
		assertEquals("k9", reportPage.targetNode());
		assertTrue(reportPage.conditions().isEmpty());
		assertTrue(reportPage.actions().isEmpty());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT2.id())),
			reportPage.afterCommit());

		QuestTransition report = singleTalkRoute(definition, "k9", REPORT_NPC,
			QuestDialogAction.SELECT_QUEST_REWARD);
		assertEquals("reward", report.targetNode());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())),
			report.afterCommit());

		QuestSnapshot snapshot = new QuestSnapshot(7, 1346, QuestStatus.START,
			definition.progressLayout().pack(Map.of("var0", 9)), Map.of());
		QuestMutationPlan reportPagePlan = QuestMutationPlanner.plan(compiled, snapshot,
			new QuestEvent.TalkToNpc(REPORT_NPC, QuestDialogAction.QUEST_SELECT.id()), reportPage).orElseThrow();
		assertEquals(QuestStatus.START, reportPagePlan.nextStatus());
		assertEquals(Map.of("var0", 9), definition.progressLayout().unpack(reportPagePlan.nextPackedVariables()));

		QuestMutationPlan reportPlan = QuestMutationPlanner.plan(compiled, snapshot,
			new QuestEvent.TalkToNpc(REPORT_NPC, QuestDialogAction.SELECT_QUEST_REWARD.id()), report).orElseThrow();
		assertEquals(QuestStatus.REWARD, reportPlan.nextStatus());
		assertEquals(Map.of("var0", 9), definition.progressLayout().unpack(reportPlan.nextPackedVariables()));

		assertTrue(talkRoutes(definition, "k9", START_NPC).isEmpty());
		assertTrue(talkRoutes(definition, "reward", START_NPC).isEmpty());
		assertEquals(List.of(REPORT_NPC), completionNpcs(definition));

		QuestTransition completion = singleTalkRoute(definition, "reward", REPORT_NPC,
			QuestDialogAction.SELECTED_QUEST_REWARD1);
		assertEquals("complete", completion.targetNode());
		assertEquals(List.of(
			new QuestAction.GrantReward("EXP", 0, 476911, QuestRewardAmountMode.QUEST_BASE),
			new QuestAction.GrantReward("ITEM", 186000003, 9, QuestRewardAmountMode.EXACT),
			new QuestAction.GrantReward("ITEM", 162000048, 9, QuestRewardAmountMode.EXACT),
			new QuestAction.CompleteQuest(0)), completion.actions());
		assertEquals(List.of(
			new AfterCommitAction.RefreshPlayerStats(),
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION),
			new AfterCommitAction.ShowQuestSelectionDialog(QuestDialogPage.SELECT_QUEST.id())),
			completion.afterCommit());
	}

	private static QuestTransition singleTalkRoute(QuestDefinition definition, String source, int npcId,
			QuestDialogAction action) {
		List<QuestTransition> routes = talkRoutes(definition, source, npcId, action);
		assertEquals(1, routes.size(), "quest 1346 " + source + " " + npcId + " " + action);
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
