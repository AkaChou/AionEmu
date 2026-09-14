package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlan;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlanner;
import com.aionemu.gameserver.questEngine.runtime.QuestSnapshot;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 验证任务 1922 的最终交任务合同：可选收集物的整叠移除不得阻断完成。
 * Verifies quest 1922's final turn-in contract: removing the optional collectible as a whole
 * stack must never block completion, whether or not the player still carries it.
 */
class Quest1922RewardTurnInTest {
	private static final int PLAYER_ID = 7;
	private static final int QUEST_ID = 1922;
	private static final int REWARD_NPC = 203901;
	private static final int FORTRESS_FLAG = 182206030;
	private static final int REWARD_ITEM = 167000497;
	private static final int FIRST_REWARD_ACTION = QuestDialogAction.SELECTED_QUEST_REWARD1.id();
	private static final int LAST_REWARD_ACTION = QuestDialogAction.SELECTED_QUEST_NOREWARD.id();

	@Test
	void finalTurnInCompletesWhenTheFlagIsMissing() throws Exception {
		CompiledQuestDefinition definition = load();
		QuestTransition turnIn = rewardTurnInRoute(definition, LAST_REWARD_ACTION);

		assertEquals("reward", turnIn.sourceNode());
		assertEquals("complete", turnIn.targetNode());
		assertEquals(List.of(), turnIn.conditions());
		assertEquals(finalTurnInActions(), turnIn.actions());
		assertEquals(finalTurnInAfterCommit(), turnIn.afterCommit());

		QuestMutationPlan plan = plan(definition, turnIn, Map.of());
		assertEquals(QuestStatus.COMPLETE, plan.nextStatus());
		assertEquals(definition.definition().progressLayout().pack(Map.of("var0", 0, "var4", 10)),
			plan.nextPackedVariables());
		assertEquals(finalTurnInActions(), plan.requiredActions());
		assertEquals(finalTurnInAfterCommit(), plan.afterCommit());
	}

	@Test
	void finalTurnInConsumesTheFlagStackWhenPresent() throws Exception {
		CompiledQuestDefinition definition = load();
		QuestTransition turnIn = rewardTurnInRoute(definition, FIRST_REWARD_ACTION);

		QuestMutationPlan plan = plan(definition, turnIn, Map.of(FORTRESS_FLAG, 1));
		assertEquals(QuestStatus.COMPLETE, plan.nextStatus());
		assertEquals(finalTurnInActions(), plan.requiredActions());
	}

	@Test
	void everyRewardWindowActionSharesTheSingleCompletionRoute() throws Exception {
		CompiledQuestDefinition definition = load();
		for (int action = FIRST_REWARD_ACTION; action <= LAST_REWARD_ACTION; action++) {
			QuestTransition turnIn = rewardTurnInRoute(definition, action);
			assertEquals("complete", turnIn.targetNode());
			assertEquals(finalTurnInActions(), turnIn.actions());
		}

		QuestTransition preview = route(definition, "s7", QuestDialogAction.SELECT_QUEST_REWARD);
		assertEquals("reward", preview.targetNode());
		assertEquals(List.of(new QuestAction.SetStatus(QuestStatus.REWARD)), preview.actions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW2.id())),
			preview.afterCommit());
	}

	private static List<QuestAction> finalTurnInActions() {
		return List.of(
			new QuestAction.GrantReward("GOLD", 0, 4000, QuestRewardAmountMode.QUEST_BASE),
			new QuestAction.GrantReward("EXP", 0, 8325278, QuestRewardAmountMode.QUEST_BASE),
			new QuestAction.GrantReward("ITEM", REWARD_ITEM, 1),
			new QuestAction.RemoveItem(FORTRESS_FLAG, QuestAction.RemoveItem.ALL),
			new QuestAction.CompleteQuest(1));
	}

	private static List<AfterCommitAction> finalTurnInAfterCommit() {
		return List.of(
			new AfterCommitAction.RefreshPlayerStats(),
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION),
			new AfterCommitAction.ShowQuestSelectionDialog(QuestDialogPage.SELECT_QUEST.id()));
	}

	private static QuestMutationPlan plan(CompiledQuestDefinition definition, QuestTransition transition,
			Map<Integer, Integer> inventory) {
		int packed = definition.definition().progressLayout().pack(Map.of("var0", 7, "var4", 10));
		QuestEvent.TalkToNpc event = (QuestEvent.TalkToNpc) transition.event();
		return QuestMutationPlanner.plan(definition,
			new QuestSnapshot(PLAYER_ID, QUEST_ID, QuestStatus.REWARD, packed, inventory), event, transition)
			.orElseThrow(() -> new AssertionError("quest 1922 final turn-in produced no mutation plan for "
				+ event.dialogId()));
	}

	private static QuestTransition rewardTurnInRoute(CompiledQuestDefinition definition, int action) {
		QuestEvent.TalkToNpc event = new QuestEvent.TalkToNpc(REWARD_NPC, action);
		List<QuestTransition> routes = definition.definition().transitions().stream()
			.filter(transition -> "reward".equals(transition.sourceNode())
				&& "complete".equals(transition.targetNode())
				&& QuestEvent.matches(transition.event(), event))
			.toList();
		assertEquals(1, routes.size(), "reward -> complete route count for action " + action);
		return routes.getFirst();
	}

	private static QuestTransition route(CompiledQuestDefinition definition, String source,
			QuestDialogAction action) {
		return definition.definition().transitions().stream()
			.filter(transition -> source.equals(transition.sourceNode())
				&& transition.event().equals(new QuestEvent.TalkToNpc(REWARD_NPC, action.id())))
			.findFirst()
			.orElseThrow(() -> new AssertionError("missing quest 1922 route: " + source + " " + action));
	}

	private static CompiledQuestDefinition load() throws Exception {
		try (InputStream input = Quest1922RewardTurnInTest.class.getResourceAsStream(
				"/aion/data/static_data/quest_definition/quests/1922.xml")) {
			assertNotNull(input, "missing quest definition 1922.xml");
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
