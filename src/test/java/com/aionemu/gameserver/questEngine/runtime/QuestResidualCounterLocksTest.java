package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.AfterCommitAction;
import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
import com.aionemu.gameserver.questEngine.definition.QuestAction;
import com.aionemu.gameserver.questEngine.definition.QuestCondition;
import com.aionemu.gameserver.questEngine.definition.QuestDefinitionXmlCompiler;
import com.aionemu.gameserver.questEngine.definition.QuestDialogAction;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.definition.QuestNode;
import com.aionemu.gameserver.questEngine.definition.QuestStateSyncMode;
import com.aionemu.gameserver.questEngine.definition.QuestTransition;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 锁定 E2E 报告刷新后剩余 NO_MATCH/TRANSACTION_FAILURE 批次的修复合同：
 * 25324 的 h3 与 17541 的 started 不再投影实时计数字段；28504/15322/25322 的阶段完成边
 * 不再对已达阈值的字段执行越界自增。
 * Locks the repair contract for the residual NO_MATCH/TRANSACTION_FAILURE batch found after the
 * E2E report refresh: 25324 h3 and 17541 started no longer project live counter fields, and the
 * stage-completion edges of 28504/15322/25322 no longer over-increment fields past their threshold.
 */
class QuestResidualCounterLocksTest {

	@Test
	void quest25324FinalStageReportsWithoutVar1ProjectionLock() throws Exception {
		CompiledQuestDefinition definition = load(25324);
		assertEquals(Map.of("var0", 3), node(definition, "h3").projection().variables());

		QuestTransition report = definition.definition().transitions().stream()
			.filter(candidate -> candidate.event().equals(
				new QuestEvent.TalkToNpc(805343, QuestDialogAction.SELECT_QUEST_REWARD.id())))
			.filter(candidate -> "h3".equals(candidate.sourceNode()))
			.findFirst().orElseThrow();
		assertEquals(List.of(new QuestCondition.VariableAtLeast("var1", 20)), report.conditions());

		try (QuestE2eRuntime runtime = new QuestE2eRuntime(definition)) {
			runtime.prepare(report);
			assertTrue(runtime.dispatchPrepared().handled());
			assertEquals(QuestStatus.REWARD, runtime.state().status());
		}
	}

	@Test
	void quest17541CountsThreeIndependentTargetsInAnyOrder() throws Exception {
		CompiledQuestDefinition definition = load(17541);
		assertEquals(Map.of(), node(definition, "started").projection().variables());

		QuestTransition firstCounting = definition.definition().transitions().stream()
			.filter(candidate -> candidate.event().equals(new QuestEvent.KillNpc(217195)))
			.filter(candidate -> candidate.priority() != null && candidate.priority() == 3)
			.findFirst().orElseThrow();
		try (QuestE2eRuntime runtime = new QuestE2eRuntime(definition)) {
			runtime.prepare(firstCounting);
			dispatchKill(runtime, 217195);
			assertEquals(Map.of("var0", 0, "var1", 1, "var2", 0), variables(definition, runtime));
			dispatchKill(runtime, 217185);
			assertEquals(Map.of("var0", 1, "var1", 1, "var2", 0), variables(definition, runtime));
			dispatchKill(runtime, 217204);
			assertEquals(QuestStatus.START, runtime.state().status());
			assertEquals(Map.of("var0", 1, "var1", 1, "var2", 1), variables(definition, runtime));
		}
	}

	@Test
	void quest28504FinalKillEntersRewardWithoutOutOfRangeIncrement() throws Exception {
		CompiledQuestDefinition definition = load(28504);
		QuestTransition counting = killRoute(definition, "hunting");
		QuestTransition finishing = killRoute(definition, "reward");
		assertEquals(List.of(new QuestCondition.VariableBelow("var0", 65)), counting.conditions());
		assertEquals(List.of(new QuestAction.IncrementVariable("var0", 1)), counting.actions());
		assertEquals(List.of(new QuestCondition.VariableAtLeast("var0", 65)), finishing.conditions());
		assertEquals(List.of(new QuestAction.SetVariable("var0", 65)), finishing.actions());

		try (QuestE2eRuntime runtime = new QuestE2eRuntime(definition)) {
			runtime.prepare(counting);
			assertTrue(runtime.dispatchWorld(new QuestEvent.KillNpc(216897)).handled());
			assertEquals(QuestStatus.START, runtime.state().status());
			runtime.prepare(finishing);
			assertTrue(runtime.dispatchPrepared().handled());
			assertEquals(QuestStatus.REWARD, runtime.state().status());
			assertEquals(Map.of("var0", 65), variables(definition, runtime));
		}
	}

	@Test
	void quests15322And25322StageEdgesResetCountersWithoutOverIncrement() throws Exception {
		for (int questId : new int[] {15322, 25322}) {
			CompiledQuestDefinition definition = load(questId);
			List<QuestTransition> stageEdges = definition.definition().transitions().stream()
				.filter(candidate -> candidate.actions().stream()
					.anyMatch(action -> action instanceof QuestAction.SetVariable set
						&& "var1".equals(set.field()) && set.value() == 0))
				.toList();
			assertEquals(4, stageEdges.size(), "quest " + questId + " stage edge count");
			for (QuestTransition edge : stageEdges) {
				assertTrue(edge.actions().stream()
						.noneMatch(action -> action instanceof QuestAction.IncrementVariable increment
							&& "var1".equals(increment.field())),
					"quest " + questId + " stage edge still increments var1 past its threshold");
			}
		}
	}

	private static void dispatchKill(QuestE2eRuntime runtime, int npcId) {
		assertTrue(runtime.dispatchWorld(new QuestEvent.KillNpc(npcId)).handled(),
			"kill " + npcId + " was not handled");
	}

	private static QuestTransition killRoute(CompiledQuestDefinition definition, String targetNode) {
		return definition.definition().transitions().stream()
			.filter(candidate -> candidate.event() instanceof QuestEvent.KillNpcSet)
			.filter(candidate -> candidate.targetNode().equals(targetNode))
			.findFirst().orElseThrow();
	}

	private static Map<String, Integer> variables(CompiledQuestDefinition definition, QuestE2eRuntime runtime) {
		return definition.definition().progressLayout().unpack(runtime.state().packedVariables());
	}

	private static QuestNode node(CompiledQuestDefinition definition, String label) {
		return definition.definition().nodes().stream()
			.filter(candidate -> candidate.label().equals(label))
			.findFirst().orElseThrow();
	}

	private static CompiledQuestDefinition load(int questId) throws Exception {
		String resource = "/aion/data/static_data/quest_definition/quests/" + questId + ".xml";
		try (InputStream input = Objects.requireNonNull(
				QuestResidualCounterLocksTest.class.getResourceAsStream(resource), resource)) {
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
