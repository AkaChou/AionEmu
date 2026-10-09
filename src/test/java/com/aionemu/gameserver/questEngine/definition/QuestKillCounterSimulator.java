package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlan;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlanner;
import com.aionemu.gameserver.questEngine.runtime.QuestSnapshot;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 用生产 IR 与真实 {@link QuestMutationPlanner} 模拟连续击杀，得到"引擎口径下完成所需击杀数"。
 * Simulates consecutive kills through the production IR and the real planner to derive the
 * engine-side number of kills a quest requires.
 * <p>模拟不读客户端数据，只回答"按当前 XML 玩家要杀几只"；与客户端门控的比对由门禁测试完成。
 * The simulator never reads client data: it answers "how many kills does the current XML require".
 */
final class QuestKillCounterSimulator {
	/** 无法从全新 START 快照触发任何击杀路由（例如需要先走对话/前置步骤）。 */
	static final int UNREACHABLE = -1;
	/** 超过击杀上限仍未进入 REWARD（上限用于防止自环任务把测试拖死）。 */
	static final int EXCEEDED_CAP = -2;

	private static final int MAX_SIMULATED_KILLS = 300;

	private QuestKillCounterSimulator() {
	}

	/**
	 * 返回完成所需击杀数；不可达或超限时返回 {@link #UNREACHABLE} / {@link #EXCEEDED_CAP}。
	 * Returns the kill count required to reach REWARD, or the sentinel values above.
	 */
	static int requiredKills(CompiledQuestDefinition compiled) {
		QuestDefinition definition = compiled.definition();
		List<QuestEvent> killEvents = killEvents(compiled);
		if (killEvents.isEmpty()) {
			return UNREACHABLE;
		}
		List<QuestTransition> ordered = killTransitions(compiled);
		// 全新 START 快照（所有字段 0）先试一次；不命中时退回到第一条击杀转换的 source 投影。
		// 15101 这类任务在击杀行之前还有一段客户端行 0 对话（legacy STEP_TO_1 写 SECTION_0=1），
		// 只从 0 态模拟会把"要杀几只"误判成不可达。
		// First try a fresh START snapshot; if no kill route matches, restart from the source projection of
		// the first kill transition, because quests such as 15101 open the kill row through a dialogue step.
		Integer fresh = simulate(compiled, definition, killEvents, ordered, Map.of());
		if (fresh != null && fresh != UNREACHABLE) {
			return fresh;
		}
		Map<String, Integer> stage = firstKillStageVariables(compiled, ordered);
		if (stage == null || stage.equals(Map.of())) {
			return UNREACHABLE;
		}
		Integer entered = simulate(compiled, definition, killEvents, ordered, stage);
		return entered == null ? UNREACHABLE : entered;
	}

	/**
	 * 从给定起始变量模拟连续击杀；无法匹配任何击杀路由时返回 null。
	 * Simulates consecutive kills from the given start variables, returning null when no kill route matches.
	 */
	private static Integer simulate(CompiledQuestDefinition compiled, QuestDefinition definition,
			List<QuestEvent> killEvents, List<QuestTransition> ordered, Map<String, Integer> start) {
		Map<String, Integer> variables = new LinkedHashMap<>();
		for (BitField field : definition.progressLayout().fields()) {
			variables.put(field.name(), 0);
		}
		variables.putAll(start);
		QuestStatus status = QuestStatus.START;
		for (int kills = 1; kills <= MAX_SIMULATED_KILLS; kills++) {
			QuestEvent event = killEvents.get((kills - 1) % killEvents.size());
			QuestSnapshot snapshot = new QuestSnapshot(1, definition.id(), status,
				definition.progressLayout().pack(variables), Map.of());
			QuestMutationPlan plan = null;
			for (QuestTransition transition : ordered) {
				var candidate = QuestMutationPlanner.plan(compiled, snapshot, event, transition);
				if (candidate.isPresent()) {
					plan = candidate.get();
					break;
				}
			}
			if (plan == null) {
				// 击杀段在此收口（后续由对话/交付推进，如 25304 的 s2 -> s3）：返回已计入的击杀数。
				// The kill stage ends here (a dialogue or hand-in takes over, e.g. 25304 s2 -> s3).
				return kills == 1 ? null : kills - 1;
			}
			variables = definition.progressLayout().unpack(plan.nextPackedVariables());
			status = plan.nextStatus();
			if (status == QuestStatus.REWARD || status == QuestStatus.COMPLETE) {
				return kills;
			}
		}
		return EXCEEDED_CAP;
	}

	/** 第一条击杀转换的 source 节点投影（阶段入口）。 / Source-node projection of the first kill transition. */
	private static Map<String, Integer> firstKillStageVariables(CompiledQuestDefinition compiled,
			List<QuestTransition> ordered) {
		for (QuestTransition transition : ordered) {
			String source = transition.sourceNode();
			if (source == null) {
				continue;
			}
			var node = compiled.definition().nodes().stream()
				.filter(candidate -> candidate.label().equals(source))
				.findFirst();
			if (node.isPresent()) {
				return node.get().projection().variables();
			}
		}
		return null;
	}

	/**
	 * 击杀转换里真正承担计数的字段：被 increment，或被写入 &gt;1 的值。
	 * 仅被置 0/1 的字段是阶段标记（如"可报告"开关），不计入计数器数量。
	 * Counter fields written by kill transitions: incremented, or set to a value above 1. Fields only
	 * set to 0/1 are stage flags such as “ready to report”, not counters.
	 */
	static Set<String> killCounterFields(CompiledQuestDefinition compiled) {
		Set<String> fields = new LinkedHashSet<>();
		for (QuestTransition transition : killTransitions(compiled)) {
			Map<String, Integer> targetProjection = projectionVariables(compiled, transition.targetNode());
			for (QuestAction action : transition.actions()) {
				if (action instanceof QuestAction.IncrementVariable(String field, int delta) && delta > 0) {
					fields.add(field);
				} else if (action instanceof QuestAction.SetVariable(String field, int value) && value > 1
						&& !Integer.valueOf(value).equals(targetProjection.get(field))) {
					// 写入目标节点投影值的动作是"阶段/行号钉住"，不是计数器（15101 收口击杀写 SECTION_0=2）。
					// Writes equal to the target projection pin the stage/row instead of counting.
					fields.add(field);
				}
			}
		}
		return fields.isEmpty() ? ladderCounterFields(compiled) : fields;
	}

	/**
	 * 台阶计数（原版 IR 形态）：击杀把局面一级一级推上节点台阶，转换上没有动作；每级往前挪的
	 * 那个进度字段就是计数器（13765 原版形态为 a0→a1→…→a5，全部钉在 var0 上）。
	 * 只在动作口径一无所获时才走这条路——否则原版"台阶 + 附带动词"的写法会把阶段标记也算进来。
	 * Ladder counters (the retail IR form): kills walk a node ladder whose transitions carry no
	 * actions, and the progress field advanced by every rung is the counter. This rule only runs when
	 * the action rule found nothing, so a mixed retail form cannot promote stage markers to counters.
	 */
	private static Set<String> ladderCounterFields(CompiledQuestDefinition compiled) {
		Set<String> fields = new LinkedHashSet<>();
		for (QuestTransition transition : killTransitions(compiled)) {
			Map<String, Integer> source = projectionVariables(compiled, transition.sourceNode());
			Map<String, Integer> target = projectionVariables(compiled, transition.targetNode());
			if (source.isEmpty() || target.isEmpty()) {
				continue;
			}
			for (Map.Entry<String, Integer> entry : target.entrySet()) {
				if (!entry.getValue().equals(source.get(entry.getKey()))) {
					fields.add(entry.getKey());
				}
			}
		}
		return fields;
	}

	/** 目标节点的投影变量。 / Projected variables of the target node. */
	private static Map<String, Integer> projectionVariables(CompiledQuestDefinition compiled, String label) {
		return compiled.definition().nodes().stream()
			.filter(node -> node.label().equals(label))
			.findFirst()
			.map(node -> Map.copyOf(node.projection().variables()))
			.orElseGet(Map::of);
	}

	/** 击杀类事件（按 npc/world/rank 排序，供模拟轮转）。 / Kill-like events, sorted for deterministic rotation. */
	static List<QuestEvent> killEvents(CompiledQuestDefinition compiled) {
		Set<Integer> npcIds = new LinkedHashSet<>();
		Set<Integer> worldIds = new LinkedHashSet<>();
		Set<Integer> ranks = new LinkedHashSet<>();
		for (QuestTransition transition : compiled.definition().transitions()) {
			if (transition.event() instanceof QuestEvent.KillNpc(int npcId)) {
				npcIds.add(npcId);
			} else if (transition.event() instanceof QuestEvent.KillNpcSet(Set<Integer> ids)) {
				npcIds.addAll(ids);
			} else if (transition.event() instanceof QuestEvent.KillInWorld killInWorld) {
				worldIds.add(killInWorld.worldId());
			} else if (transition.event() instanceof QuestEvent.KillRanked killRanked) {
				ranks.add(killRanked.rankId());
			}
		}
		List<QuestEvent> events = new ArrayList<>();
		npcIds.stream().sorted().forEach(id -> events.add(new QuestEvent.KillNpc(id)));
		worldIds.stream().sorted().forEach(id -> events.add(new QuestEvent.KillInWorld(id)));
		ranks.stream().sorted().forEach(id -> events.add(new QuestEvent.KillRanked(id)));
		return List.copyOf(events);
	}

	/**
	 * 击杀转换按运行期派发顺序排序（priority 升序，空值最后），即运行时优先命中的顺序。
	 * Kill transitions in runtime dispatch order: ascending priority with nulls last.
	 */
	private static List<QuestTransition> killTransitions(CompiledQuestDefinition compiled) {
		return compiled.definition().transitions().stream()
			.filter(transition -> transition.event() instanceof QuestEvent.KillNpc
				|| transition.event() instanceof QuestEvent.KillNpcSet
				|| transition.event() instanceof QuestEvent.KillInWorld
				|| transition.event() instanceof QuestEvent.KillRanked)
			.sorted(Comparator.comparing(transition -> transition.priority() == null
				? Integer.MAX_VALUE : transition.priority()))
			.toList();
	}
}
