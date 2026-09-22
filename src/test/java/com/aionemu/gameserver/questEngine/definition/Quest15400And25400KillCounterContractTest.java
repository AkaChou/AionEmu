package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlan;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlanner;
import com.aionemu.gameserver.questEngine.runtime.QuestSnapshot;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.io.InputStream;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证多杀任务自环累加、收口推进与跨阶段计数器清零契约。
 * Verifies multi-kill counters: self-loop accumulation, closure advancement, and the
 * cross-stage counter reset contract.
 * <p>覆盖两类历史手写缺陷：
 * <ul>
 *   <li>收口条件阈值配置过小（A &lt; required - 1）导致“少杀怪就提前跳步”。</li>
 *   <li>收口推进不清零本阶段局部计数（QE-044），把非计数阶段的打包步数污染成
 *       {@code (var1&lt;&lt;6)|step}；客户端只在 {@code progress == step} 时发起任务对白，
 *       因此报告 NPC 头顶无标记、点击只下发 questId=0 的通用第 10 页。15400 的
 *       佩里埃尔(805355) 正是此形态。</li>
 * </ul>
 * Counter-threshold drift and the missing counter reset both break the packed step; a
 * polluted value makes the client skip the quest-specific dialog request. All contracts
 * must keep the closure stage pure and provide a login self-heal for polluted saves.</p>
 */
class Quest15400And25400KillCounterContractTest {

	/** 计数阶段的污染目标：收口后仍可携带脏计数的节点与其阶段索引。 */
	private record CountingStage(String node, int stage) {
	}

	private record QuestContract(int questId, String sourceNode, String targetNode, int killTargetNpc,
			int stageKill, int stageNext, int requiredKills, String countingEntrySource,
			List<CountingStage> pollutedStages) {
	}

	private static final List<QuestContract> CONTRACTS = List.of(
		new QuestContract(15400, "s6", "s7", 885101, 6, 7, 2, "s5",
			List.of(new CountingStage("s7", 7), new CountingStage("s8", 8), new CountingStage("reward", 9))),
		new QuestContract(25400, "s6", "s7", 885101, 6, 7, 2, "s5",
			List.of(new CountingStage("s7", 7), new CountingStage("s8", 8), new CountingStage("reward", 9))),
		new QuestContract(15604, "s1", "s2", 241161, 1, 2, 5, "started",
			List.of(new CountingStage("s2", 2), new CountingStage("s3", 3), new CountingStage("s4", 4),
				new CountingStage("reward", 5))),
		new QuestContract(16821, "s2", "s3", 220458, 2, 3, 35, "s1",
			List.of(new CountingStage("s3", 3), new CountingStage("s4", 4), new CountingStage("s5", 5),
				new CountingStage("reward", 6))),
		new QuestContract(26821, "s2", "s3", 220458, 2, 3, 35, "s1",
			List.of(new CountingStage("s3", 3), new CountingStage("s4", 4), new CountingStage("s5", 5),
				new CountingStage("reward", 6)))
	);

	@TestFactory
	Stream<DynamicTest> killCounterRequiresExactlyFullKillsToAdvance() {
		return CONTRACTS.stream().map(contract -> DynamicTest.dynamicTest("quest " + contract.questId(),
			() -> assertKillCounterContract(contract)));
	}

	private static void assertKillCounterContract(QuestContract contract) {
		int questId = contract.questId();
		CompiledQuestDefinition compiled = load(questId);
		QuestDefinition definition = compiled.definition();
		ProgressLayout layout = definition.progressLayout();

		BitField var0 = layout.field("var0");
		BitField var1 = layout.field("var1");
		assertEquals(0, var0.offset(), "阶段必须占用客户端任务说明行索引读取的 SECTION_0");
		assertEquals(6, var1.offset(), "击杀计数必须留在 SECTION_1（offset 6）");
		assertEquals(contract.requiredKills(), var1.maxValue(),
			() -> "quest " + questId + " 击杀计数器 maxValue 必须与客户端门控一致");

		QuestEvent killEvent = new QuestEvent.KillNpc(contract.killTargetNpc());
		AfterCommitAction packetSync = new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY);

		// 1. 结构断言：自环转移只负责累加，收口转移必须把计数器清回 0（QE-044）。
		QuestTransition selfLoop = definition.transitions().stream()
			.filter(t -> contract.sourceNode().equals(t.sourceNode()) && contract.sourceNode().equals(t.targetNode()))
			.filter(t -> killEvent.equals(t.event()))
			.findFirst()
			.orElseThrow(() -> new AssertionError("quest " + questId + " 缺失自环转移"));
		assertEquals(1, selfLoop.priority(), "自环转移 priority 应为 1");
		assertTrue(selfLoop.conditions().contains(new QuestCondition.VariableBelow("var1", contract.requiredKills() - 1)),
			() -> "quest " + questId + " 自环转移必须在 var1 < " + (contract.requiredKills() - 1) + " 时匹配");
		assertEquals(List.of(new QuestAction.IncrementVariable("var1", 1)), selfLoop.actions(),
			() -> "quest " + questId + " 自环转移只允许累加 var1");
		assertEquals(List.of(packetSync), selfLoop.afterCommit(),
			() -> "quest " + questId + " 自环转移的 after-commit 必须只有 PACKET_ONLY 同步");

		QuestTransition completing = definition.transitions().stream()
			.filter(t -> contract.sourceNode().equals(t.sourceNode()) && contract.targetNode().equals(t.targetNode()))
			.filter(t -> killEvent.equals(t.event()))
			.findFirst()
			.orElseThrow(() -> new AssertionError("quest " + questId + " 缺失收口推进转移"));
		assertEquals(0, completing.priority(), "收口转移 priority 应为 0");
		assertTrue(completing.conditions().contains(new QuestCondition.VariableAtLeast("var1", contract.requiredKills() - 1)),
			() -> "quest " + questId + " 收口转移必须在 var1 >= " + (contract.requiredKills() - 1) + " 时匹配");
		assertEquals(List.of(new QuestAction.SetVariable("var1", 0)), completing.actions(),
			() -> "quest " + questId + " 收口转移必须清空局部计数，禁止把 (var1<<6)|step 带进下一阶段");
		assertEquals(List.of(packetSync), completing.afterCommit(),
			() -> "quest " + questId + " 收口转移的 after-commit 必须只有 PACKET_ONLY 同步");

		// 2. 结构断言：进入客户端声明的计数阶段时计数器必须从 0 起算。
		QuestTransition countingEntry = definition.transitions().stream()
			.filter(t -> contract.countingEntrySource().equals(t.sourceNode())
				&& contract.stageKill() == nodeStage(definition, t.targetNode()))
			.findFirst()
			.orElseThrow(() -> new AssertionError("quest " + questId + " 缺失进入计数阶段的转移"));
		assertTrue(countingEntry.actions().contains(new QuestAction.SetVariable("var1", 0)),
			() -> "quest " + questId + " 进入计数阶段前必须清零 var1");

		// 3. 结构断言：污染阶段必须有上线自愈边（旧存档的 (var1<<6)|step 整型步数）。
		for (CountingStage stage : contract.pollutedStages()) {
			QuestTransition heal = definition.transitions().stream()
				.filter(t -> stage.node().equals(t.sourceNode()) && stage.node().equals(t.targetNode()))
				.filter(t -> new QuestEvent.EnterWorld().equals(t.event()))
				.findFirst()
				.orElseThrow(() -> new AssertionError(
					"quest " + questId + " 缺失 " + stage.node() + " 的上线自愈转移"));
			assertEquals(List.of(new QuestCondition.QuestVariableIs("var0", stage.stage()),
				new QuestCondition.VariableAtLeast("var1", 1)), heal.conditions(),
				() -> "quest " + questId + " " + stage.node() + " 自愈必须只匹配被污染的整型步数");
			assertEquals(List.of(new QuestAction.SetVariable("var1", 0)), heal.actions(),
				() -> "quest " + questId + " " + stage.node() + " 自愈必须把残留计数清回 0");
			assertEquals(List.of(packetSync), heal.afterCommit(),
				() -> "quest " + questId + " " + stage.node() + " 自愈的 after-commit 必须只有 PACKET_ONLY 同步");
		}

		// 4. 行为模拟：通过真实 QuestMutationPlanner 验证击杀推进与打包步数纯净。
		int initialPacked = layout.pack(Map.of("var0", contract.stageKill(), "var1", 0));
		QuestSnapshot current = snapshot(questId, initialPacked);

		// 前 N-1 次击杀：应自环留在 sourceNode，var1 逐次累加
		for (int kill = 1; kill < contract.requiredKills(); kill++) {
			int currentKill = kill;
			current = dispatch(compiled, current, killEvent);
			Map<String, Integer> vars = layout.unpack(current.packedVariables());
			assertEquals(contract.stageKill(), vars.get("var0"),
				() -> "quest " + questId + " 第 " + currentKill + " 次击杀后阶段必须保持为 " + contract.sourceNode());
			assertEquals(kill, vars.get("var1"),
				() -> "quest " + questId + " 第 " + currentKill + " 次击杀后 var1 计数必须为 " + currentKill);
			assertEquals(QuestStatus.START, current.status());
		}

		// 第 N 次击杀：应收口推进到 targetNode，并把计数清零，打包步数等于纯阶段值
		current = dispatch(compiled, current, killEvent);
		Map<String, Integer> varsAfterCompletion = layout.unpack(current.packedVariables());
		assertEquals(contract.stageNext(), varsAfterCompletion.get("var0"),
			() -> "quest " + questId + " 达成满额击杀后阶段必须推进到 " + contract.targetNode());
		assertEquals(0, varsAfterCompletion.get("var1"),
			() -> "quest " + questId + " 达成满额击杀后必须清空局部计数（QE-044）");
		assertEquals(contract.stageNext(), current.packedVariables(),
			() -> "quest " + questId + " 收口后的整型步数必须是纯净的 " + contract.stageNext());
		assertEquals(QuestStatus.START, current.status());

		// 超额击杀：在 targetNode 状态下不应再被上一阶段的击杀路由捕获
		QuestSnapshot afterOverkill = dispatch(compiled, current, killEvent);
		assertEquals(current.packedVariables(), afterOverkill.packedVariables(),
			() -> "quest " + questId + " 推进后超额击杀不应导致状态变化");

		// 5. 行为模拟：旧存档携带 (var1<<6)|stage 的污染整型步数时，登录自愈必须收敛到纯净阶段
		for (CountingStage stage : contract.pollutedStages()) {
			QuestStatus status = "reward".equals(stage.node()) ? QuestStatus.REWARD : QuestStatus.START;
			int polluted = layout.pack(Map.of("var0", stage.stage(), "var1", 1));
			QuestSnapshot healed = dispatch(compiled, snapshot(questId, status, polluted), new QuestEvent.EnterWorld());
			assertEquals(stage.stage(), healed.packedVariables(),
				() -> "quest " + questId + " " + stage.node() + " 登录自愈后整型步数必须纯净");
		}
	}

	private static int nodeStage(QuestDefinition definition, String node) {
		return definition.nodes().stream()
			.filter(candidate -> candidate.label().equals(node))
			.map(candidate -> candidate.projection().variables().get("var0"))
			.findFirst()
			.orElseThrow(() -> new AssertionError("unknown node " + node));
	}

	private static QuestSnapshot dispatch(CompiledQuestDefinition definition, QuestSnapshot snapshot,
			QuestEvent event) {
		List<QuestTransition> candidates = definition.transitionsFor(event.type()).stream()
			.filter(transition -> QuestEvent.matches(transition.event(), event))
			.sorted(Comparator.comparingInt(transition -> transition.priority() == null
				? Integer.MAX_VALUE : transition.priority()))
			.toList();
		for (QuestTransition transition : candidates) {
			var plan = QuestMutationPlanner.plan(definition, snapshot, event, transition);
			if (plan.isPresent()) {
				QuestMutationPlan mutation = plan.orElseThrow();
				return snapshot(definition.id(), mutation.nextStatus(), mutation.nextPackedVariables());
			}
		}
		return snapshot;
	}

	private static QuestSnapshot snapshot(int questId, int packedVariables) {
		return snapshot(questId, QuestStatus.START, packedVariables);
	}

	private static QuestSnapshot snapshot(int questId, QuestStatus status, int packedVariables) {
		return new QuestSnapshot(7, questId, status, packedVariables, Map.of());
	}

	private static CompiledQuestDefinition load(int questId) {
		String resource = "/aion/data/static_data/quest_definition/quests/" + questId + ".xml";
		try (InputStream input = Objects.requireNonNull(
				Quest15400And25400KillCounterContractTest.class.getResourceAsStream(resource), resource)) {
			return QuestDefinitionXmlCompiler.compile(input);
		} catch (Exception e) {
			throw new AssertionError("unable to load " + resource, e);
		}
	}
}
