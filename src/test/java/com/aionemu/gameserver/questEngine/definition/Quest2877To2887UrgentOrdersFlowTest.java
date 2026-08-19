package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 验证双阵营任务 1877–1887 与 2877–2887 的区域自动接取、军衔击杀链和奖励报告合同。
 * Verifies the zone auto-start, ranked-kill chain, and reward report contracts for faction-paired quests 1877–1887
 * and 2877–2887.
 */
class Quest2877To2887UrgentOrdersFlowTest {
	private static final String ELYOS_START_ZONE = "TEMINON_LANDING_400010000";
	private static final String ASMODIAN_START_ZONE = "PRIMUM_LANDING_400010000";
	private static final List<QuestContract> CONTRACTS = List.of(
		new QuestContract(1877, ELYOS_START_ZONE, Set.of(1876), 1, 278503, 3166358, 162000027, 10),
		new QuestContract(1878, ELYOS_START_ZONE, Set.of(1876, 1877), 2, 278502, 3166358, 162000050, 10),
		new QuestContract(1879, ELYOS_START_ZONE, Set.of(1876, 1878), 3, 278503, 3166358, 164000079, 10),
		new QuestContract(1880, ELYOS_START_ZONE, Set.of(1876, 1879), 4, 278502, 3166358, 169000010, 100),
		new QuestContract(1881, ELYOS_START_ZONE, Set.of(1876, 1880), 5, 278503, 3166358, 160001274, 10),
		new QuestContract(1882, ELYOS_START_ZONE, Set.of(1876, 1881), 6, 278502, 3166358, 164000095, 5),
		new QuestContract(1883, ELYOS_START_ZONE, Set.of(1876, 1882), 7, 278503, 3166358, 161000003, 5),
		new QuestContract(1884, ELYOS_START_ZONE, Set.of(1876, 1883), 8, 278502, 3166358, 188054203, 5),
		new QuestContract(1885, ELYOS_START_ZONE, Set.of(1876, 1884), 9, 278501, 3166358, 188100335, 500),
		new QuestContract(1886, ELYOS_START_ZONE, Set.of(1876, 1885), 10, 278501, 3518176, 188055829, 1),
		new QuestContract(1887, ELYOS_START_ZONE, Set.of(1876, 1886), 15, 278501, 3518176, 188055830, 1),
		new QuestContract(2877, ASMODIAN_START_ZONE, Set.of(2876), 1, 278016, 3166358, 162000027, 10),
		new QuestContract(2878, ASMODIAN_START_ZONE, Set.of(2876, 2877), 2, 278017, 3166358, 162000050, 10),
		new QuestContract(2879, ASMODIAN_START_ZONE, Set.of(2876, 2878), 3, 278016, 3166358, 164000079, 10),
		new QuestContract(2880, ASMODIAN_START_ZONE, Set.of(2876, 2879), 4, 278017, 3166358, 169000010, 100),
		new QuestContract(2881, ASMODIAN_START_ZONE, Set.of(2876, 2880), 5, 278016, 3166358, 160002274, 10),
		new QuestContract(2882, ASMODIAN_START_ZONE, Set.of(2876, 2881), 6, 278017, 3166358, 164000095, 5),
		new QuestContract(2883, ASMODIAN_START_ZONE, Set.of(2876, 2882), 7, 278016, 3166358, 161000003, 5),
		new QuestContract(2884, ASMODIAN_START_ZONE, Set.of(2876, 2883), 8, 278017, 3166358, 188054203, 5),
		new QuestContract(2885, ASMODIAN_START_ZONE, Set.of(2876, 2884), 9, 278001, 3166358, 188100335, 500),
		new QuestContract(2886, ASMODIAN_START_ZONE, Set.of(2876, 2885), 10, 278001, 3518176, 188055829, 1),
		new QuestContract(2887, ASMODIAN_START_ZONE, Set.of(2876, 2886), 15, 278001, 3518176, 188055830, 1));

	@TestFactory
	Stream<DynamicTest> matchesLegacyZoneStartKillAndRewardContracts() {
		return CONTRACTS.stream().map(contract -> DynamicTest.dynamicTest("quest " + contract.questId(),
			() -> assertContract(contract)));
	}

	private static void assertContract(QuestContract contract) throws Exception {
		QuestDefinition definition = definition(contract.questId());
		assertEquals(contract.prerequisites(), definition.metadata().prerequisites());
		assertEquals(List.of(), definition.metadata().startConditionGroups());
		assertEquals(List.of(
			new QuestReward("EXP", 0, contract.exp()),
			new QuestReward("ITEM", contract.rewardItemId(), contract.rewardItemCount())),
			definition.metadata().rewards());
		assertNodes(definition);

		assertEquals(1L, definition.transitions().stream()
			.filter(candidate -> candidate.sourceNode().equals("unaccepted"))
			.count());
		QuestTransition start = transition(definition, "unaccepted", "started",
			new QuestEvent.EnterZone(contract.startZone()));
		assertEquals(List.of(new QuestCondition.StartEligible()), start.conditions());
		assertEquals(List.of(), start.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH)),
			start.afterCommit());
		assertNull(start.priority());
		assertFalse(definition.transitions().stream()
			.anyMatch(candidate -> candidate.sourceNode().equals("unaccepted")
				&& candidate.event() instanceof QuestEvent.TalkToNpc));

		assertKillStep(definition, "started", "k1", contract.rankId(), QuestStateSyncMode.PACKET_ONLY);
		assertKillStep(definition, "k1", "k2", contract.rankId(), QuestStateSyncMode.PACKET_ONLY);
		assertKillStep(definition, "k2", "reward", contract.rankId(),
			QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH);

		QuestTransition report = talk(definition, "reward", "reward", contract.rewardNpcId(),
			QuestDialogAction.QUEST_SELECT.id());
		assertEmptyRoute(report);
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.DEFAULT_SUCCESS.id())),
			report.afterCommit());

		for (QuestDialogAction action : List.of(QuestDialogAction.USE_OBJECT,
				QuestDialogAction.SELECT_QUEST_REWARD)) {
			QuestTransition preview = talk(definition, "reward", "reward", contract.rewardNpcId(), action.id());
			assertEmptyRoute(preview);
			assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(
				QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())), preview.afterCommit());
		}

		List<QuestAction> completionActions = List.of(
			new QuestAction.GrantReward("EXP", 0, contract.exp(), QuestRewardAmountMode.QUEST_BASE),
			new QuestAction.GrantReward("ITEM", contract.rewardItemId(), contract.rewardItemCount(),
				QuestRewardAmountMode.EXACT),
			new QuestAction.CompleteQuest(0));
		List<AfterCommitAction> completionAfterCommit = List.of(
			new AfterCommitAction.RefreshPlayerStats(),
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION),
			new AfterCommitAction.ShowQuestSelectionDialog(QuestDialogPage.SELECT_QUEST.id()));
		for (int action = QuestDialogAction.SELECTED_QUEST_REWARD1.id();
				action <= QuestDialogAction.SELECTED_QUEST_NOREWARD.id(); action++) {
			QuestTransition completion = talk(definition, "reward", "complete", contract.rewardNpcId(), action);
			assertEquals(List.of(), completion.conditions());
			assertEquals(completionActions, completion.actions());
			assertEquals(completionAfterCommit, completion.afterCommit());
			assertNull(completion.priority());
		}

		QuestTransition reportedCompletion = transition(definition, "reward", "complete",
			new QuestEvent.QuestDialog(QuestDialogAction.SELECTED_QUEST_AUTO_REWARD.id()));
		assertEquals(List.of(), reportedCompletion.conditions());
		assertEquals(completionActions, reportedCompletion.actions());
		assertEquals(List.of(
			new AfterCommitAction.RefreshPlayerStats(),
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION),
			new AfterCommitAction.CloseDialog()), reportedCompletion.afterCommit());
		assertNull(reportedCompletion.priority());
		assertEquals(24, definition.transitions().size());
	}

	private static void assertNodes(QuestDefinition definition) {
		assertEquals(6, definition.nodes().size());
		assertNode(definition, "unaccepted", QuestStatus.NONE, 0);
		assertNode(definition, "started", QuestStatus.START, 0);
		assertNode(definition, "k1", QuestStatus.START, 1);
		assertNode(definition, "k2", QuestStatus.START, 2);
		assertNode(definition, "reward", QuestStatus.REWARD, 3);
		assertNode(definition, "complete", QuestStatus.COMPLETE, 0);
	}

	private static void assertNode(QuestDefinition definition, String label, QuestStatus status, int var0) {
		QuestNode node = definition.nodes().stream()
			.filter(candidate -> candidate.label().equals(label))
			.findFirst().orElseThrow();
		assertEquals(status, node.projection().status());
		assertEquals(Map.of("var0", var0), node.projection().variables());
	}

	private static void assertKillStep(QuestDefinition definition, String source, String target, int rankId,
			QuestStateSyncMode syncMode) {
		QuestTransition kill = transition(definition, source, target, new QuestEvent.KillRanked(rankId));
		assertEmptyRoute(kill);
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(syncMode)), kill.afterCommit());
	}

	private static void assertEmptyRoute(QuestTransition transition) {
		assertEquals(List.of(), transition.conditions());
		assertEquals(List.of(), transition.actions());
		assertNull(transition.priority());
	}

	private static QuestTransition talk(QuestDefinition definition, String source, String target, int npcId,
			int action) {
		return transition(definition, source, target, new QuestEvent.TalkToNpc(npcId, action));
	}

	private static QuestTransition transition(QuestDefinition definition, String source, String target,
			QuestEvent event) {
		List<QuestTransition> matches = definition.transitions().stream()
			.filter(candidate -> candidate.sourceNode().equals(source)
				&& candidate.targetNode().equals(target)
				&& candidate.event().equals(event))
			.toList();
		assertEquals(1, matches.size(), () -> source + " -> " + target + " " + event);
		return matches.getFirst();
	}

	private static QuestDefinition definition(int questId) throws Exception {
		try (InputStream input = Quest2877To2887UrgentOrdersFlowTest.class.getResourceAsStream(
				"/aion/data/static_data/quest_definition/quests/" + questId + ".xml")) {
			if (input == null) {
				throw new IllegalStateException("missing quest definition " + questId + ".xml");
			}
			return QuestDefinitionXmlCompiler.compile(input).definition();
		}
	}

	/**
	 * 保存每个连续紧急军令任务的权威差异字段。
	 * Holds the authoritative fields that differ across the urgent-order quest chain.
	 */
	private record QuestContract(int questId, String startZone, Set<Integer> prerequisites, int rankId, int rewardNpcId,
			long exp, int rewardItemId, int rewardItemCount) {
	}
}
