package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.e2e.client.ClientResourceOracle;
import com.aionemu.gameserver.questEngine.e2e.journey.QuestProductionJourneyExecutor;
import com.aionemu.gameserver.questEngine.e2e.journey.QuestProductionJourneyPlanner;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlan;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlanner;
import com.aionemu.gameserver.questEngine.runtime.QuestSnapshot;
import com.aionemu.gameserver.questEngine.runtime.QuestStartEligibility;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 锁定 49715 的阵营接取、十次击杀计数和零售报告/奖励合同。
 * Locks quest 49715's faction acquisition, ten-kill counter, and retail report/reward contract.
 */
class Quest49715RetailFlowAlignmentTest {
	private static final int QUEST_ID = 49715;
	private static final int NPC_FACTION_ID = 18;
	private static final Set<Integer> END_NPC_IDS = Set.of(800936, 800937, 800938);
	private static final Set<Integer> NON_RETAIL_NPC_IDS = Set.of(800934, 800935);
	private static final Set<Integer> TARGET_NPC_IDS = Set.of(231284, 231285, 231286, 231287, 231288, 231289);

	@Test
	void preservesFactionMetadataUnprojectedCounterAndRetailNpcOwners() throws Exception {
		QuestDefinition definition = load().definition();

		assertEquals(NPC_FACTION_ID, definition.metadata().npcFactionId());
		assertEquals(Set.of("ALL"), definition.metadata().repeatCycles());
		assertEquals(new NodeProjection(QuestStatus.NONE, Map.of("var0", 0)),
			node(definition, "unaccepted").projection());
		assertEquals(new NodeProjection(QuestStatus.START, Map.of()), node(definition, "started").projection());
		assertEquals(new NodeProjection(QuestStatus.REWARD, Map.of("var0", 10)),
			node(definition, "reward").projection());
		assertEquals(new NodeProjection(QuestStatus.COMPLETE, Map.of("var0", 10)),
			node(definition, "complete").projection());

		Set<Integer> dialogNpcIds = definition.transitions().stream()
			.map(QuestTransition::event)
			.filter(QuestEvent.TalkToNpc.class::isInstance)
			.map(QuestEvent.TalkToNpc.class::cast)
			.map(QuestEvent.TalkToNpc::npcId)
			.collect(Collectors.toSet());
		assertEquals(END_NPC_IDS, dialogNpcIds);
		assertTrue(dialogNpcIds.stream().noneMatch(NON_RETAIL_NPC_IDS::contains));
		assertTrue(definition.transitions().stream()
			.filter(transition -> "unaccepted".equals(transition.sourceNode()))
			.noneMatch(transition -> transition.event() instanceof QuestEvent.TalkToNpc));

		Set<Integer> acquisitionActions = definition.transitions().stream()
			.filter(transition -> "unaccepted".equals(transition.sourceNode()))
			.filter(transition -> "started".equals(transition.targetNode()))
			.map(QuestTransition::event)
			.filter(QuestEvent.QuestDialog.class::isInstance)
			.map(QuestEvent.QuestDialog.class::cast)
			.map(QuestEvent.QuestDialog::dialogId)
			.collect(Collectors.toSet());
		assertEquals(Set.of(QuestDialogAction.QUEST_ACCEPT_1.id(), QuestDialogAction.QUEST_ACCEPT_SIMPLE.id()),
			acquisitionActions);
	}

	@Test
	void targetlessAcquisitionStartsNpcFactionAfterStateCommit() throws Exception {
		CompiledQuestDefinition definition = load();
		for (QuestDialogAction action : List.of(
			QuestDialogAction.QUEST_ACCEPT_1, QuestDialogAction.QUEST_ACCEPT_SIMPLE)) {
			QuestEvent event = new QuestEvent.QuestDialog(action.id());
			QuestTransition transition = route(definition.definition(), "unaccepted", "started", event);
			assertEquals(List.of(new QuestCondition.StartEligible()), transition.conditions());
			assertEquals(List.of(), transition.actions());
			assertEquals(List.of(
				new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH),
				new AfterCommitAction.CloseDialog()), transition.afterCommit());

			QuestMutationPlan plan = QuestMutationPlanner.plan(definition,
				new QuestSnapshot(7, QUEST_ID, QuestStatus.NONE, 0, Map.of())
					.withStartEligibility(QuestStartEligibility.allowed()),
				event, transition).orElseThrow();
			assertEquals(QuestStatus.START, plan.nextStatus());
			assertEquals(0, plan.nextPackedVariables());
			assertEquals(List.of(
				new AfterCommitAction.StartNpcFactionQuest(NPC_FACTION_ID),
				new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH),
				new AfterCommitAction.CloseDialog()), plan.afterCommit());
		}
	}

	@Test
	void allLuluTargetsReachTenInStartAndAnyTargetCanBeTheFinalKill() throws Exception {
		CompiledQuestDefinition definition = load();
		assertFalse(definition.definition().transitions().stream()
			.filter(transition -> transition.event() instanceof QuestEvent.KillNpc)
			.anyMatch(transition -> "reward".equals(transition.targetNode())));

		int repeatedTargetId = TARGET_NPC_IDS.iterator().next();
		for (int finalTargetId : TARGET_NPC_IDS) {
			int packedVariables = packed(definition, 0);
			for (int killNumber = 1; killNumber <= 10; killNumber++) {
				int targetId = killNumber == 10 ? finalTargetId : repeatedTargetId;
				QuestEvent event = new QuestEvent.KillNpc(targetId);
				QuestTransition transition = route(definition.definition(), "started", "started", event);
				assertEquals(Integer.valueOf(1), transition.priority());
				assertEquals(List.of(new QuestCondition.VariableBelow("var0", 10)), transition.conditions());
				assertEquals(List.of(new QuestAction.IncrementVariable("var0", 1)), transition.actions());
				assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
					transition.afterCommit());

				QuestMutationPlan plan = QuestMutationPlanner.plan(definition,
					new QuestSnapshot(7, QUEST_ID, QuestStatus.START, packedVariables, Map.of()), event, transition)
					.orElseThrow();
				assertEquals(QuestStatus.START, plan.nextStatus());
				assertEquals(packed(definition, killNumber), plan.nextPackedVariables());
				packedVariables = plan.nextPackedVariables();
			}
		}
	}

	@Test
	void reportUsesTheClientSelect2RewardChainAndCompletionClosesFactionLifecycle() throws Exception {
		CompiledQuestDefinition definition = load();
		for (int npcId : END_NPC_IDS) {
			QuestEvent select = new QuestEvent.TalkToNpc(npcId, QuestDialogAction.QUEST_SELECT.id());
			QuestTransition selectRoute = route(definition.definition(), "started", "started", select);
			assertEquals(List.of(new QuestCondition.VariableAtLeast("var0", 10)), selectRoute.conditions());
			assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT2.id())),
				selectRoute.afterCommit());

			QuestEvent report = new QuestEvent.TalkToNpc(npcId, QuestDialogAction.SELECT_QUEST_REWARD.id());
			QuestTransition reportRoute = route(definition.definition(), "started", "reward", report);
			assertEquals(List.of(new QuestCondition.VariableAtLeast("var0", 10)), reportRoute.conditions());
			assertEquals(List.of(), reportRoute.actions());
			assertEquals(List.of(
				new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
				new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())),
				reportRoute.afterCommit());

			QuestMutationPlan reportPlan = QuestMutationPlanner.plan(definition,
				new QuestSnapshot(7, QUEST_ID, QuestStatus.START, packed(definition, 10), Map.of()), report,
				reportRoute).orElseThrow();
			assertEquals(QuestStatus.REWARD, reportPlan.nextStatus());
			assertEquals(packed(definition, 10), reportPlan.nextPackedVariables());

			QuestTransition preview = route(definition.definition(), "reward", "reward",
				new QuestEvent.TalkToNpc(npcId, QuestDialogAction.USE_OBJECT.id()));
			assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(
				QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())), preview.afterCommit());

			QuestEvent complete = new QuestEvent.TalkToNpc(npcId, QuestDialogAction.SELECTED_QUEST_REWARD1.id());
			QuestTransition completion = route(definition.definition(), "reward", "complete", complete);
			assertEquals(List.of(
				new QuestAction.GrantReward("EXP", 0, 3251515, QuestRewardAmountMode.QUEST_BASE),
				new QuestAction.GrantReward("ITEM", 186000234, 3, QuestRewardAmountMode.EXACT),
				new QuestAction.GrantReward("ITEM", 188052288, 1, QuestRewardAmountMode.EXACT),
				new QuestAction.CompleteQuest(0)), completion.actions());

			QuestMutationPlan completionPlan = QuestMutationPlanner.plan(definition,
				new QuestSnapshot(7, QUEST_ID, QuestStatus.REWARD, packed(definition, 10), Map.of()), complete,
				completion).orElseThrow();
			assertEquals(QuestStatus.COMPLETE, completionPlan.nextStatus());
			assertEquals(packed(definition, 10), completionPlan.nextPackedVariables());
			assertEquals(List.of(
				new AfterCommitAction.CompleteNpcFactionQuest(NPC_FACTION_ID),
				new AfterCommitAction.RefreshPlayerStats(),
				new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION),
				new AfterCommitAction.ShowQuestSelectionDialog(QuestDialogPage.SELECT_QUEST.id())),
				completionPlan.afterCommit());
		}
	}

	@Test
	void completesThroughTheProductionHeadlessJourney() throws Exception {
		CompiledQuestDefinition definition = load();
		ClientResourceOracle oracle = ClientResourceOracle.load(Path.of("docs/quest/client-dialog-mapping"));
		QuestProductionJourneyPlanner.Result planned = new QuestProductionJourneyPlanner().plan(definition, oracle);
		assertTrue(planned.planned(), () -> String.valueOf(planned.failure()));

		QuestProductionJourneyExecutor.Result executed = new QuestProductionJourneyExecutor()
			.execute(definition, oracle, planned.plan());
		assertTrue(executed.completed(), () -> String.valueOf(executed.failure()));
	}

	private static int packed(CompiledQuestDefinition definition, int value) {
		return definition.definition().progressLayout().pack(Map.of("var0", value));
	}

	private static QuestTransition route(QuestDefinition definition, String source, String target,
		QuestEvent event) {
		List<QuestTransition> routes = definition.transitions().stream()
			.filter(transition -> source.equals(transition.sourceNode()))
			.filter(transition -> target.equals(transition.targetNode()))
			.filter(transition -> transition.event().equals(event))
			.toList();
		assertEquals(1, routes.size(), () -> source + " -> " + target + " " + event);
		return routes.getFirst();
	}

	private static QuestNode node(QuestDefinition definition, String label) {
		return definition.nodes().stream()
			.filter(candidate -> label.equals(candidate.label()))
			.findFirst().orElseThrow();
	}

	private static CompiledQuestDefinition load() throws Exception {
		String resource = "/aion/data/static_data/quest_definition/quests/49715.xml";
		try (InputStream input = Quest49715RetailFlowAlignmentTest.class.getResourceAsStream(resource)) {
			return QuestDefinitionXmlCompiler.compile(Objects.requireNonNull(input, resource));
		}
	}
}
