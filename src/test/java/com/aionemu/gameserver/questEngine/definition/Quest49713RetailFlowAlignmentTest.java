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
 * 锁定 49713 的阵营接取、南部支部领取粉末、物品使用和报告时序。
 * Locks quest 49713's faction acquisition, South Katalam powder handoff, item use, and report ordering.
 */
class Quest49713RetailFlowAlignmentTest {
	private static final int QUEST_ID = 49713;
	private static final int NPC_FACTION_ID = 18;
	private static final int POWDER_ITEM_ID = 182215277;
	private static final Set<Integer> SOUTH_BRANCH_NPC_IDS = Set.of(800936, 800937, 800938);
	private static final Set<Integer> NORTH_BRANCH_NPC_IDS = Set.of(800934, 800935);

	@Test
	void preservesFactionMetadataProgressAndClientNpcContract() throws Exception {
		QuestDefinition definition = load().definition();

		assertEquals(NPC_FACTION_ID, definition.metadata().npcFactionId());
		assertEquals(Set.of("ALL"), definition.metadata().repeatCycles());
		assertEquals(List.of(new QuestItemRequirement(POWDER_ITEM_ID, 1)),
			definition.metadata().questWorkItems());
		assertEquals(new NodeProjection(QuestStatus.NONE, Map.of("var0", 0)),
			node(definition, "unaccepted").projection());
		assertEquals(new NodeProjection(QuestStatus.START, Map.of("var0", 0)),
			node(definition, "started").projection());
		assertEquals(new NodeProjection(QuestStatus.START, Map.of("var0", 1)),
			node(definition, "powder-received").projection());
		assertEquals(new NodeProjection(QuestStatus.START, Map.of("var0", 2)),
			node(definition, "powder-used").projection());
		assertEquals(new NodeProjection(QuestStatus.REWARD, Map.of("var0", 2)),
			node(definition, "reward").projection());

		Set<Integer> dialogNpcIds = definition.transitions().stream()
			.map(QuestTransition::event)
			.filter(QuestEvent.TalkToNpc.class::isInstance)
			.map(QuestEvent.TalkToNpc.class::cast)
			.map(QuestEvent.TalkToNpc::npcId)
			.collect(Collectors.toSet());
		assertEquals(SOUTH_BRANCH_NPC_IDS, dialogNpcIds);
		assertTrue(dialogNpcIds.stream().noneMatch(NORTH_BRANCH_NPC_IDS::contains));
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
	void allSouthBranchNpcsUseTheClientReceivePageAndGiveThePowder() throws Exception {
		CompiledQuestDefinition definition = load();
		for (int npcId : SOUTH_BRANCH_NPC_IDS) {
			QuestEvent select = new QuestEvent.TalkToNpc(npcId, QuestDialogAction.QUEST_SELECT.id());
			QuestTransition selectRoute = route(definition.definition(), "started", "started", select);
			assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT2.id())),
				selectRoute.afterCommit());

			QuestEvent receive = new QuestEvent.TalkToNpc(npcId, QuestDialogAction.SETPRO1.id());
			QuestTransition receiveRoute = route(definition.definition(), "started", "powder-received", receive);
			assertEquals(List.of(), receiveRoute.conditions());
			assertEquals(List.of(
				new QuestAction.GiveItem(POWDER_ITEM_ID, 1),
				new QuestAction.SetVariable("var0", 1)), receiveRoute.actions());
			assertEquals(List.of(
				new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
				new AfterCommitAction.CloseDialog()), receiveRoute.afterCommit());

			QuestMutationPlan plan = QuestMutationPlanner.plan(definition,
				new QuestSnapshot(7, QUEST_ID, QuestStatus.START, packed(definition, 0), Map.of()),
				receive, receiveRoute).orElseThrow();
			assertEquals(QuestStatus.START, plan.nextStatus());
			assertEquals(packed(definition, 1), plan.nextPackedVariables());
		}
	}

	@Test
	void useItemConsumesThePowderAndUnlocksTheClientReportPage() throws Exception {
		CompiledQuestDefinition definition = load();
		for (int npcId : SOUTH_BRANCH_NPC_IDS) {
			QuestTransition select = route(definition.definition(), "powder-used", "powder-used",
				new QuestEvent.TalkToNpc(npcId, QuestDialogAction.QUEST_SELECT.id()));
			assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT5.id())),
				select.afterCommit());

			QuestEvent use = new QuestEvent.UseItem(POWDER_ITEM_ID);
			QuestTransition useRoute = definition.definition().transitions().stream()
				.filter(transition -> "powder-received".equals(transition.sourceNode()))
				.filter(transition -> "powder-used".equals(transition.targetNode()))
				.filter(transition -> transition.event().equals(use))
				.findFirst().orElseThrow();
			assertEquals(List.of(
				new QuestCondition.QuestVariableIs("var0", 1),
				new QuestCondition.HasItem(POWDER_ITEM_ID, 1)), useRoute.conditions());
			assertEquals(List.of(
				new QuestAction.RemoveItem(POWDER_ITEM_ID, 1),
				new QuestAction.SetVariable("var0", 2)), useRoute.actions());
			assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
				useRoute.afterCommit());

			QuestMutationPlan plan = QuestMutationPlanner.plan(definition,
				new QuestSnapshot(7, QUEST_ID, QuestStatus.START, packed(definition, 1),
					Map.of(POWDER_ITEM_ID, 1)), use, useRoute).orElseThrow();
			assertEquals(QuestStatus.START, plan.nextStatus());
			assertEquals(packed(definition, 2), plan.nextPackedVariables());
			assertTrue(plan.requiredActions().contains(new QuestAction.RemoveItem(POWDER_ITEM_ID, 1)));
		}
	}

	@Test
	void reportingUsesOnlyTheVisibleSelectQuestRewardAction() throws Exception {
		CompiledQuestDefinition definition = load();
		for (int npcId : SOUTH_BRANCH_NPC_IDS) {
			QuestEvent report = new QuestEvent.TalkToNpc(npcId, QuestDialogAction.SELECT_QUEST_REWARD.id());
			QuestTransition reportRoute = route(definition.definition(), "powder-used", "reward", report);
			assertEquals(List.of(new QuestCondition.QuestVariableIs("var0", 2)), reportRoute.conditions());
			assertEquals(List.of(
				new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
				new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())),
				reportRoute.afterCommit());

			QuestMutationPlan plan = QuestMutationPlanner.plan(definition,
				new QuestSnapshot(7, QUEST_ID, QuestStatus.START, packed(definition, 2), Map.of()),
				report, reportRoute).orElseThrow();
			assertEquals(QuestStatus.REWARD, plan.nextStatus());
			assertEquals(packed(definition, 2), plan.nextPackedVariables());
			assertFalse(definition.definition().transitions().stream()
				.anyMatch(transition -> transition.event().equals(
					new QuestEvent.TalkToNpc(npcId, QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM.id()))));
		}
	}

	@Test
	void rewardPreviewAndCompletionKeepFactionLifecycleBeforeClientPackets() throws Exception {
		CompiledQuestDefinition definition = load();
		for (int npcId : SOUTH_BRANCH_NPC_IDS) {
			QuestTransition preview = route(definition.definition(), "reward", "reward",
				new QuestEvent.TalkToNpc(npcId, QuestDialogAction.USE_OBJECT.id()));
			assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(
				QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())), preview.afterCommit());

			QuestEvent complete = new QuestEvent.TalkToNpc(npcId, QuestDialogAction.SELECTED_QUEST_REWARD1.id());
			QuestTransition completion = route(definition.definition(), "reward", "complete", complete);
			assertEquals(List.of(
				new QuestAction.GrantReward("EXP", 0, 3251515, QuestRewardAmountMode.EXACT),
				new QuestAction.GrantReward("ITEM", 186000234, 3, QuestRewardAmountMode.EXACT),
				new QuestAction.GrantReward("ITEM", 188052288, 1, QuestRewardAmountMode.EXACT),
				new QuestAction.CompleteQuest(0)), completion.actions());

			QuestMutationPlan plan = QuestMutationPlanner.plan(definition,
				new QuestSnapshot(7, QUEST_ID, QuestStatus.REWARD, packed(definition, 2), Map.of()),
				complete, completion).orElseThrow();
			assertEquals(QuestStatus.COMPLETE, plan.nextStatus());
			assertTrue(plan.requiredActions().contains(new QuestAction.RemoveItem(POWDER_ITEM_ID,
				QuestAction.RemoveItem.ALL)));
			assertEquals(List.of(
				new AfterCommitAction.CompleteNpcFactionQuest(NPC_FACTION_ID),
				new AfterCommitAction.RefreshPlayerStats(),
				new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION),
				new AfterCommitAction.ShowQuestSelectionDialog(QuestDialogPage.SELECT_QUEST.id())),
				plan.afterCommit());
		}
	}

	private static int packed(CompiledQuestDefinition definition, int value) {
		return definition.definition().progressLayout().pack(Map.of("var0", value));
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
		String resource = "/aion/data/static_data/quest_definition/quests/49713.xml";
		try (InputStream input = Quest49713RetailFlowAlignmentTest.class.getResourceAsStream(resource)) {
			return QuestDefinitionXmlCompiler.compile(Objects.requireNonNull(input, resource));
		}
	}
}
