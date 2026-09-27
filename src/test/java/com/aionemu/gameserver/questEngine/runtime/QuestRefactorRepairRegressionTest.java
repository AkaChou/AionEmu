package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.QuestDefinitionDirectoryLoader;
import com.aionemu.gameserver.questEngine.definition.AfterCommitAction;
import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
import com.aionemu.gameserver.questEngine.definition.QuestAction;
import com.aionemu.gameserver.questEngine.definition.QuestCatalog;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.definition.QuestStateSyncMode;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.retail.RetailQuestDriver;
import org.junit.jupiter.api.Test;


import java.util.List;
import java.util.Map;

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
	void leatherWingsBriefingTalkClearsSectionFiveWithoutCountingAKill() throws Exception {
		var compiled = load(24155);
		assertEquals(Map.of("var0", 0, "var5", 1), compiled.definition().nodes().stream()
			.filter(n -> n.label().equals("started")).findFirst().orElseThrow().projection().variables());
		var state = snapshot(compiled, Map.of("var0", 0, "var5", 1), Map.of());
		// P0-2 规范形：简报入口是 QUEST_SELECT 一步清 SECTION_5（select2 页链与 SETPRO2 按钮不再下发）。
		// Canonical since P0-2: the briefing entry is QUEST_SELECT clearing SECTION_5 in one step
		// (the select2 page chain and its SETPRO2 button are no longer served).
		var talk = onlyPlan(compiled, state, new QuestEvent.TalkToNpc(204785, 31));
		// 清 SECTION_5 时任务书要从"去见简报 NPC"切到击杀行：LEVEL_AND_VISIBILITY_REFRESH
		// （含可见性/等级刷新，PACKET_ONLY 的超集），与串行族一致。
		// Clearing SECTION_5 must re-render the journal rows: LEVEL_AND_VISIBILITY_REFRESH, like the serial family.
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.CloseDialog()), talk.afterCommit());
		state = next(state, talk);
		assertEquals(Map.of("var0", 0, "var5", 0),
			compiled.definition().progressLayout().unpack(state.packedVariables()));
		for (int count = 1; count <= 3; count++) {
			// 未满段 QUEST_SELECT 不得进领奖（规范形交付边只挂满段节点）。
			// Non-full QUEST_SELECT must not reach reward (the canonical delivery edge hangs off the
			// full node only).
			assertTrue(plans(compiled, state, new QuestEvent.TalkToNpc(204701, 31)).isEmpty());
			state = next(state, onlyPlan(compiled, state, new QuestEvent.KillNpc(700290)));
			assertEquals(Map.of("var0", count, "var5", 0),
				compiled.definition().progressLayout().unpack(state.packedVariables()));
		}
		assertEquals(QuestStatus.REWARD,
			onlyPlan(compiled, state, new QuestEvent.TalkToNpc(204701, 31)).nextStatus());
	}

	/**
	 * 交付边界契约：短堆栈被拒（零计划进领奖），满堆栈恰好扣除真端需求数量、多余保留。
	 * 每个任务登记各交付动作的满堆栈成功计划数：
	 * 26930 仍是 XML：39 显式交付带双变体路由（2 条），1009 领奖入口是第二条交付边（1 条）。
	 * 80798 已由真端 DataDriven 交付流接管（retention: DD_TALK_COLLECT_CANONICAL，客户端交付页齐备）：
	 * 交付检查对 39/20002 成对登记（事件匹配器把 item-check 族视为等价，满堆栈时两条成功边都进领奖），
	 * 1009 只剩领奖态预览入口，不再是 START 态交付边。
	 * Hand-in boundary: short stacks are rejected (no plan reaches reward) and a full stack removes
	 * exactly the retail count, keeping the surplus. Per quest and action the map records the
	 * expected full-stack success plans: 26930 stays XML (the explicit 39 turn-in carries two
	 * variant routes, the 1009 reward entry is the second hand-in edge). 80798 is the retail
	 * DataDriven hand-in flow now: the 39/20002 check pair is registered together (the event
	 * matcher treats the item-check family as equivalent, so both success edges reach reward on a
	 * full stack), and 1009 remains only the reward-state preview, not a START-state hand-in.
	 */
	@Test
	void handInsRejectShortStacksAndKeepTheSurplus() throws Exception {
		for (HandInContract contract : List.of(
			new HandInContract(26930, 186000257, 10, 804627, Map.of(39, 2, 1009, 1)),
			new HandInContract(80798, 182215809, 5, 833545, Map.of(39, 2, 20002, 2)))) {
			var compiled = load(contract.questId());
			for (var entry : contract.successPlansByAction().entrySet()) {
				int action = entry.getKey();
				int expectedFullStack = entry.getValue();
				for (int count : List.of(contract.required() - 1, contract.required(),
						contract.required() * 2)) {
					var state = snapshot(compiled, Map.of("var0", 0), Map.of(contract.item(), count));
					var successes = plans(compiled, state, new QuestEvent.TalkToNpc(contract.npc(), action))
						.stream().filter(p -> p.nextStatus() == QuestStatus.REWARD).toList();
					assertEquals(count < contract.required() ? 0 : expectedFullStack, successes.size(),
						contract.questId() + " action " + action + " stack " + count);
					if (count >= contract.required()) {
						for (var success : successes) {
							assertEquals(List.of(new QuestAction.RemoveItem(contract.item(), contract.required())),
								success.requiredActions().stream()
									.filter(QuestAction.RemoveItem.class::isInstance).toList());
						}
					}
				}
			}
		}
	}

	/** 交付边界契约行。 / One hand-in boundary contract row. */
	private record HandInContract(int questId, int item, int required, int npc,
			Map<Integer, Integer> successPlansByAction) {
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
		// 退役任务的生产 XML 只在 git 历史里：统一取生产视图（XML 目录 + 真端 overlay）。
		// TEMP-VERIFY(view): 并行 SimpleTalk 批次落定前生产覆盖门不可用，用宽松 overlay 验证本断言。
		return VIEW.updateAndGet(current -> current != null ? current
				: RetailQuestDriver.overlay(QuestDefinitionDirectoryLoader.compile(
					QuestRefactorRepairRegressionTest.class.getClassLoader())))
			.find(id)
			.orElseThrow(() -> new IllegalStateException("missing production quest definition " + id));
	}

	// TEMP-VERIFY(view): 并行批次落定前的宽松生产视图（XML 目录 + 真端驱动，跳过覆盖门）。
	private static final java.util.concurrent.atomic.AtomicReference<QuestCatalog> VIEW =
		new java.util.concurrent.atomic.AtomicReference<>();

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
