package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlan;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlanner;
import com.aionemu.gameserver.questEngine.runtime.QuestSnapshot;
import com.aionemu.gameserver.questEngine.runtime.QuestStartEligibility;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 锁定 39707 的阵营接取、十次击杀、双 NPC 报告和生命周期顺序。
 * Locks quest 39707's faction acquisition, ten kills, dual-NPC turn-in, and lifecycle ordering.
 */
class Quest39707RetailFlowAlignmentTest {
	private static final int QUEST_ID = 39707;
	private static final int NPC_FACTION_ID = 17;
	private static final int TARGET_NPC_ID = 230318;
	private static final Set<Integer> TURN_IN_NPC_IDS = Set.of(800931, 800932);

	@Test
	void preservesFactionMetadataAndKeepsTurnInNpcsOutOfAcquisition() throws Exception {
		QuestDefinition definition = load().definition();

		assertEquals(NPC_FACTION_ID, definition.metadata().npcFactionId());
		assertEquals(Set.of("ALL"), definition.metadata().repeatCycles());
		assertEquals(new NodeProjection(QuestStatus.NONE, Map.of("var0", 0)),
			node(definition, "unaccepted").projection());
		assertEquals(new NodeProjection(QuestStatus.START, Map.of()), node(definition, "started").projection());
		assertEquals(new NodeProjection(QuestStatus.REWARD, Map.of("var0", 10)),
			node(definition, "reward").projection());

		Set<Integer> dialogNpcIds = definition.transitions().stream()
			.map(QuestTransition::event)
			.filter(QuestEvent.TalkToNpc.class::isInstance)
			.map(QuestEvent.TalkToNpc.class::cast)
			.map(QuestEvent.TalkToNpc::npcId)
			.collect(Collectors.toSet());
		assertEquals(TURN_IN_NPC_IDS, dialogNpcIds);
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
	void targetlessAcquisitionStartsTheNpcFactionOnlyAfterStateCommit() throws Exception {
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
	void exactlyTenKillsReachTheTurnInBoundaryWithoutEnteringReward() throws Exception {
		CompiledQuestDefinition definition = load();
		QuestEvent event = new QuestEvent.KillNpc(TARGET_NPC_ID);
		int packedVariables = definition.definition().progressLayout().pack(Map.of("var0", 0));

		for (int kill = 1; kill <= 10; kill++) {
			QuestSnapshot snapshot = new QuestSnapshot(7, QUEST_ID, QuestStatus.START, packedVariables, Map.of());
			List<QuestMutationPlan> plans = plans(definition, snapshot, event);
			assertEquals(1, plans.size(), "kill " + kill);
			QuestMutationPlan plan = plans.getFirst();
			assertEquals(QuestStatus.START, plan.nextStatus(), "kill " + kill);
			assertEquals(kill,
				definition.definition().progressLayout().unpack(plan.nextPackedVariables()).get("var0").intValue(),
				"kill " + kill);
			assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
				plan.afterCommit(), "kill " + kill);
			packedVariables = plan.nextPackedVariables();
		}

		QuestSnapshot completedHunt = new QuestSnapshot(7, QUEST_ID, QuestStatus.START, packedVariables, Map.of());
		assertTrue(plans(definition, completedHunt, event).isEmpty());
	}

	@Test
	void bothTurnInNpcsUseTheClientSelect2AndRewardPages() throws Exception {
		CompiledQuestDefinition definition = load();
		int completeCount = definition.definition().progressLayout().pack(Map.of("var0", 10));
		int incompleteCount = definition.definition().progressLayout().pack(Map.of("var0", 9));

		for (int npcId : TURN_IN_NPC_IDS) {
			QuestEvent select = new QuestEvent.TalkToNpc(npcId, QuestDialogAction.QUEST_SELECT.id());
			QuestTransition selectRoute = route(definition.definition(), "started", "started", select);
			assertEquals(List.of(new QuestCondition.VariableAtLeast("var0", 10)), selectRoute.conditions());
			assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT2.id())),
				selectRoute.afterCommit());
			assertTrue(QuestMutationPlanner.plan(definition,
				new QuestSnapshot(7, QUEST_ID, QuestStatus.START, completeCount, Map.of()),
				select, selectRoute).isPresent());
			assertFalse(QuestMutationPlanner.plan(definition,
				new QuestSnapshot(7, QUEST_ID, QuestStatus.START, incompleteCount, Map.of()),
				select, selectRoute).isPresent());

			QuestEvent report = new QuestEvent.TalkToNpc(npcId, QuestDialogAction.SELECT_QUEST_REWARD.id());
			QuestTransition reportRoute = route(definition.definition(), "started", "reward", report);
			QuestMutationPlan reportPlan = QuestMutationPlanner.plan(definition,
				new QuestSnapshot(7, QUEST_ID, QuestStatus.START, completeCount, Map.of()),
				report, reportRoute).orElseThrow();
			assertEquals(QuestStatus.REWARD, reportPlan.nextStatus());
			assertEquals(completeCount, reportPlan.nextPackedVariables());
			assertEquals(List.of(
				new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
				new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())),
				reportPlan.afterCommit());
		}
	}

	@Test
	void completionAwardsRetailRewardsAndCompletesFactionBeforeClientPackets() throws Exception {
		CompiledQuestDefinition definition = load();
		int packedVariables = definition.definition().progressLayout().pack(Map.of("var0", 10));

		for (int npcId : TURN_IN_NPC_IDS) {
			QuestEvent event = new QuestEvent.TalkToNpc(npcId,
				QuestDialogAction.SELECTED_QUEST_REWARD1.id());
			QuestTransition transition = route(definition.definition(), "reward", "complete", event);
			assertEquals(List.of(
				new QuestAction.GrantReward("EXP", 0, 2805963, QuestRewardAmountMode.QUEST_BASE),
				new QuestAction.GrantReward("ITEM", 186000234, 3, QuestRewardAmountMode.EXACT),
				new QuestAction.GrantReward("ITEM", 188052288, 1, QuestRewardAmountMode.EXACT),
				new QuestAction.CompleteQuest(0)), transition.actions());

			QuestMutationPlan plan = QuestMutationPlanner.plan(definition,
				new QuestSnapshot(7, QUEST_ID, QuestStatus.REWARD, packedVariables, Map.of()),
				event, transition).orElseThrow();
			assertEquals(QuestStatus.COMPLETE, plan.nextStatus());
			assertEquals(List.of(
				new AfterCommitAction.CompleteNpcFactionQuest(NPC_FACTION_ID),
				new AfterCommitAction.RefreshPlayerStats(),
				new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION),
				new AfterCommitAction.ShowQuestSelectionDialog(QuestDialogPage.SELECT_QUEST.id())),
				plan.afterCommit());
		}
	}

	private static List<QuestMutationPlan> plans(CompiledQuestDefinition definition, QuestSnapshot snapshot,
			QuestEvent event) {
		return definition.definition().transitions().stream()
			.filter(transition -> "started".equals(transition.sourceNode()))
			.filter(transition -> QuestEvent.matches(transition.event(), event))
			.flatMap(transition -> QuestMutationPlanner.plan(definition, snapshot, event, transition).stream())
			.toList();
	}

	private static QuestTransition route(QuestDefinition definition, String source, String target,
			QuestEvent event) {
		return definition.transitions().stream()
			.filter(transition -> source.equals(transition.sourceNode()))
			.filter(transition -> target.equals(transition.targetNode()))
			.filter(transition -> transition.event().equals(event))
			.findFirst().orElseThrow();
	}

	private static QuestNode node(QuestDefinition definition, String label) {
		return definition.nodes().stream()
			.filter(candidate -> label.equals(candidate.label()))
			.findFirst().orElseThrow();
	}

	private static CompiledQuestDefinition load() throws Exception {
		String resource = "/aion/data/static_data/quest_definition/quests/39707.xml";
		try (InputStream input = Quest39707RetailFlowAlignmentTest.class.getResourceAsStream(resource)) {
			return QuestDefinitionXmlCompiler.compile(Objects.requireNonNull(input, resource));
		}
	}
}
