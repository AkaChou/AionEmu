package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证任务 29001 将客户端简易接取与报告、领奖路由限定在各自的正式 NPC owner。
 * Verifies quest 29001 confines its simple acceptance, report, and reward routes to their retail NPC owners.
 */
class Quest29001ClientDialogAlignmentTest {
	private static final int START_NPC = 204096;
	private static final int REPORT_NPC = 204052;

	@Test
	void keepsTheRetailSimpleStartReportAndRewardOwnersExclusive() {
		QuestDefinition definition = load().definition();

		assertEquals(Set.of(29000), definition.metadata().startConditions().stream()
			.map(condition -> condition.questId()).collect(java.util.stream.Collectors.toSet()));
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
		assertEquals(List.of(new QuestAction.GiveItem(182207141, 1)), accept.actions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH),
			new AfterCommitAction.CloseDialog()), accept.afterCommit());
		assertNull(accept.priority());

		QuestTransition finish = route(definition, "started", START_NPC,
			QuestDialogAction.FINISH_DIALOG);
		assertContract(finish, "started", List.of(), List.of(
			new AfterCommitAction.ShowQuestSelectionDialog(QuestDialogPage.SELECT_QUEST.id())));
		assertNoRoute(definition, "started", START_NPC, QuestDialogAction.QUEST_SELECT);
		assertNoRoute(definition, "started", START_NPC, QuestDialogAction.SELECT_QUEST_REWARD);
		assertTrue(routes(definition, "reward", START_NPC).isEmpty());

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
				new QuestAction.GrantReward("EXP", 0, 1347585, QuestRewardAmountMode.QUEST_BASE),
				new QuestAction.GrantReward("ITEM", 125010014, 1, QuestRewardAmountMode.EXACT),
				new QuestAction.GrantReward("ITEM", 169405021, 30, QuestRewardAmountMode.EXACT),
				new QuestAction.CompleteQuest(0)), List.of(
				new AfterCommitAction.RefreshPlayerStats(),
				new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION),
				new AfterCommitAction.ShowQuestSelectionDialog(QuestDialogPage.SELECT_QUEST.id())));
		}
	}

	private static void assertContract(QuestTransition transition, String target,
			List<QuestAction> actions, List<AfterCommitAction> afterCommit) {
		assertEquals(target, transition.targetNode());
		assertEquals(List.of(), transition.conditions());
		assertEquals(actions, transition.actions());
		assertEquals(afterCommit, transition.afterCommit());
		assertNull(transition.priority());
	}

	private static void assertNoRoute(QuestDefinition definition, String source, int npcId,
			QuestDialogAction action) {
		assertTrue(routes(definition, source, npcId).stream()
			.noneMatch(transition -> transition.event().equals(
				new QuestEvent.TalkToNpc(npcId, action.id()))));
	}

	private static QuestTransition route(QuestDefinition definition, String source, int npcId,
			QuestDialogAction action) {
		List<QuestTransition> matches = routes(definition, source, npcId).stream()
			.filter(transition -> transition.event().equals(
				new QuestEvent.TalkToNpc(npcId, action.id())))
			.toList();
		assertEquals(1, matches.size(), "quest 29001 " + source + " " + npcId + " " + action);
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
		String resource = "/aion/data/static_data/quest_definition/quests/29001.xml";
		try (InputStream input = Objects.requireNonNull(
			Quest29001ClientDialogAlignmentTest.class.getResourceAsStream(resource), resource)) {
			return QuestDefinitionXmlCompiler.compile(input);
		} catch (Exception e) {
			throw new AssertionError("unable to load " + resource, e);
		}
	}
}
