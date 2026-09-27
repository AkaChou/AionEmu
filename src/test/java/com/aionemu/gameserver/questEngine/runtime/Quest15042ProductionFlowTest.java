package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.AfterCommitAction;
import com.aionemu.gameserver.questEngine.definition.BitField;
import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
import com.aionemu.gameserver.questEngine.definition.QuestAction;
import com.aionemu.gameserver.questEngine.definition.QuestCondition;
import com.aionemu.gameserver.questEngine.definition.QuestDefinition;
import com.aionemu.gameserver.questEngine.definition.QuestDialogAction;
import com.aionemu.gameserver.questEngine.definition.QuestDialogPage;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.definition.QuestItemRequirement;
import com.aionemu.gameserver.questEngine.definition.QuestRewardAmountMode;
import com.aionemu.gameserver.questEngine.definition.QuestStateSyncMode;
import com.aionemu.gameserver.questEngine.definition.QuestTransition;
import com.aionemu.gameserver.questEngine.definition.ProductionQuestDefinitions;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证真端任务的三次道具演出、物品产出和领奖合同。
 * Verifies the three retail item plays, output item and reward contract for Calling Kaisinel's Butterfly.
 */
class Quest15042ProductionFlowTest {
	private static final int QUEST_ID = 15042;
	private static final int REPORT_NPC_ID = 804885;
	private static final int FLUTE_ITEM_ID = 182215676;
	private static final int BUTTERFLY_ITEM_ID = 182215753;
	private static final int DRAGON_LORDS_GARDENS_WORLD_ID = 210070000;

	@Test
	void preservesTheThreeItemPlaysAndRewardFlow() {
		CompiledQuestDefinition compiled = load();
		QuestDefinition definition = compiled.definition();
		assertProgressAndWorkItems(definition);
		assertNode(definition, "unaccepted", QuestStatus.NONE, Map.of("var0", 0));
		assertNode(definition, "started", QuestStatus.START, Map.of("var0", 0));
		assertNode(definition, "reward", QuestStatus.REWARD, Map.of("var0", 1));
		assertNode(definition, "complete", QuestStatus.COMPLETE, Map.of("var0", 0, "var1", 0));
		QuestTransition accept = transition(definition, "unaccepted", "started",
			new QuestEvent.TalkToNpc(REPORT_NPC_ID, QuestDialogAction.QUEST_ACCEPT_SIMPLE.id()));
		assertEquals(List.of(new QuestCondition.StartEligible()), accept.conditions());
		assertEquals(List.of(new QuestAction.GiveItem(FLUTE_ITEM_ID, 1)), accept.actions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH),
			new AfterCommitAction.CloseDialog()), accept.afterCommit());

		QuestEvent useFlute = new QuestEvent.ItemPlay(FLUTE_ITEM_ID, 3000);
		QuestTransition countUse = transition(definition, "started", "started", useFlute);
		assertEquals(1, countUse.priority());
		assertEquals(List.of(new QuestCondition.VariableBelow("var1", 2)), countUse.conditions());
		assertEquals(List.of(new QuestAction.IncrementVariable("var1", 1)), countUse.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
			countUse.afterCommit());

		QuestTransition finalUse = transition(definition, "started", "reward", useFlute);
		assertEquals(0, finalUse.priority());
		assertEquals(List.of(new QuestCondition.VariableAtLeast("var1", 2)), finalUse.conditions());
		assertEquals(List.of(
			new QuestAction.GiveItem(BUTTERFLY_ITEM_ID, 1),
			new QuestAction.RemoveItem(FLUTE_ITEM_ID, 1),
			new QuestAction.SetVariable("var0", 1),
			new QuestAction.SetVariable("var1", 0)), finalUse.actions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)),
			finalUse.afterCommit());

		QuestSnapshot outside = snapshot(QuestStatus.START, Map.of("var0", 0, "var1", 0),
			Map.of(FLUTE_ITEM_ID, 1), 210060000, definition);
		assertNoMatch(compiled, outside, new QuestEvent.UseItem(FLUTE_ITEM_ID));

		QuestSnapshot current = snapshot(QuestStatus.START, Map.of("var0", 0, "var1", 0),
			Map.of(FLUTE_ITEM_ID, 1), DRAGON_LORDS_GARDENS_WORLD_ID, definition);
		for (int count = 1; count <= 2; count++) {
			QuestMutationPlan plan = dispatch(compiled, current, useFlute);
			assertEquals(QuestStatus.START, plan.nextStatus());
			assertEquals(List.of(new QuestAction.IncrementVariable("var1", 1)), plan.requiredActions());
			current = nextSnapshot(current, plan, current.inventory());
			assertEquals(Map.of("var0", 0, "var1", count),
				definition.progressLayout().unpack(current.packedVariables()));
		}

		QuestMutationPlan completedUse = dispatch(compiled, current, useFlute);
		assertEquals(QuestStatus.REWARD, completedUse.nextStatus());
		assertEquals(Map.of("var0", 1, "var1", 0),
			definition.progressLayout().unpack(completedUse.nextPackedVariables()));
		assertEquals(finalUse.actions(), completedUse.requiredActions());

		assertRewardDialog(definition, QuestDialogAction.QUEST_SELECT, QuestDialogPage.DEFAULT_SUCCESS);
		assertRewardDialog(definition, QuestDialogAction.USE_OBJECT,
			QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1);
		assertRewardDialog(definition, QuestDialogAction.SELECT_QUEST_REWARD,
			QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1);
		assertCompletion(compiled, definition, QuestDialogAction.SELECTED_QUEST_REWARD1, 169300007);
		assertCompletion(compiled, definition, QuestDialogAction.SELECTED_QUEST_REWARD2, 169000010);
	}

	private static void assertProgressAndWorkItems(QuestDefinition definition) {
		BitField stage = definition.progressLayout().field("var0");
		assertEquals(0, stage.offset());
		assertEquals(6, stage.width());
		assertEquals(63, stage.maxValue());
		BitField useCount = definition.progressLayout().field("var1");
		assertEquals(6, useCount.offset());
		assertEquals(6, useCount.width());
		assertEquals(63, useCount.maxValue());
		assertEquals(List.of(
			new QuestItemRequirement(FLUTE_ITEM_ID, 1),
			new QuestItemRequirement(BUTTERFLY_ITEM_ID, 1)), definition.metadata().questWorkItems());
	}

	private static void assertRewardDialog(QuestDefinition definition, QuestDialogAction action,
			QuestDialogPage page) {
		QuestTransition transition = transition(definition, "reward", "reward",
			new QuestEvent.TalkToNpc(REPORT_NPC_ID, action.id()));
		assertEquals(List.of(), transition.conditions());
		assertEquals(List.of(), transition.actions());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(page.id())), transition.afterCommit());
	}

	private static void assertCompletion(CompiledQuestDefinition compiled, QuestDefinition definition,
			QuestDialogAction action, int selectedItemId) {
		QuestEvent event = new QuestEvent.TalkToNpc(REPORT_NPC_ID, action.id());
		QuestTransition completion = transition(definition, "reward", "complete", event);
		List<QuestAction> declaredActions = List.of(
			new QuestAction.GrantReward("GOLD", 0, 106740, QuestRewardAmountMode.QUEST_BASE),
			new QuestAction.GrantReward("EXP", 0, 9340221, QuestRewardAmountMode.QUEST_BASE),
			new QuestAction.GrantReward("ITEM", 186000231, 10),
			new QuestAction.GrantReward("ITEM", 186000237, 15),
			new QuestAction.GrantReward("ITEM", selectedItemId, 500),
			new QuestAction.CompleteQuest(0));
		assertEquals(declaredActions, completion.actions());
		assertEquals(List.of(
			new AfterCommitAction.RefreshPlayerStats(),
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION),
			new AfterCommitAction.ShowQuestSelectionDialog(QuestDialogPage.SELECT_QUEST.id())),
			completion.afterCommit());

		QuestSnapshot reward = snapshot(QuestStatus.REWARD, Map.of("var0", 1, "var1", 0),
			Map.of(BUTTERFLY_ITEM_ID, 1), DRAGON_LORDS_GARDENS_WORLD_ID, definition);
		QuestMutationPlan plan = QuestMutationPlanner.plan(compiled, reward, event, completion).orElseThrow();
		assertEquals(QuestStatus.COMPLETE, plan.nextStatus());
		assertEquals(Map.of("var0", 0, "var1", 0),
			definition.progressLayout().unpack(plan.nextPackedVariables()));
		assertEquals(List.of(
			declaredActions.get(0), declaredActions.get(1), declaredActions.get(2), declaredActions.get(3),
			declaredActions.get(4), declaredActions.get(5),
			new QuestAction.RemoveItem(FLUTE_ITEM_ID, QuestAction.RemoveItem.ALL),
			new QuestAction.RemoveItem(BUTTERFLY_ITEM_ID, QuestAction.RemoveItem.ALL)), plan.requiredActions());
	}

	private static QuestMutationPlan dispatch(CompiledQuestDefinition compiled, QuestSnapshot snapshot,
			QuestEvent event) {
		List<QuestMutationPlan> plans = compiled.definition().transitions().stream()
			.map(transition -> QuestMutationPlanner.plan(compiled, snapshot, event, transition).orElse(null))
			.filter(Objects::nonNull)
			.toList();
		assertEquals(1, plans.size(), () -> event + " "
			+ compiled.definition().progressLayout().unpack(snapshot.packedVariables()));
		return plans.getFirst();
	}

	private static void assertNoMatch(CompiledQuestDefinition compiled, QuestSnapshot snapshot, QuestEvent event) {
		assertTrue(compiled.definition().transitions().stream().noneMatch(transition ->
			QuestMutationPlanner.plan(compiled, snapshot, event, transition).isPresent()));
	}

	private static QuestSnapshot nextSnapshot(QuestSnapshot snapshot, QuestMutationPlan plan,
			Map<Integer, Integer> inventory) {
		return new QuestSnapshot(snapshot.playerId(), snapshot.questId(), plan.nextStatus(),
			plan.nextPackedVariables(), inventory, Map.of(), true, true, 0, 0,
			snapshot.worldId(), snapshot.instanceId(), 0f, 0f, 0f, (byte) 0);
	}

	private static QuestSnapshot snapshot(QuestStatus status, Map<String, Integer> variables,
			Map<Integer, Integer> inventory, int worldId, QuestDefinition definition) {
		return new QuestSnapshot(7, QUEST_ID, status, definition.progressLayout().pack(variables),
			inventory, Map.of(), true, true, 0, 0, worldId, 1, 0f, 0f, 0f, (byte) 0);
	}

	private static void assertNode(QuestDefinition definition, String label, QuestStatus status,
			Map<String, Integer> variables) {
		var node = definition.nodes().stream()
			.filter(candidate -> candidate.label().equals(label))
			.findFirst().orElseThrow();
		assertEquals(status, node.projection().status());
		assertEquals(variables, node.projection().variables());
	}

	private static QuestTransition transition(QuestDefinition definition, String source, String target,
			QuestEvent event) {
		List<QuestTransition> matches = definition.transitions().stream()
			.filter(candidate -> source.equals(candidate.sourceNode()))
			.filter(candidate -> candidate.targetNode().equals(target))
			.filter(candidate -> candidate.event().equals(event))
			.toList();
		assertEquals(1, matches.size(), () -> source + " -> " + target + " " + event);
		return matches.getFirst();
	}

	private static CompiledQuestDefinition load() {
		return ProductionQuestDefinitions.definitionInOverlay(QUEST_ID);
	}
}
