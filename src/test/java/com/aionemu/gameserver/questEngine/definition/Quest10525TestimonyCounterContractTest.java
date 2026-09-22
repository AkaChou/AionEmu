package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlan;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlanner;
import com.aionemu.gameserver.questEngine.runtime.QuestSnapshot;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证 10525/20525 四位证人证言计数器的 SECTION_1 布局、增量上限守卫与旧存档自愈。
 * Verifies the SECTION_1 layout, bounded increments and legacy-save healing of the four-witness testimony
 * counter shared by quests 10525 and 20525.
 * <p>报障复盘：旧定义把 var1 紧凑放在 offset 4/width 5，且四条证言自环没有任何上限守卫。
 * 玩家反复点击同一名证人即可让 var1 累加（实测 packed 步数 130/258/386 对应 var1 8/16/24），
 * 下一次 +8 得到 32，被 {@code ProgressLayout.pack} 判为越界抛出 IllegalArgumentException，
 * 该次对话在 PLAN 阶段整笔失败（QUEST_AUDIT 根因：value out of range for progress field: var1）；
 * 且精确匹配的完成阈值（14/13/11/7）一旦被越过就再也无法到达，任务永久停在 s2。</p>
 * <p>Failure recap: the old definition packed var1 at offset 4/width 5 with unguarded testimony self-loops.
 * Repeated clicks on one witness pushed var1 to 8/16/24 (packed steps 130/258/386) and the next +8 produced 32,
 * which {@code ProgressLayout.pack} rejected as out of range and failed the dialog at the PLAN stage; the
 * exact completion thresholds (14/13/11/7) became unreachable, stranding the quest on s2.</p>
 */
class Quest10525TestimonyCounterContractTest {
	private static final int STAGE = 2;
	private static final int NEXT_STAGE = 3;
	private static final int ALL_TESTIMONIES = 15;
	/** 旧布局（offset 4/width 5，var1=24）下报障玩家存档的 packed 值。 Reported packed value under the old layout. */
	private static final int REPORTED_LEGACY_PACKED = 386;
	private static final List<QuestContract> CONTRACTS = List.of(
		new QuestContract(10525, List.of(
			new TestimonyAgent(806224, QuestDialogAction.SELECT3_1_1, 1),
			new TestimonyAgent(806225, QuestDialogAction.SELECT3_2_1, 2),
			new TestimonyAgent(806226, QuestDialogAction.SELECT3_3_1, 4),
			new TestimonyAgent(806227, QuestDialogAction.SELECT3_4_1, 8))),
		new QuestContract(20525, List.of(
			new TestimonyAgent(806228, QuestDialogAction.SELECT3_1_1, 1),
			new TestimonyAgent(806229, QuestDialogAction.SELECT3_2_1, 2),
			new TestimonyAgent(806230, QuestDialogAction.SELECT3_3_1, 4),
			new TestimonyAgent(806231, QuestDialogAction.SELECT3_4_1, 8))));

	@TestFactory
	Stream<DynamicTest> testimonyCountersStayInSectionOneAndRemainCompletable() {
		return CONTRACTS.stream().map(contract -> DynamicTest.dynamicTest("quest " + contract.questId(),
			() -> assertContract(contract)));
	}

	private static void assertContract(QuestContract contract) {
		CompiledQuestDefinition compiled = load(contract.questId());
		QuestDefinition definition = compiled.definition();
		ProgressLayout layout = definition.progressLayout();

		BitField stage = layout.field("var0");
		BitField testimonies = layout.field("var1");
		assertEquals(0, stage.offset(), "阶段必须占用客户端任务说明行索引读取的 SECTION_0");
		assertEquals(6, testimonies.offset(),
			"四位证言必须放在 SECTION_1（offset 6），低位证人证言不得污染 SECTION_0 行索引");
		assertEquals(ALL_TESTIMONIES, testimonies.maxValue(), "证言计数器是 1/2/4/8 的四位集合");

		for (TestimonyAgent agent : contract.agents()) {
			int threshold = ALL_TESTIMONIES - agent.weight();
			QuestEvent testimonyEvent = new QuestEvent.TalkToNpc(agent.npcId(), agent.action().id());

			QuestTransition testimony = s2Transition(definition, "s2", testimonyEvent);
			assertEquals(1, testimony.priority().intValue(),
				() -> "quest " + contract.questId() + " 证言自环必须使用 priority 1 留在 s2");
			assertTrue(testimony.conditions().contains(new QuestCondition.VariableBelow("var1", threshold)),
				() -> "quest " + contract.questId() + " 证人 " + agent.npcId()
					+ " 的增量缺少 variable-below var1=" + threshold + " 上限守卫");
			assertEquals(List.of(new QuestAction.IncrementVariable("var1", agent.weight())),
				testimony.actions());
			assertEquals(List.of(
				new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
				new AfterCommitAction.CloseDialog()), testimony.afterCommit());

			QuestTransition completion = s2Transition(definition, "s3", testimonyEvent);
			assertEquals(0, completion.priority().intValue(),
				() -> "quest " + contract.questId() + " 收齐证言路线必须使用 priority 0 推进");
			assertTrue(completion.conditions().contains(new QuestCondition.QuestVariableIs("var1", threshold)),
				() -> "quest " + contract.questId() + " 证人 " + agent.npcId()
					+ " 仅在其他三份证言齐备（var1==" + threshold + "）时推进");
			assertEquals(Set.of(new QuestAction.SetVariable("var0", NEXT_STAGE),
				new QuestAction.SetVariable("var1", 0)), Set.copyOf(completion.actions()),
				() -> "quest " + contract.questId() + " 推进时必须清空证言计数器");
			assertEquals(List.of(
				new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
				new AfterCommitAction.CloseDialog()), completion.afterCommit());
		}

		Set<Integer> reachable = reachableTestimonyValues(compiled, contract);
		assertEquals(Set.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14), reachable,
			() -> "quest " + contract.questId() + " 的可达证言值必须落在声明范围内");
		for (int value : reachable) {
			assertTrue(canCompleteFrom(compiled, contract,
					snapshot(contract.questId(), layout.pack(Map.of("var0", STAGE, "var1", value)))),
				() -> "quest " + contract.questId() + " 的证言值 " + value + " 无法再回到完成路线");
		}

		QuestSnapshot reportedLegacy = snapshot(contract.questId(), REPORTED_LEGACY_PACKED);
		assertEquals(Map.of("var0", STAGE, "var1", 6), layout.unpack(REPORTED_LEGACY_PACKED),
			() -> "quest " + contract.questId() + " 必须按新布局解读报障存档");
		assertTrue(canCompleteFrom(compiled, contract, reportedLegacy),
			() -> "quest " + contract.questId() + " 的报障存档必须仍可完成");

		QuestSnapshot saturated = snapshot(contract.questId(),
			layout.pack(Map.of("var0", STAGE, "var1", ALL_TESTIMONIES)));
		QuestSnapshot healed = dispatch(compiled, saturated, new QuestEvent.EnterWorld());
		assertEquals(Map.of("var0", STAGE, "var1", 0), layout.unpack(healed.packedVariables()),
			() -> "quest " + contract.questId() + " 的饱和证言残值必须由进入世界自愈边归零");
	}

	/** 遍历四条证言自环可到达的全部计数器值。 Explores every counter value reachable through the testimony self-loops. */
	private static Set<Integer> reachableTestimonyValues(CompiledQuestDefinition compiled, QuestContract contract) {
		ProgressLayout layout = compiled.definition().progressLayout();
		Set<Integer> reachable = new LinkedHashSet<>();
		Deque<Integer> pending = new ArrayDeque<>();
		reachable.add(0);
		pending.add(0);
		while (!pending.isEmpty()) {
			int value = pending.removeFirst();
			QuestSnapshot state = snapshot(compiled.id(), layout.pack(Map.of("var0", STAGE, "var1", value)));
			for (TestimonyAgent agent : contract.agents()) {
				QuestSnapshot next = dispatch(compiled, state,
					new QuestEvent.TalkToNpc(agent.npcId(), agent.action().id()));
				if (next.packedVariables() == state.packedVariables()) {
					continue;
				}
				int produced = layout.unpack(next.packedVariables()).get("var1");
				assertTrue(produced <= ALL_TESTIMONIES,
					() -> "quest " + compiled.id() + " 产生越界证言值 " + produced);
				if (layout.unpack(next.packedVariables()).get("var0") != STAGE) {
					continue;
				}
				if (reachable.add(produced)) {
					pending.add(produced);
				}
			}
		}
		return reachable;
	}

	/** 从给定存档出发，判断是否仍存在收齐四份证言进入 s3 的路径。 True when s3 is still reachable from the state. */
	private static boolean canCompleteFrom(CompiledQuestDefinition compiled, QuestContract contract,
			QuestSnapshot start) {
		ProgressLayout layout = compiled.definition().progressLayout();
		Set<Integer> visited = new LinkedHashSet<>();
		Deque<QuestSnapshot> pending = new ArrayDeque<>();
		visited.add(start.packedVariables());
		pending.add(start);
		while (!pending.isEmpty()) {
			QuestSnapshot state = pending.removeFirst();
			for (TestimonyAgent agent : contract.agents()) {
				QuestSnapshot next = dispatch(compiled, state,
					new QuestEvent.TalkToNpc(agent.npcId(), agent.action().id()));
				if (next.packedVariables() == state.packedVariables()) {
					continue;
				}
				Map<String, Integer> produced = layout.unpack(next.packedVariables());
				if (produced.get("var0") == NEXT_STAGE) {
					return true;
				}
				if (visited.add(next.packedVariables())) {
					pending.add(next);
				}
			}
		}
		return false;
	}

	/** 定位 s2 出发的唯一目标节点路线。 Locates the single route leaving s2 towards one target node. */
	private static QuestTransition s2Transition(QuestDefinition definition, String target,
			QuestEvent event) {
		List<QuestTransition> matches = definition.transitions().stream()
			.filter(candidate -> "s2".equals(candidate.sourceNode()))
			.filter(candidate -> candidate.targetNode().equals(target))
			.filter(candidate -> candidate.event().equals(event))
			.toList();
		assertEquals(1, matches.size(), () -> "s2 -> " + target + " " + event);
		return matches.getFirst();
	}

	/** 复刻 dispatcher 的优先级选择：条件不匹配即无操作。 Mirrors the dispatcher priority choice; a mismatch is a no-op. */
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
				Quest10525TestimonyCounterContractTest.class.getResourceAsStream(resource), resource)) {
			return QuestDefinitionXmlCompiler.compile(input);
		} catch (Exception e) {
			throw new AssertionError("unable to load " + resource, e);
		}
	}

	/** 单个任务的双阵营证人契约。 One quest's two-faction witness contract. */
	private record QuestContract(int questId, List<TestimonyAgent> agents) {
	}

	/** 一名证人：NPC、证言按钮动作与其证言权重（1/2/4/8）。 One witness: NPC, testimony action and weight. */
	private record TestimonyAgent(int npcId, QuestDialogAction action, int weight) {
	}
}
