package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlan;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlanner;
import com.aionemu.gameserver.questEngine.runtime.QuestSnapshot;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 验证 11468/21468 的三段使用技能计数在最后一击直接进入 REWARD，并保留已持久化满计数的对话恢复路径。
 * Verifies that quests 11468/21468 enter REWARD on the final use-skill event and keep a report-recovery route for persisted full counters.
 */
class Quest11468And21468SkillCompletionTest {
	private static final int REPORT_NPC_ID = 799503;
	private static final List<Integer> QUEST_IDS = List.of(11468, 21468);
	private static final List<CounterCase> COUNTERS = List.of(
		new CounterCase("var1", 10, 9832,
			List.of(new OtherCounter("var2", 5), new OtherCounter("var3", 3))),
		new CounterCase("var2", 5, 9833,
			List.of(new OtherCounter("var1", 10), new OtherCounter("var3", 3))),
		new CounterCase("var3", 3, 9834,
			List.of(new OtherCounter("var1", 10), new OtherCounter("var2", 5))));

	@Test
	void finalItemSkillEntersRewardAndPersistedFullCountersCanReport() throws Exception {
		for (int questId : QUEST_IDS) {
			CompiledQuestDefinition compiled = load(questId);
			QuestDefinition definition = compiled.definition();
			assertNode(definition, "started", QuestStatus.START, Map.of());
			assertNode(definition, "reward", QuestStatus.REWARD, Map.of());

			for (CounterCase counter : COUNTERS) {
				QuestEvent event = new QuestEvent.UseSkill(counter.skillId());
				QuestTransition continuing = transition(definition, "started", event, 1);
				assertEquals(List.of(new QuestCondition.VariableBelow(counter.field(), counter.required())),
					continuing.conditions());
				assertEquals(List.of(new QuestAction.IncrementVariable(counter.field(), 1)), continuing.actions());
				assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
					continuing.afterCommit());

				QuestTransition completing = transition(definition, "reward", event, 0);
				List<QuestCondition> expectedConditions = new ArrayList<>();
				expectedConditions.add(new QuestCondition.QuestVariableIs(counter.field(), counter.required() - 1));
				for (OtherCounter other : counter.others()) {
					expectedConditions.add(new QuestCondition.VariableAtLeast(other.field(), other.required()));
				}
				assertEquals(expectedConditions, completing.conditions());
				assertEquals(List.of(new QuestAction.IncrementVariable(counter.field(), 1)), completing.actions());
				assertEquals(List.of(new AfterCommitAction.SyncQuestState(
					QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)), completing.afterCommit());

				assertCounterUse(compiled, counter, false);
				assertCounterUse(compiled, counter, true);
			}

			QuestTransition recoveredReport = transition(definition, "reward",
				new QuestEvent.TalkToNpc(REPORT_NPC_ID, QuestDialogAction.SELECT_QUEST_REWARD.id()), null);
			assertEquals(List.of(
				new QuestCondition.VariableAtLeast("var1", 10),
				new QuestCondition.VariableAtLeast("var2", 5),
				new QuestCondition.VariableAtLeast("var3", 3)), recoveredReport.conditions());
			assertEquals(List.of(), recoveredReport.actions());
			assertEquals(List.of(
				new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
				new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())),
				recoveredReport.afterCommit());
		}
	}

	private static void assertCounterUse(CompiledQuestDefinition compiled, CounterCase counter, boolean finalUse) {
		ProgressLayout layout = compiled.definition().progressLayout();
		Map<String, Integer> variables = new LinkedHashMap<>();
		for (OtherCounter other : counter.others()) {
			variables.put(other.field(), other.required());
		}
		variables.put(counter.field(), finalUse ? counter.required() - 1 : counter.required() - 2);

		QuestSnapshot snapshot = new QuestSnapshot(7, compiled.id(), QuestStatus.START,
			layout.pack(variables), Map.of());
		QuestTransition route = transition(compiled.definition(), finalUse ? "reward" : "started",
			new QuestEvent.UseSkill(counter.skillId()), finalUse ? 0 : 1);
		QuestMutationPlan plan = QuestMutationPlanner.plan(compiled, snapshot, route.event(), route).orElseThrow();

		assertEquals(finalUse ? QuestStatus.REWARD : QuestStatus.START, plan.nextStatus());
		variables.put(counter.field(), finalUse ? counter.required() : counter.required() - 1);
		assertEquals(variables, layout.unpack(plan.nextPackedVariables()));
	}

	private static QuestTransition transition(QuestDefinition definition, String target, QuestEvent event,
			Integer priority) {
		List<QuestTransition> matches = definition.transitions().stream()
			.filter(candidate -> candidate.sourceNode().equals("started"))
			.filter(candidate -> candidate.targetNode().equals(target))
			.filter(candidate -> candidate.event().equals(event))
			.filter(candidate -> Objects.equals(candidate.priority(), priority))
			.toList();
		assertEquals(1, matches.size(), () -> "expected one transition started -> " + target + " "
			+ event + " priority " + priority);
		return matches.getFirst();
	}

	private static void assertNode(QuestDefinition definition, String label, QuestStatus status,
			Map<String, Integer> variables) {
		QuestNode node = definition.nodes().stream()
			.filter(candidate -> candidate.label().equals(label))
			.findFirst().orElseThrow();
		assertEquals(status, node.projection().status());
		assertEquals(variables, node.projection().variables());
	}

	private static CompiledQuestDefinition load(int questId) throws Exception {
		try (InputStream input = Quest11468And21468SkillCompletionTest.class.getResourceAsStream(
			"/aion/data/static_data/quest_definition/quests/" + questId + ".xml")) {
			if (input == null) {
				throw new IllegalStateException("missing quest definition " + questId + ".xml");
			}
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}

	private record CounterCase(String field, int required, int skillId, List<OtherCounter> others) {
	}

	private record OtherCounter(String field, int required) {
	}
}
