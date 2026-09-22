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
		Map<String, Integer> variables = new LinkedHashMap<>();
		for (BitField field : definition.progressLayout().fields()) {
			variables.put(field.name(), 0);
		}
		QuestStatus status = QuestStatus.START;
		List<QuestTransition> ordered = killTransitions(compiled);
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
				return UNREACHABLE;
			}
			variables = definition.progressLayout().unpack(plan.nextPackedVariables());
			status = plan.nextStatus();
			if (status == QuestStatus.REWARD || status == QuestStatus.COMPLETE) {
				return kills;
			}
		}
		return EXCEEDED_CAP;
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
			for (QuestAction action : transition.actions()) {
				if (action instanceof QuestAction.IncrementVariable(String field, int delta) && delta > 0) {
					fields.add(field);
				} else if (action instanceof QuestAction.SetVariable(String field, int value) && value > 1) {
					fields.add(field);
				}
			}
		}
		return fields;
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
