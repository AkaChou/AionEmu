package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.AfterCommitAction;
import com.aionemu.gameserver.questEngine.definition.BitField;
import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
import com.aionemu.gameserver.questEngine.definition.QuestAction;
import com.aionemu.gameserver.questEngine.definition.QuestCondition;
import com.aionemu.gameserver.questEngine.definition.QuestDefinition;
import com.aionemu.gameserver.questEngine.definition.QuestDefinitionXmlCompiler;
import com.aionemu.gameserver.questEngine.definition.QuestDialogAction;
import com.aionemu.gameserver.questEngine.definition.QuestDialogPage;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.definition.QuestStateSyncMode;
import com.aionemu.gameserver.questEngine.definition.QuestTransition;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证旧 skill-use 模板迁移后的实时计数、客户端报告页和领奖状态流。
 * Verifies live counters, client report pages, and reward-state flow migrated from the legacy skill-use template.
 */
class QuestSkillUseProductionFlowTest {
	private static final List<QuestContract> CONTRACTS = List.of(
		new QuestContract(3910, 203707, "var0", 100, List.of(9912)),
		new QuestContract(3912, 203707, "var0", 20, List.of(890, 1100, 1699, 4646, 4004)),
		new QuestContract(3922, 203704, "var0", 10, List.of(599)),
		new QuestContract(3923, 203704, "var0", 10, List.of(3161)),
		new QuestContract(3924, 203705, "var0", 10, List.of(4610)),
		new QuestContract(3925, 203705, "var0", 10, List.of(3491)),
		new QuestContract(3926, 203706, "var0", 10, List.of(1338)),
		new QuestContract(3927, 203706, "var0", 10, List.of(3781)),
		new QuestContract(3928, 203707, "var0", 10, List.of(4220)),
		new QuestContract(3929, 203707, "var0", 10, List.of(1810)),
		new QuestContract(4931, 204059, "var0", 100, List.of(9912)),
		new QuestContract(4933, 204059, "var0", 20, List.of(890, 1100, 1699, 4646, 4004)),
		new QuestContract(4922, 204056, "var0", 10, List.of(599)),
		new QuestContract(4923, 204056, "var0", 10, List.of(3161)),
		new QuestContract(4924, 204057, "var0", 10, List.of(799)),
		new QuestContract(4925, 204057, "var0", 10, List.of(3491)),
		new QuestContract(4926, 204058, "var0", 10, List.of(1338)),
		new QuestContract(4927, 204058, "var0", 10, List.of(3781)),
		new QuestContract(4928, 204059, "var1", 10, List.of(4220)),
		new QuestContract(4929, 204059, "var0", 10, List.of(1810)),
		new QuestContract(19074, 801214, "var0", 10, List.of(2007)),
		new QuestContract(19075, 801215, "var0", 10, List.of(4469)),
		new QuestContract(19078, 801214, "var1", 10, List.of(2572)),
		new QuestContract(29074, 801222, "var1", 10, List.of(2007)),
		new QuestContract(29075, 801223, "var0", 10, List.of(4469)),
		new QuestContract(29078, 801222, "var1", 10, List.of(2572)));

	@TestFactory
	Stream<DynamicTest> preservesLiveCountersAndClientReportFlow() {
		return CONTRACTS.stream().map(contract -> DynamicTest.dynamicTest("quest " + contract.questId(),
			() -> assertContract(contract)));
	}

	private static void assertContract(QuestContract contract) throws Exception {
		CompiledQuestDefinition compiled = load(contract.questId());
		QuestDefinition definition = compiled.definition();
		assertEquals(4, definition.nodes().size());
		assertNode(definition, "unaccepted", QuestStatus.NONE, Map.of(contract.counterField(), 0));
		assertNode(definition, "started", QuestStatus.START, Map.of());
		assertNode(definition, "reward", QuestStatus.REWARD, Map.of());
		assertNode(definition, "complete", QuestStatus.COMPLETE, Map.of());

		assertEquals(1, definition.progressLayout().fields().size());
		BitField counter = definition.progressLayout().field(contract.counterField());
		assertEquals(0, counter.offset());
		assertEquals(0, counter.minValue());
		assertTrue(counter.maxValue() >= contract.required());

		for (int skillId : contract.skillIds()) {
			QuestTransition skill = transition(definition, "started", "started", new QuestEvent.UseSkill(skillId));
			assertEquals(List.of(new QuestCondition.VariableBelow(contract.counterField(), contract.required())),
				skill.conditions());
			assertEquals(List.of(new QuestAction.IncrementVariable(contract.counterField(), 1)), skill.actions());
			assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
				skill.afterCommit());
			assertNull(skill.priority());
		}

		QuestTransition finish = talk(definition, "started", "started", contract.npcId(),
			QuestDialogAction.FINISH_DIALOG);
		assertEmptyRoute(finish);
		assertEquals(List.of(new AfterCommitAction.ShowQuestSelectionDialog(QuestDialogPage.SELECT_QUEST.id())),
			finish.afterCommit());

		QuestTransition reportPage = talk(definition, "started", "started", contract.npcId(),
			QuestDialogAction.QUEST_SELECT);
		assertEmptyRoute(reportPage);
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.DEFAULT_SUCCESS.id())),
			reportPage.afterCommit());

		QuestTransition report = talk(definition, "started", "reward", contract.npcId(),
			QuestDialogAction.SELECT_QUEST_REWARD);
		assertEquals(List.of(new QuestCondition.VariableAtLeast(contract.counterField(), contract.required())),
			report.conditions());
		assertEquals(List.of(), report.actions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())),
			report.afterCommit());
		assertEquals(0, report.priority());

		QuestTransition notReady = talk(definition, "started", "started", contract.npcId(),
			QuestDialogAction.SELECT_QUEST_REWARD);
		assertEquals(List.of(), notReady.conditions());
		assertEquals(List.of(), notReady.actions());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.DEFAULT_SUCCESS.id())),
			notReady.afterCommit());
		assertEquals(1, notReady.priority());

		assertCounterRuntime(compiled, contract, report, notReady);
		assertFalse(definition.transitions().stream().anyMatch(candidate -> candidate.event().equals(
			new QuestEvent.TalkToNpc(contract.npcId(), QuestDialogAction.SETPRO3.id()))));
	}

	private static void assertCounterRuntime(CompiledQuestDefinition compiled, QuestContract contract,
			QuestTransition report, QuestTransition notReady) {
		QuestTransition skill = transition(compiled.definition(), "started", "started",
			new QuestEvent.UseSkill(contract.skillIds().getFirst()));
		int packed = compiled.definition().progressLayout().pack(Map.of(contract.counterField(), 0));
		for (int count = 0; count < contract.required(); count++) {
			QuestSnapshot snapshot = snapshot(contract.questId(), packed);
			QuestMutationPlan plan = QuestMutationPlanner.plan(compiled, snapshot, skill.event(), skill).orElseThrow();
			assertEquals(QuestStatus.START, plan.nextStatus());
			packed = plan.nextPackedVariables();
		}
		assertEquals(Map.of(contract.counterField(), contract.required()),
			compiled.definition().progressLayout().unpack(packed));
		assertTrue(QuestMutationPlanner.plan(compiled, snapshot(contract.questId(), packed), skill.event(), skill)
			.isEmpty());

		int beforeRequired = compiled.definition().progressLayout().pack(
			Map.of(contract.counterField(), contract.required() - 1));
		QuestEvent reportEvent = new QuestEvent.TalkToNpc(contract.npcId(),
			QuestDialogAction.SELECT_QUEST_REWARD.id());
		assertTrue(QuestMutationPlanner.plan(compiled, snapshot(contract.questId(), beforeRequired), reportEvent,
			notReady).isPresent());
		assertTrue(QuestMutationPlanner.plan(compiled, snapshot(contract.questId(), beforeRequired), reportEvent,
			report).isEmpty());

		QuestMutationPlan reportPlan = QuestMutationPlanner.plan(compiled, snapshot(contract.questId(), packed),
			reportEvent, report).orElseThrow();
		assertEquals(QuestStatus.REWARD, reportPlan.nextStatus());
		assertEquals(Map.of(contract.counterField(), contract.required()),
			compiled.definition().progressLayout().unpack(reportPlan.nextPackedVariables()));
	}

	private static QuestSnapshot snapshot(int questId, int packedVariables) {
		return new QuestSnapshot(7, questId, QuestStatus.START, packedVariables, Map.of());
	}

	private static void assertNode(QuestDefinition definition, String label, QuestStatus status,
			Map<String, Integer> variables) {
		var node = definition.nodes().stream()
			.filter(candidate -> candidate.label().equals(label))
			.findFirst().orElseThrow();
		assertEquals(status, node.projection().status());
		assertEquals(variables, node.projection().variables());
	}

	private static void assertEmptyRoute(QuestTransition transition) {
		assertEquals(List.of(), transition.conditions());
		assertEquals(List.of(), transition.actions());
		assertNull(transition.priority());
	}

	private static QuestTransition talk(QuestDefinition definition, String source, String target, int npcId,
			QuestDialogAction action) {
		return transition(definition, source, target, new QuestEvent.TalkToNpc(npcId, action.id()));
	}

	private static QuestTransition transition(QuestDefinition definition, String source, String target,
			QuestEvent event) {
		List<QuestTransition> matches = definition.transitions().stream()
			.filter(candidate -> candidate.sourceNode().equals(source))
			.filter(candidate -> candidate.targetNode().equals(target))
			.filter(candidate -> candidate.event().equals(event))
			.toList();
		assertEquals(1, matches.size(), () -> source + " -> " + target + " " + event);
		return matches.getFirst();
	}

	private static CompiledQuestDefinition load(int questId) throws Exception {
		try (InputStream input = QuestSkillUseProductionFlowTest.class.getResourceAsStream(
				"/aion/data/static_data/quest_definition/quests/" + questId + ".xml")) {
			if (input == null) {
				throw new IllegalStateException("missing quest definition " + questId + ".xml");
			}
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}

	/**
	 * 保存每个技能计数任务由旧模板证明的差异字段。
	 * Holds the legacy-proven fields that differ across skill-use quests.
	 */
	private record QuestContract(int questId, int npcId, String counterField, int required,
			List<Integer> skillIds) {
	}
}
