package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 验证任务 25640 的客户端报告/领奖对话只在完成三十次击杀后开放，并保留满计数恢复路线。
 * Verifies quest 25640 opens the client report and reward dialogs only after thirty kills while keeping
 * the full-count recovery route.
 */
class Quest25640ClientDialogAlignmentTest {
	private static final int QUEST_NPC = 806101;
	private static final int KILLS_REQUIRED = 30;

	@Test
	void gatesTheClientReportPageOnCompletedKillProgress() {
		QuestDefinition definition = load().definition();

		List<QuestTransition> reportRoutes = definition.transitions().stream()
			.filter(transition -> "started".equals(transition.sourceNode()))
			.filter(transition -> "reward".equals(transition.targetNode()))
			.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == QUEST_NPC)
			.toList();
		assertEquals(Set.of(QuestDialogAction.QUEST_SELECT.id(),
			QuestDialogAction.SELECT_QUEST_REWARD.id()),
			reportRoutes.stream()
				.map(transition -> ((QuestEvent.TalkToNpc) transition.event()).dialogId())
				.collect(Collectors.toSet()),
			"25640 only opens REWARD through the two client report dialogs");
		for (QuestTransition route : reportRoutes) {
			assertEquals(List.of(new QuestCondition.VariableAtLeast("var1", KILLS_REQUIRED)),
				route.conditions(), "START report route must stay behind the full kill counter");
			assertEquals(List.of(new QuestAction.SetVariable("var0", 1)), route.actions());
		}
		assertEquals(KILLS_REQUIRED, definition.progressLayout().field("var1").maxValue(),
			"25640 kill counter ceiling");
		// 未满计数不可报告由 QuestPrematureRewardRouteExclusionTest 的 counter(25640, 30, ...) 逐计数证明。
		// Incomplete progress cannot report; proven per-count by the counter(25640, 30, ...) case there.

		// 反向断言：只有上述满计数路线才允许打开领奖窗口。
		// Negative side: only the full-count routes above may open the reward window.
		assertEquals(0, definition.transitions().stream()
			.filter(transition -> "started".equals(transition.sourceNode()))
			.filter(transition -> !reportRoutes.contains(transition))
			.filter(transition -> transition.afterCommit().stream()
				.anyMatch(action -> action instanceof AfterCommitAction.ShowQuestDialog show
					&& show.dialogId() == QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id()))
			.count(), "no ungated START route may open the reward window");

		QuestTransition report = route(definition, QuestDialogAction.QUEST_SELECT);
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.DEFAULT_SUCCESS.id())),
			report.afterCommit());

		QuestTransition preview = route(definition, QuestDialogAction.SELECT_QUEST_REWARD);
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())),
			preview.afterCommit());
	}

	private static QuestTransition route(QuestDefinition definition, QuestDialogAction action) {
		return definition.transitions().stream()
			.filter(transition -> "started".equals(transition.sourceNode()))
			.filter(transition -> transition.event().equals(new QuestEvent.TalkToNpc(QUEST_NPC, action.id())))
			.findFirst().orElseThrow();
	}

	private static CompiledQuestDefinition load() {
		String resource = "/aion/data/static_data/quest_definition/quests/25640.xml";
		try (InputStream input = Objects.requireNonNull(
			Quest25640ClientDialogAlignmentTest.class.getResourceAsStream(resource), resource)) {
			return QuestDefinitionXmlCompiler.compile(input);
		} catch (Exception e) {
			throw new AssertionError("unable to load " + resource, e);
		}
	}
}
