package com.aionemu.gameserver.questEngine.definition;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlan;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlanner;
import com.aionemu.gameserver.questEngine.runtime.QuestSnapshot;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * 锁定任务 10100/20100 阅读任务道具后进入 REWARD 时扣除道具的旧服合同。
 * Locks the legacy contract that quests 10100/20100 consume their read item when entering REWARD.
 */
class Quest10100And20100ItemUseRemovalTest {
	private static final int PLAYER_ID = 7;
	private static final List<QuestCase> QUESTS = List.of(
		new QuestCase(10100, 182215448),
		new QuestCase(20100, 182215449));

	private record QuestCase(int questId, int itemId) {}

	@Test
	void consumesTheReadItemOnTheUseItemTransition() throws Exception {
		for (QuestCase quest : QUESTS) {
			CompiledQuestDefinition compiled = definition(quest.questId());
			QuestDefinition definition = compiled.definition();

			assertNode(definition, "s4", QuestStatus.START, Map.of("var0", 4));
			assertNode(definition, "reward", QuestStatus.REWARD, Map.of("var0", 4));
			assertEquals(List.of(), definition.metadata().questWorkItems(),
				"the item must be consumed by the route, not by completion cleanup");
			assertEquals(1, rewardEntryCount(definition), "quest " + quest.questId()
				+ " must have exactly one non-REWARD route into REWARD");

			List<QuestTransition> routes = definition.transitions().stream()
				.filter(transition -> "s4".equals(transition.sourceNode()))
				.filter(transition -> "reward".equals(transition.targetNode()))
				.filter(transition -> transition.event() instanceof QuestEvent.UseItem use
					&& use.itemId() == quest.itemId())
				.toList();
			assertEquals(1, routes.size(), "quest " + quest.questId() + " must use its read item on s4");
			QuestTransition route = routes.getFirst();
			assertEquals(List.of(new QuestCondition.QuestVariableIs("var0", 4)), route.conditions());
			// 旧 handler 的 useQuestItem(..., true) 在使用时扣除 1 个道具。
			// The legacy useQuestItem(..., true) removes one item during use.
			assertEquals(List.of(new QuestAction.RemoveItem(quest.itemId(), 1)), route.actions());
			assertEquals(List.of(new AfterCommitAction.SyncQuestState(
				QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)), route.afterCommit());

			QuestMutationPlan plan = QuestMutationPlanner.plan(compiled,
				new QuestSnapshot(PLAYER_ID, quest.questId(), QuestStatus.START,
					definition.progressLayout().pack(Map.of("var0", 4)), Map.of(quest.itemId(), 1)),
				route.event(), route).orElseThrow(() -> new AssertionError(
					"quest " + quest.questId() + " blocked its item-use reward route"));
			assertEquals(QuestStatus.REWARD, plan.nextStatus());
			assertEquals(4, definition.progressLayout().unpack(plan.nextPackedVariables()).get("var0"));
			assertEquals(List.of(new QuestAction.RemoveItem(quest.itemId(), 1)), plan.requiredActions());
			assertEquals(route.afterCommit(), plan.afterCommit());
		}
	}

	private static void assertNode(QuestDefinition definition, String label, QuestStatus status,
			Map<String, Integer> variables) {
		QuestNode node = definition.nodes().stream()
			.filter(candidate -> label.equals(candidate.label()))
			.findFirst()
			.orElseThrow(() -> new AssertionError("missing quest node " + label));
		assertEquals(status, node.projection().status());
		assertEquals(variables, node.projection().variables());
	}

	private static long rewardEntryCount(QuestDefinition definition) {
		Map<String, QuestStatus> statuses = definition.nodes().stream()
			.collect(java.util.stream.Collectors.toMap(QuestNode::label, node -> node.projection().status()));
		return definition.transitions().stream()
			.filter(transition -> statuses.get(transition.sourceNode()) != QuestStatus.REWARD)
			.filter(transition -> statuses.get(transition.targetNode()) == QuestStatus.REWARD)
			.count();
	}

	private static CompiledQuestDefinition definition(int questId) throws Exception {
		try (InputStream input = Quest10100And20100ItemUseRemovalTest.class.getResourceAsStream(
			"/aion/data/static_data/quest_definition/quests/" + questId + ".xml")) {
			if (input == null) {
				throw new IllegalStateException("missing quest definition " + questId + ".xml");
			}
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
