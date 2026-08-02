package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlanner;
import com.aionemu.gameserver.questEngine.runtime.QuestSnapshot;
import com.aionemu.gameserver.questEngine.runtime.QuestStartEligibility;
import com.aionemu.gameserver.questEngine.runtime.QuestShadowRunner;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Full vertical proof for the current ReportTo owner of quest 1101. */
class ReportTo1101DefinitionTest {
	private static final String DEFINITION =
		"/aion/data/static_data/quest_definition/candidates/reportto-1101.xml";
	private static final String MANIFEST =
		"/aion/data/static_data/quest_definition/quest_definition_candidate_manifest.xml";

	@Test
	void packagedSingleOwnerManifestCompilesTheProductionCandidate() throws Exception {
		try (InputStream input = resource(MANIFEST)) {
			QuestCatalog catalog = QuestDefinitionCandidateManifest.compile(input, getClass().getClassLoader());
			assertEquals(List.of(1101), catalog.all().stream().map(CompiledQuestDefinition::id).toList());
		}
	}

	@Test
	void candidateCoversEveryHandleableReportToDialogPath() throws Exception {
		CompiledQuestDefinition compiled = definition();
		List<QuestTransition> transitions = compiled.definition().transitions();

		assertEquals(29, transitions.size());
		assertTrue(transitions.stream().allMatch(t -> t.event() instanceof QuestEvent.TalkToNpc talk
			&& talk.dialogId() != null));
		assertEquals(Set.of(31, 1007, 1002, 20000, 1003, 1004, 20001, 1008),
			dialogIds(transitions, "unaccepted", 203049));
		assertEquals(Set.of(1008), dialogIds(transitions, "started", 203049));
		assertEquals(Set.of(31, 1009), dialogIds(transitions, "started", 203057));
		Set<Integer> rewardDialogs = IntStream.rangeClosed(8, 23).boxed()
			.collect(Collectors.toCollection(java.util.LinkedHashSet::new));
		assertEquals(rewardDialogs,
			transitions.stream().filter(t -> t.sourceNode().equals("reward") && t.targetNode().equals("complete"))
				.map(t -> ((QuestEvent.TalkToNpc) t.event()).dialogId())
				.collect(Collectors.toCollection(java.util.LinkedHashSet::new)));
		assertEquals(Set.of(-1, 1009), transitions.stream()
			.filter(t -> t.sourceNode().equals("reward") && t.targetNode().equals("reward"))
			.map(t -> ((QuestEvent.TalkToNpc) t.event()).dialogId()).collect(Collectors.toSet()));
		Set<String> offlineRoutes = transitions.stream()
			.filter(t -> t.shadowCoverage() == QuestShadowCoverageRequirement.OFFLINE_ONLY)
			.map(ReportTo1101DefinitionTest::routeKey).collect(Collectors.toSet());
		Set<String> expectedOfflineRoutes = IntStream.rangeClosed(8, 22)
			.mapToObj(dialogId -> "reward:203057:" + dialogId).collect(Collectors.toSet());
		expectedOfflineRoutes.addAll(Set.of(
			"unaccepted:203049:1004",
			"unaccepted:203049:1008",
			"unaccepted:203049:20000",
			"unaccepted:203049:20001",
			"started:203049:1008",
			"reward:203057:1009"));
		assertEquals(expectedOfflineRoutes, offlineRoutes);
		assertEquals(21, transitions.stream()
			.filter(t -> t.shadowCoverage() == QuestShadowCoverageRequirement.OFFLINE_ONLY).count());
		assertEquals(8, transitions.stream()
			.filter(t -> t.shadowCoverage() == QuestShadowCoverageRequirement.PRODUCTION_REQUIRED).count());
		assertEquals(QuestShadowCoverageRequirement.PRODUCTION_REQUIRED,
			transition(compiled, "reward", 203057, 23).shadowCoverage(),
			"无选择奖励的完成响应仍须由真实生产 observation 证明");
		assertEquals(QuestShadowCoverageRequirement.PRODUCTION_REQUIRED,
			transition(compiled, "reward", 203057, -1).shadowCoverage(),
			"重新打开奖励界面的真实客户端响应必须保留在生产门禁");
		assertEquals(QuestShadowCoverageRequirement.PRODUCTION_REQUIRED,
			transition(compiled, "started", 203057, 1009).shadowCoverage(),
			"从进行中进入奖励状态的真实客户端响应必须保留在生产门禁");
		assertEquals(QuestShadowCoverageRequirement.PRODUCTION_REQUIRED,
			transition(compiled, "unaccepted", 203049, 1002).shadowCoverage(),
			"标准接取协议必须保留在生产门禁");
	}

	@Test
	void acceptanceFailsClosedWithoutEligibilityAndUsesTheCorrectProtocol() throws Exception {
		CompiledQuestDefinition compiled = definition();
		QuestTransition accept = transition(compiled, "unaccepted", 203049, 1002);
		QuestSnapshot unknown = new QuestSnapshot(7, 1101, QuestStatus.NONE, 0, Map.of());
		QuestSnapshot rejected = unknown.withStartEligibility(QuestStartEligibility.rejected("LEVEL"));
		QuestSnapshot allowed = unknown.withStartEligibility(QuestStartEligibility.allowed());

		assertTrue(QuestMutationPlanner.plan(compiled, unknown, accept).isEmpty());
		assertTrue(QuestMutationPlanner.plan(compiled, rejected, accept).isEmpty());
		assertTrue(QuestMutationPlanner.plan(compiled, allowed, accept).isPresent());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(1003)), accept.afterCommit());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH),
			new AfterCommitAction.CloseDialog()),
			transition(compiled, "unaccepted", 203049, 20000).afterCommit());
	}

	@Test
	void everyCompletionPathUsesTypedRewardsAndCompleteLifecycle() throws Exception {
		CompiledQuestDefinition compiled = definition();
		List<QuestAction> expected = List.of(
			new QuestAction.GrantReward("ITEM", 164002010, 20),
			new QuestAction.GrantReward("ITEM", 164002011, 20),
			new QuestAction.GrantReward("ITEM", 164002057, 20),
			new QuestAction.GrantReward("GOLD", 0, 120, QuestRewardAmountMode.QUEST_BASE),
			new QuestAction.GrantReward("EXP", 0, 130, QuestRewardAmountMode.QUEST_BASE),
			new QuestAction.CompleteQuest(0));
		List<AfterCommitAction> afterCommit = List.of(new AfterCommitAction.RefreshPlayerStats(),
			new AfterCommitAction.RefreshPlayerStats(),
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION),
			new AfterCommitAction.ShowQuestSelectionDialog(10));

		List<QuestTransition> completions = compiled.definition().transitions().stream()
			.filter(t -> t.targetNode().equals("complete")).toList();
		assertEquals(16, completions.size());
		for (QuestTransition completion : completions) {
			assertEquals(expected, completion.actions());
			assertEquals(afterCommit, completion.afterCommit());
		}
	}

	@Test
	void noFivePathFixtureCanBeMistakenForReplacementProof() throws Exception {
		CompiledQuestDefinition compiled = definition();
		assertFalse(compiled.definition().transitions().size() == 5);
		assertEquals(3, compiled.definition().metadata().rewards().stream()
			.filter(reward -> reward.kind().equals("ITEM")).count());
	}

	@Test
	void candidateOnlyDryRunBuildsAPlanForAllTwentyNinePaths() throws Exception {
		CompiledQuestDefinition compiled = definition();
		QuestShadowRunner runner = new QuestShadowRunner(new ImmutableQuestCatalog(List.of(compiled)));
		for (QuestTransition transition : compiled.definition().transitions()) {
			QuestEvent.TalkToNpc route = (QuestEvent.TalkToNpc) transition.event();
			QuestSnapshot snapshot = switch (transition.sourceNode()) {
				case "unaccepted" -> new QuestSnapshot(7, 1101, QuestStatus.NONE, 0, Map.of());
				case "started" -> new QuestSnapshot(7, 1101, QuestStatus.START, 0, Map.of());
				case "reward" -> new QuestSnapshot(7, 1101, QuestStatus.REWARD, 1, Map.of());
				default -> throw new AssertionError("unexpected source " + transition.sourceNode());
			};
			snapshot = snapshot.withStartEligibility(QuestStartEligibility.allowed());
			QuestEvent event = new QuestEvent.TalkToNpc(route.npcId(), route.dialogId(), 900007);

			QuestShadowRunner.QuestShadowResult result = runner.inspect(event, Map.of(1101, snapshot));

			assertTrue(result.hasCandidatePlan(), "no plan for " + transition.sourceNode() + ":" + route.dialogId());
		}
	}

	private static Set<Integer> dialogIds(List<QuestTransition> transitions, String source, int npcId) {
		return transitions.stream().filter(t -> t.sourceNode().equals(source))
			.map(t -> (QuestEvent.TalkToNpc) t.event()).filter(t -> t.npcId() == npcId)
			.map(QuestEvent.TalkToNpc::dialogId).collect(Collectors.toSet());
	}

	private static String routeKey(QuestTransition transition) {
		QuestEvent.TalkToNpc event = (QuestEvent.TalkToNpc) transition.event();
		return transition.sourceNode() + ":" + event.npcId() + ":" + event.dialogId();
	}

	private static QuestTransition transition(CompiledQuestDefinition compiled, String source, int npcId,
			int dialogId) {
		return compiled.definition().transitions().stream().filter(t -> t.sourceNode().equals(source))
			.filter(t -> t.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == npcId && talk.dialogId() == dialogId)
			.findFirst().orElseThrow();
	}

	private CompiledQuestDefinition definition() throws Exception {
		try (InputStream input = resource(DEFINITION)) {
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}

	private InputStream resource(String path) {
		InputStream input = getClass().getResourceAsStream(path);
		if (input == null) throw new IllegalStateException("missing resource " + path);
		return input;
	}
}
