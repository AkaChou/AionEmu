package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.QuestDefinitionDirectoryLoader;
import com.aionemu.gameserver.questEngine.definition.AfterCommitAction;
import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
import com.aionemu.gameserver.questEngine.definition.QuestCatalog;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;


import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 使用真实编译定义验证改造后的计数、客户端位域和交付边界。
 * <p>
 * 2026-10-05 清扫：24155（皮革翅膀简报清 SECTION_5 法）主语已退役（retention: RETAIL_TABLE）——
 * IR 不复存在，方法随其车道退场（P7/P8 先例：退役任务的 IR 断言不保留；阶梯语义由 native 车道
 * 门承担）。残余方法只服务 XML 保留行。
 * Exercises compiled production counters, client sections and hand-in boundaries after migration.
 * The 24155 method retired with its quest (retention: RETAIL_TABLE): retail-driven rows have no IR,
 * so the IR assertion leaves with the lane (the P7/P8 precedent). The remaining method only serves
 * XML-retained quests.
 */
class QuestRefactorRepairRegressionTest {

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
		// XML 保留行直接取 XML 目录编译产物（退役任务无 IR，见类注释）。
		// XML-retained rows come straight from the XML directory compile (retired rows have no IR).
		return CATALOG.updateAndGet(current -> current != null ? current
				: QuestDefinitionDirectoryLoader.compile(QuestRefactorRepairRegressionTest.class.getClassLoader()))
			.findExecutable(id)
			.orElseThrow(() -> new IllegalStateException("quest " + id + " is not XML-retained"));
	}

	private static final java.util.concurrent.atomic.AtomicReference<QuestCatalog> CATALOG =
		new java.util.concurrent.atomic.AtomicReference<>();

	private static QuestSnapshot snapshot(CompiledQuestDefinition compiled, Map<String, Integer> variables,
			Map<Integer, Integer> inventory) {
		return new QuestSnapshot(7, compiled.id(), QuestStatus.START,
			compiled.definition().progressLayout().pack(variables), inventory);
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
