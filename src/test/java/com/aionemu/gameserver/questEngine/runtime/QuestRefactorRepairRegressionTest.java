package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.AfterCommitAction;
import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
import com.aionemu.gameserver.questEngine.definition.QuestAction;
import com.aionemu.gameserver.questEngine.definition.QuestDefinitionXmlCompiler;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.definition.QuestStateSyncMode;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 使用真实编译定义验证改造后的计数、客户端位域和交付边界。
 * Exercises compiled production counters, client sections and hand-in boundaries after migration.
 */
class QuestRefactorRepairRegressionTest {
	@Test
	void repeatedKillsDoNotLockAfterTheFirstEvent() throws Exception {
		for (int id : List.of(13945, 18994, 28994)) {
			var compiled = load(id);
			var state = snapshot(compiled, Map.of("var0", 1, "var1", 0), Map.of());
			int required = id == 13945 ? 2 : 3;
			for (int count = 1; count <= required; count++) {
				var plan = onlyPlan(compiled, state, new QuestEvent.KillNpc(id == 13945 ? 884544 : 857974));
				state = next(state, plan);
				assertEquals(count == required && id != 13945 ? 0 : count,
					compiled.definition().progressLayout().unpack(state.packedVariables()).get("var1"));
				assertEquals(count == required && id == 13945 ? QuestStatus.REWARD : QuestStatus.START,
					state.status());
			}
		}
	}

	@Test
	void leatherWingsDialogClearsSectionFiveWithoutCountingAKill() throws Exception {
		var compiled = load(24155);
		assertEquals(Map.of("var0", 0, "var5", 1), compiled.definition().nodes().stream()
			.filter(n -> n.label().equals("started")).findFirst().orElseThrow().projection().variables());
		var state = snapshot(compiled, Map.of("var0", 0, "var5", 1), Map.of());
		var talk = onlyPlan(compiled, state, new QuestEvent.TalkToNpc(204785, 10001));
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
			new AfterCommitAction.CloseDialog()), talk.afterCommit());
		state = next(state, talk);
		assertEquals(Map.of("var0", 0, "var5", 0),
			compiled.definition().progressLayout().unpack(state.packedVariables()));
		for (int count = 1; count <= 3; count++) {
			assertTrue(plans(compiled, state, new QuestEvent.TalkToNpc(204701, 1009)).isEmpty());
			state = next(state, onlyPlan(compiled, state, new QuestEvent.KillNpc(700290)));
			assertEquals(Map.of("var0", count, "var5", 0),
				compiled.definition().progressLayout().unpack(state.packedVariables()));
		}
		assertEquals(QuestStatus.REWARD,
			onlyPlan(compiled, state, new QuestEvent.TalkToNpc(204701, 1009)).nextStatus());
	}

	@Test
	void handInsRejectShortStacksAndKeepTheSurplus() throws Exception {
		for (int id : List.of(80798, 26930)) {
			var compiled = load(id);
			int item = id == 80798 ? 182215809 : 186000257;
			int required = id == 80798 ? 5 : 10;
			int npc = id == 80798 ? 833545 : 804627;
			for (int action : List.of(39, 1009)) {
				for (int count : List.of(required - 1, required, required * 2)) {
					var state = snapshot(compiled, Map.of("var0", 0), Map.of(item, count));
					var successes = plans(compiled, state, new QuestEvent.TalkToNpc(npc, action)).stream()
						.filter(p -> p.nextStatus() == QuestStatus.REWARD).toList();
					int expectedSuccesses = count < required ? 0 : (id == 26930 && action == 39 ? 2 : 1);
					assertEquals(expectedSuccesses, successes.size(), id + " action " + action);
					if (count >= required) {
						for (var success : successes) {
							assertEquals(List.of(new QuestAction.RemoveItem(item, required)),
								success.requiredActions().stream()
									.filter(QuestAction.RemoveItem.class::isInstance).toList());
						}
					}
				}
			}
		}
	}

	@Test
	void namusContinuationKeepsStateAndReturnsTheActualClientPage() throws Exception {
		var compiled = load(1917);
		var state = snapshot(compiled, Map.of("var0", 0), Map.of());
		var plan = onlyPlan(compiled, state, new QuestEvent.TalkToNpc(203075, 1354));
		assertEquals(state.status(), plan.nextStatus());
		assertEquals(state.packedVariables(), plan.nextPackedVariables());
		assertEquals(List.of(), plan.requiredActions());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(1354)), plan.afterCommit());
	}

	private static CompiledQuestDefinition load(int id) throws Exception {
		try (var input = Objects.requireNonNull(QuestRefactorRepairRegressionTest.class.getResourceAsStream(
				"/aion/data/static_data/quest_definition/quests/" + id + ".xml"))) {
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}

	private static QuestSnapshot snapshot(CompiledQuestDefinition compiled, Map<String, Integer> variables,
			Map<Integer, Integer> inventory) {
		return new QuestSnapshot(7, compiled.id(), QuestStatus.START,
			compiled.definition().progressLayout().pack(variables), inventory);
	}

	private static QuestSnapshot next(QuestSnapshot before, QuestMutationPlan plan) {
		return new QuestSnapshot(before.playerId(), before.questId(), plan.nextStatus(),
			plan.nextPackedVariables(), before.inventory());
	}

	private static List<QuestMutationPlan> plans(CompiledQuestDefinition compiled, QuestSnapshot state,
			QuestEvent event) {
		return compiled.definition().transitions().stream()
			.flatMap(t -> QuestMutationPlanner.plan(compiled, state, event, t).stream()).toList();
	}

	private static QuestMutationPlan onlyPlan(CompiledQuestDefinition compiled, QuestSnapshot state,
			QuestEvent event) {
		var plans = plans(compiled, state, event);
		assertEquals(1, plans.size(), compiled.id() + " " + event);
		return plans.getFirst();
	}
}
