package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.model.PlayerClass;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 锁定任务 11031/11032 的旧客户端对话链与一次性任务物品生命周期。
 * Locks the legacy client dialog chain and consumable quest-item lifecycle for quests 11031 and 11032.
 */
class Quest11031And11032RetailFlowTest {
	private static final int PLAYER_ID = 7;
	private static final int NPC_ID = 798959;
	private static final int COLLECT_COUNT = 10;
	private static final int EXPERIENCE = 5980975;
	private static final int REWARD_ITEM_ID = 186000018;
	private static final int REWARD_ACTION_FIRST = QuestDialogAction.SELECTED_QUEST_REWARD1.id();
	private static final int REWARD_ACTION_LAST = QuestDialogAction.SELECTED_QUEST_NOREWARD.id();

	private record QuestCase(int questId, int collectItemId, int workItemId, int gold, int prerequisiteQuestId) {}

	private static final List<QuestCase> QUESTS = List.of(
		new QuestCase(11031, 182206723, 182206724, 75130, 0),
		new QuestCase(11032, 182206725, 182206726, 97130, 11031));

	@Test
	void restoresTheLegacyFourStepFlowAndClientPages() throws Exception {
		for (QuestCase quest : QUESTS) {
			QuestDefinition definition = load(quest.questId()).definition();
			assertEquals(quest.prerequisiteQuestId() == 0 ? Set.of() : Set.of(quest.prerequisiteQuestId()),
				definition.metadata().prerequisites());
			assertEquals(List.of(new QuestItemRequirement(quest.collectItemId(), COLLECT_COUNT)),
				definition.metadata().itemRequirements());
			assertEquals(List.of(new QuestItemRequirement(quest.workItemId(), 1)),
				definition.metadata().questWorkItems());

			assertNode(definition, "unaccepted", QuestStatus.NONE, 0);
			assertNode(definition, "started", QuestStatus.START, 0);
			assertNode(definition, "v1", QuestStatus.START, 1);
			assertNode(definition, "v2", QuestStatus.START, 2);
			assertNode(definition, "reward", QuestStatus.REWARD, 3);
			assertNode(definition, "complete", QuestStatus.COMPLETE, 3);

			assertPage(definition, "unaccepted", QuestDialogAction.QUEST_SELECT, QuestDialogPage.SELECT_NONE);
			assertPage(definition, "unaccepted", QuestDialogAction.ASK_QUEST_ACCEPT,
				QuestDialogPage.SHOW_ASK_QUEST_ACCEPT_WINDOW);
			QuestTransition accept = route(definition, "unaccepted",
				new QuestEvent.TalkToNpc(NPC_ID, QuestDialogAction.QUEST_ACCEPT_1.id()));
			assertEquals("started", accept.targetNode());
			assertEquals(List.of(new QuestCondition.StartEligible()), accept.conditions());
			assertEquals(List.of(), accept.actions());
			assertEquals(List.of(
				new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH),
				new AfterCommitAction.ShowQuestDialog(QuestDialogPage.QUEST_ACCEPT_1.id())), accept.afterCommit());
			QuestTransition refuseClose = route(definition, "unaccepted",
				new QuestEvent.TalkToNpc(NPC_ID, QuestDialogAction.FINISH_DIALOG.id()));
			assertEquals("unaccepted", refuseClose.targetNode());
			assertEquals(List.of(new AfterCommitAction.ShowQuestSelectionDialog(QuestDialogPage.SELECT_QUEST.id())),
				refuseClose.afterCommit());
			QuestTransition startedClose = route(definition, "started",
				new QuestEvent.TalkToNpc(NPC_ID, QuestDialogAction.FINISH_DIALOG.id()));
			assertEquals("started", startedClose.targetNode());
			assertEquals(List.of(new AfterCommitAction.ShowQuestSelectionDialog(QuestDialogPage.SELECT_QUEST.id())),
				startedClose.afterCommit());

			assertPage(definition, "started", QuestDialogAction.QUEST_SELECT, QuestDialogPage.SELECT1);
			QuestTransition collect = route(definition, "started",
				new QuestEvent.TalkToNpc(NPC_ID, QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM.id()), 0);
			assertEquals("v1", collect.targetNode());
			assertEquals(List.of(new QuestCondition.HasItem(quest.collectItemId(), COLLECT_COUNT)),
				collect.conditions());
			assertEquals(List.of(new QuestAction.RemoveItem(quest.collectItemId(), COLLECT_COUNT)), collect.actions());
			assertEquals(List.of(
				new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
				new AfterCommitAction.ShowQuestDialog(QuestDialogPage.CHECK_USER_ITEM_OK.id())), collect.afterCommit());

			QuestTransition collectFallback = route(definition, "started",
				new QuestEvent.TalkToNpc(NPC_ID, QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM.id()), 1);
			assertEquals("started", collectFallback.targetNode());
			assertEquals(List.of(), collectFallback.conditions());
			assertEquals(List.of(), collectFallback.actions());
			assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.CHECK_USER_ITEM_FAIL.id())),
				collectFallback.afterCommit());
			assertPage(definition, "v1", QuestDialogAction.QUEST_SELECT, QuestDialogPage.SELECT2);

			QuestTransition giveWorkItem = route(definition, "v1",
				new QuestEvent.TalkToNpc(NPC_ID, QuestDialogAction.SETPRO2.id()));
			assertEquals("v2", giveWorkItem.targetNode());
			assertEquals(List.of(new QuestCondition.QuestVariableIs("var0", 1)), giveWorkItem.conditions());
			assertEquals(List.of(new QuestAction.GiveItem(quest.workItemId(), 1)), giveWorkItem.actions());
			assertEquals(List.of(
				new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
				new AfterCommitAction.CloseDialog()), giveWorkItem.afterCommit());

			QuestTransition useWorkItem = route(definition, "v2", new QuestEvent.UseItem(quest.workItemId()));
			assertEquals("reward", useWorkItem.targetNode());
			assertEquals(List.of(), useWorkItem.actions());
			assertEquals(List.of(
				new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
				new AfterCommitAction.ShowQuestSelectionDialog(QuestDialogPage.SELECT_QUEST.id())),
				useWorkItem.afterCommit());

			assertPage(definition, "reward", QuestDialogAction.QUEST_SELECT, QuestDialogPage.DEFAULT_SUCCESS);
			assertPage(definition, "reward", QuestDialogAction.SELECT_QUEST_REWARD,
				QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1);
			for (int actionId = REWARD_ACTION_FIRST; actionId <= REWARD_ACTION_LAST; actionId++) {
				QuestTransition completion = route(definition, "reward", new QuestEvent.TalkToNpc(NPC_ID, actionId));
				assertEquals("complete", completion.targetNode());
				assertEquals(List.of(
					new QuestAction.RemoveItem(quest.workItemId(), QuestAction.RemoveItem.ALL),
					new QuestAction.GrantReward("GOLD", 0, quest.gold, QuestRewardAmountMode.QUEST_BASE),
					new QuestAction.GrantReward("EXP", 0, EXPERIENCE, QuestRewardAmountMode.QUEST_BASE),
					new QuestAction.GrantReward("ITEM", REWARD_ITEM_ID, 6),
					new QuestAction.CompleteQuest(0)), completion.actions());
				assertEquals(List.of(
					new AfterCommitAction.RefreshPlayerStats(),
					new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION),
					new AfterCommitAction.ShowQuestSelectionDialog(QuestDialogPage.SELECT_QUEST.id())),
					completion.afterCommit());
			}
		}
	}

	@Test
	void consumedWorkItemDoesNotBlockTheCollectionToCompletionPlan() throws Exception {
		for (QuestCase quest : QUESTS) {
			CompiledQuestDefinition definition = load(quest.questId());
			int started = definition.definition().progressLayout().pack(Map.of("var0", 0));
			int collected = definition.definition().progressLayout().pack(Map.of("var0", 1));
			int prepared = definition.definition().progressLayout().pack(Map.of("var0", 2));
			int reward = definition.definition().progressLayout().pack(Map.of("var0", 3));

			QuestTransition accept = route(definition.definition(), "unaccepted",
				new QuestEvent.TalkToNpc(NPC_ID, QuestDialogAction.QUEST_ACCEPT_1.id()));
			QuestSnapshot eligible = new QuestSnapshot(PLAYER_ID, quest.questId(), QuestStatus.NONE, 0, Map.of())
				.withStartEligibility(QuestStartEligibility.allowed())
				.withCompletedQuestIds(quest.prerequisiteQuestId() == 0
					? Set.of() : Set.of(quest.prerequisiteQuestId()));
			assertTrue(QuestMutationPlanner.plan(definition, eligible, accept.event(), accept).isPresent(),
				"quest " + quest.questId() + " must accept with its prerequisites");
			if (quest.prerequisiteQuestId() != 0) {
				assertFalse(QuestMutationPlanner.plan(definition, eligible.withCompletedQuestIds(Set.of()),
					accept.event(), accept).isPresent(), "quest " + quest.questId() + " must enforce its prerequisite");
			}

			QuestTransition collect = route(definition.definition(), "started",
				new QuestEvent.TalkToNpc(NPC_ID, QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM.id()), 0);
			QuestSnapshot collecting = new QuestSnapshot(PLAYER_ID, quest.questId(), QuestStatus.START, started,
				Map.of(quest.collectItemId(), COLLECT_COUNT));
			QuestMutationPlan collectPlan = QuestMutationPlanner.plan(definition, collecting, collect.event(), collect)
				.orElseThrow(() -> new AssertionError("collection route blocked for quest " + quest.questId()));
			assertEquals(QuestStatus.START, collectPlan.nextStatus());
			assertEquals(collected, collectPlan.nextPackedVariables());
			assertEquals(List.of(new QuestAction.RemoveItem(quest.collectItemId(), COLLECT_COUNT)),
				collectPlan.requiredActions());

			QuestTransition giveWorkItem = route(definition.definition(), "v1",
				new QuestEvent.TalkToNpc(NPC_ID, QuestDialogAction.SETPRO2.id()));
			QuestMutationPlan givePlan = QuestMutationPlanner.plan(definition,
				new QuestSnapshot(PLAYER_ID, quest.questId(), QuestStatus.START, collected, Map.of()),
				giveWorkItem.event(), giveWorkItem).orElseThrow();
			assertEquals(prepared, givePlan.nextPackedVariables());
			assertEquals(List.of(new QuestAction.GiveItem(quest.workItemId(), 1)), givePlan.requiredActions());

			QuestTransition useWorkItem = route(definition.definition(), "v2", new QuestEvent.UseItem(quest.workItemId()));
			QuestMutationPlan usePlan = QuestMutationPlanner.plan(definition,
				new QuestSnapshot(PLAYER_ID, quest.questId(), QuestStatus.START, prepared, Map.of()),
				useWorkItem.event(), useWorkItem).orElseThrow();
			assertEquals(QuestStatus.REWARD, usePlan.nextStatus());
			assertEquals(reward, usePlan.nextPackedVariables());
			assertEquals(List.of(), usePlan.requiredActions());

			QuestTransition completion = route(definition.definition(), "reward",
				new QuestEvent.TalkToNpc(NPC_ID, QuestDialogAction.SELECTED_QUEST_REWARD1.id()));
			List<QuestAction> expectedCompletion = List.of(
				new QuestAction.RemoveItem(quest.workItemId(), QuestAction.RemoveItem.ALL),
				new QuestAction.GrantReward("GOLD", 0, quest.gold, QuestRewardAmountMode.QUEST_BASE),
				new QuestAction.GrantReward("EXP", 0, EXPERIENCE, QuestRewardAmountMode.QUEST_BASE),
				new QuestAction.GrantReward("ITEM", REWARD_ITEM_ID, 6),
				new QuestAction.CompleteQuest(0));
			QuestMutationPlan emptyInventoryCompletion = QuestMutationPlanner.plan(definition,
				new QuestSnapshot(PLAYER_ID, quest.questId(), QuestStatus.REWARD, reward, Map.of()),
				completion.event(), completion).orElseThrow(() ->
					new AssertionError("completed consumable blocked quest " + quest.questId()));
			assertEquals(QuestStatus.COMPLETE, emptyInventoryCompletion.nextStatus());
			assertEquals(expectedCompletion, emptyInventoryCompletion.requiredActions());
			QuestMutationPlan carriedItemCompletion = QuestMutationPlanner.plan(definition,
				new QuestSnapshot(PLAYER_ID, quest.questId(), QuestStatus.REWARD, reward,
					Map.of(quest.workItemId(), 1)), completion.event(), completion).orElseThrow();
			assertEquals(expectedCompletion, carriedItemCompletion.requiredActions());
		}
	}

	@Test
	void protocolLoopCompletesBothQuestsFromTheClientDialogContract() throws Exception {
		ClientResourceOracle oracle = ClientResourceOracle.load(Path.of("docs/quest/client-dialog-mapping"));
		for (QuestCase quest : QUESTS) {
			CompiledQuestDefinition definition = load(quest.questId());
			List<QuestProductionJourneyPlanner.PlannedStep> steps = List.of(
				step(QuestProductionJourneyPlanner.StepKind.INTERACT, route(definition, "unaccepted",
					new QuestEvent.TalkToNpc(NPC_ID, QuestDialogAction.QUEST_SELECT.id()))),
				step(QuestProductionJourneyPlanner.StepKind.PAGE_ACTION, route(definition, "unaccepted",
					new QuestEvent.TalkToNpc(NPC_ID, QuestDialogAction.ASK_QUEST_ACCEPT.id()))),
				step(QuestProductionJourneyPlanner.StepKind.PAGE_ACTION, route(definition, "unaccepted",
					new QuestEvent.TalkToNpc(NPC_ID, QuestDialogAction.QUEST_ACCEPT_1.id()))),
				step(QuestProductionJourneyPlanner.StepKind.PAGE_ACTION, route(definition, "started",
					new QuestEvent.TalkToNpc(NPC_ID, QuestDialogAction.FINISH_DIALOG.id()))),
				step(QuestProductionJourneyPlanner.StepKind.INTERACT, route(definition, "started",
					new QuestEvent.TalkToNpc(NPC_ID, QuestDialogAction.QUEST_SELECT.id()))),
				step(QuestProductionJourneyPlanner.StepKind.PAGE_ACTION, route(definition, "started",
					new QuestEvent.TalkToNpc(NPC_ID, QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM.id()), 0)),
				step(QuestProductionJourneyPlanner.StepKind.PAGE_ACTION, route(definition, "v1",
					new QuestEvent.TalkToNpc(NPC_ID, QuestDialogAction.FINISH_DIALOG.id()))),
				step(QuestProductionJourneyPlanner.StepKind.INTERACT, route(definition, "v1",
					new QuestEvent.TalkToNpc(NPC_ID, QuestDialogAction.QUEST_SELECT.id()))),
				step(QuestProductionJourneyPlanner.StepKind.PAGE_ACTION, route(definition, "v1",
					new QuestEvent.TalkToNpc(NPC_ID, QuestDialogAction.SETPRO2.id()))),
				step(QuestProductionJourneyPlanner.StepKind.USE_ITEM, route(definition, "v2",
					new QuestEvent.UseItem(quest.workItemId()))),
				step(QuestProductionJourneyPlanner.StepKind.INTERACT, route(definition, "reward",
					new QuestEvent.TalkToNpc(NPC_ID, QuestDialogAction.QUEST_SELECT.id()))),
				step(QuestProductionJourneyPlanner.StepKind.PAGE_ACTION, route(definition, "reward",
					new QuestEvent.TalkToNpc(NPC_ID, QuestDialogAction.SELECT_QUEST_REWARD.id()))),
				step(QuestProductionJourneyPlanner.StepKind.NATIVE_REWARD_ACTION, route(definition, "reward",
					new QuestEvent.TalkToNpc(NPC_ID, QuestDialogAction.SELECTED_QUEST_REWARD1.id()))));

			QuestProductionJourneyPlanner.Plan plan = new QuestProductionJourneyPlanner.Plan(quest.questId(), "unaccepted",
				"complete", PlayerClass.GLADIATOR, Map.of(quest.collectItemId(), COLLECT_COUNT), steps);
			QuestProductionJourneyExecutor.Result result = new QuestProductionJourneyExecutor()
				.execute(definition, oracle, plan);
			assertTrue(result.completed(), () -> "quest " + quest.questId() + ": " + result.failure());
		}
	}

	private static QuestProductionJourneyPlanner.PlannedStep step(
		QuestProductionJourneyPlanner.StepKind kind, QuestTransition transition) {
		return new QuestProductionJourneyPlanner.PlannedStep(kind, transition);
	}

	private static void assertPage(QuestDefinition definition, String source, QuestDialogAction action,
		QuestDialogPage page) {
		QuestTransition transition = route(definition, source, new QuestEvent.TalkToNpc(NPC_ID, action.id()));
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(page.id())), transition.afterCommit());
	}

	private static QuestTransition route(QuestDefinition definition, String source, QuestEvent event) {
		return route(definition, source, event, null);
	}

	private static QuestTransition route(QuestDefinition definition, String source, QuestEvent event, Integer priority) {
		return definition.transitions().stream()
			.filter(transition -> source.equals(transition.sourceNode()) && transition.event().equals(event))
			.filter(transition -> priority == null || Objects.equals(priority, transition.priority()))
			.findFirst()
			.orElseThrow(() -> new AssertionError("missing route " + source + " " + event + " priority=" + priority));
	}

	private static QuestTransition route(CompiledQuestDefinition definition, String source, QuestEvent event) {
		return route(definition.definition(), source, event);
	}

	private static QuestTransition route(CompiledQuestDefinition definition, String source, QuestEvent event,
		Integer priority) {
		return route(definition.definition(), source, event, priority);
	}

	private static void assertNode(QuestDefinition definition, String label, QuestStatus status, int var0) {
		QuestNode node = definition.nodes().stream()
			.filter(candidate -> label.equals(candidate.label()))
			.findFirst()
			.orElseThrow();
		assertEquals(status, node.projection().status());
		assertEquals(Map.of("var0", var0), node.projection().variables());
	}

	private static CompiledQuestDefinition load(int questId) throws Exception {
		try (InputStream input = Quest11031And11032RetailFlowTest.class.getResourceAsStream(
			"/aion/data/static_data/quest_definition/quests/" + questId + ".xml")) {
			return QuestDefinitionXmlCompiler.compile(Objects.requireNonNull(input,
				"missing quest definition " + questId));
		}
	}
}
