package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证任务 28800 将客户端简易接取与报告、领奖路由限定在各自的正式 NPC owner。
 * Verifies quest 28800 confines its simple acceptance, report, and reward routes to their retail NPC owners.
 * 28800 的 started 状态额外暴露 204191 SELECT2 辅助页，保持不回归该辅助路径，仅锁定报告链归属。
 */
class Quest28800ClientDialogAlignmentTest {
	private static final int START_NPC = 798459;
	private static final int REPORT_NPC = 830532;

	@Test
	void keepsTheRetailSimpleStartReportAndRewardOwnersExclusive() {
		QuestDefinition definition = load().definition();

		assertTrue(definition.metadata().prerequisites().isEmpty());
		assertTrue(definition.metadata().startConditions().isEmpty());
		assertNode(definition, "unaccepted", QuestStatus.NONE, Map.of("var0", 0));
		assertNode(definition, "started", QuestStatus.START, Map.of("var0", 0));
		assertNode(definition, "reward", QuestStatus.REWARD, Map.of("var0", 0));
		assertNode(definition, "complete", QuestStatus.COMPLETE, Map.of("var0", 0));

		QuestTransition offer = route(definition, "unaccepted", START_NPC,
			QuestDialogAction.QUEST_SELECT);
		assertContract(offer, "unaccepted", List.of(), List.of(
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT1.id())));

		QuestTransition accept = route(definition, "unaccepted", START_NPC,
			QuestDialogAction.QUEST_ACCEPT_SIMPLE);
		assertEquals("started", accept.targetNode());
		assertEquals(List.of(new QuestCondition.StartEligible()), accept.conditions());
		assertEquals(List.of(), accept.actions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH),
			new AfterCommitAction.CloseDialog()), accept.afterCommit());
		assertNull(accept.priority());

		QuestTransition startedKeep = route(definition, "started", START_NPC,
			QuestDialogAction.QUEST_SELECT);
		assertContract(startedKeep, "started", List.of(), List.of(
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT2.id())));
		assertTrue(routes(definition, "reward", START_NPC).stream().noneMatch(t ->
			((QuestEvent.TalkToNpc) t.event()).dialogId() == QuestDialogAction.SELECT_QUEST_REWARD.id()));

		assertTrue(routes(definition, "unaccepted", REPORT_NPC).isEmpty());
		QuestTransition reportPage = route(definition, "started", REPORT_NPC,
			QuestDialogAction.QUEST_SELECT);
		assertContract(reportPage, "started", List.of(), List.of(
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT5.id())));

		QuestTransition report = route(definition, "started", REPORT_NPC,
			QuestDialogAction.SELECT_QUEST_REWARD);
		assertContract(report, "reward", List.of(), List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(
				QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())));

		for (QuestDialogAction previewAction : List.of(
			QuestDialogAction.USE_OBJECT, QuestDialogAction.SELECT_QUEST_REWARD)) {
			QuestTransition preview = route(definition, "reward", REPORT_NPC, previewAction);
			assertContract(preview, "reward", List.of(), List.of(
				new AfterCommitAction.ShowQuestDialog(
					QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())));
		}

		List<QuestTransition> completionRoutes = routes(definition, "reward", REPORT_NPC).stream()
			.filter(transition -> {
				Integer dialogId = ((QuestEvent.TalkToNpc) transition.event()).dialogId();
				return dialogId != null && dialogId >= QuestDialogAction.SELECTED_QUEST_REWARD1.id()
					&& dialogId <= QuestDialogAction.SELECTED_QUEST_NOREWARD.id();
			})
			.toList();
		assertEquals(16, completionRoutes.size());
		for (QuestTransition completion : completionRoutes) {
			assertContract(completion, "complete", List.of(
				new QuestAction.GrantReward("EXP", 0, 12951, QuestRewardAmountMode.QUEST_BASE),
				new QuestAction.CompleteQuest(0)), List.of(
				new AfterCommitAction.RefreshPlayerStats(),
				new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION),
				new AfterCommitAction.ShowQuestSelectionDialog(QuestDialogPage.SELECT_QUEST.id())));
		}

		assertNoRewardPathForNpc(definition, START_NPC);
	}

	private static void assertNoRewardPathForNpc(QuestDefinition definition, int npcId) {
		assertTrue(routes(definition, "started", npcId).stream().noneMatch(t ->
			t.targetNode().equals("reward")));
		assertTrue(routes(definition, "reward", npcId).stream().noneMatch(t ->
			t.targetNode().equals("complete")));
	}

	private static void assertContract(QuestTransition transition, String target,
			List<QuestAction> actions, List<AfterCommitAction> afterCommit) {
		assertEquals(target, transition.targetNode());
		assertEquals(List.of(), transition.conditions());
		assertEquals(actions, transition.actions());
		assertEquals(afterCommit, transition.afterCommit());
		assertNull(transition.priority());
	}

	private static QuestTransition route(QuestDefinition definition, String source, int npcId,
			QuestDialogAction action) {
		List<QuestTransition> matches = routes(definition, source, npcId).stream()
			.filter(transition -> transition.event().equals(
				new QuestEvent.TalkToNpc(npcId, action.id())))
			.toList();
		assertEquals(1, matches.size(), "quest 28800 " + source + " " + npcId + " " + action);
		return matches.getFirst();
	}

	private static List<QuestTransition> routes(QuestDefinition definition, String source, int npcId) {
		return definition.transitions().stream()
			.filter(transition -> transition.sourceNode().equals(source))
			.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == npcId)
			.toList();
	}

	private static void assertNode(QuestDefinition definition, String label, QuestStatus status,
			Map<String, Integer> variables) {
		QuestNode node = definition.nodes().stream()
			.filter(candidate -> candidate.label().equals(label))
			.findFirst().orElseThrow();
		assertEquals(status, node.projection().status());
		assertEquals(variables, node.projection().variables());
	}

	private static CompiledQuestDefinition load() {
		String resource = "/aion/data/static_data/quest_definition/quests/28800.xml";
		try (InputStream input = Objects.requireNonNull(
			Quest28800ClientDialogAlignmentTest.class.getResourceAsStream(resource), resource)) {
			return QuestDefinitionXmlCompiler.compile(input);
		} catch (Exception e) {
			throw new AssertionError("unable to load " + resource, e);
		}
	}
}
