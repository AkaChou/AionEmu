package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 按 (状态, 打包投影) 定位节点/转移的测试工具。
 * <p>
 * 任务退役后由真端网格合成，节点**标签与旧 XML 不同**（网格用 {@code a1..a9}，旧 XML 用
 * {@code k1..k9}），因此锁合同的测试不能再按标签断言。这里的定位口径与家族等价门禁一致：
 * 状态相同且 {@link ProgressLayout#pack} 后的变量值相同即视为同一节点。
 * <p>
 * Test helper locating nodes and transitions by (status, packed projection). Retired quests come from the
 * retail grid whose node labels differ from the legacy XML, so contract tests must not assert labels.
 */
final class QuestProjectionNodes {

	private QuestProjectionNodes() {
	}

	/** 唯一定位投影匹配的节点（重复或缺失直接失败）。 / The unique node with that projection. */
	static QuestNode node(QuestDefinition definition, QuestStatus status, Map<String, Integer> vars) {
		List<QuestNode> hits = new ArrayList<>();
		for (QuestNode candidate : definition.nodes()) {
			if (matches(definition, candidate.projection(), status, vars)) {
				hits.add(candidate);
			}
		}
		assertEquals(1, hits.size(), () -> "节点投影不唯一：" + status + " " + vars);
		return hits.getFirst();
	}

	/** 以该投影节点为源的转移。 / Transitions whose source node carries that projection. */
	static List<QuestTransition> routes(QuestDefinition definition, QuestStatus status,
			Map<String, Integer> vars) {
		Map<String, NodeProjection> projections = projections(definition);
		List<QuestTransition> hits = new ArrayList<>();
		for (QuestTransition transition : definition.transitions()) {
			NodeProjection source = transition.sourceNode() == null ? null
				: projections.get(transition.sourceNode());
			if (source != null && matches(definition, source, status, vars)) {
				hits.add(transition);
			}
		}
		return hits;
	}

	/** 该投影节点上、指定 NPC 的全部对话转移。 / Talk transitions of that node for the NPC. */
	static List<QuestTransition> talkRoutes(QuestDefinition definition, QuestStatus status,
			Map<String, Integer> vars, int npcId) {
		List<QuestTransition> hits = new ArrayList<>();
		for (QuestTransition transition : routes(definition, status, vars)) {
			if (transition.event() instanceof QuestEvent.TalkToNpc talk && talk.npcId() == npcId) {
				hits.add(transition);
			}
		}
		return hits;
	}

	/** 该投影节点上、指定 NPC 与动作的全部对话转移。 / Talk transitions for the npc and action. */
	static List<QuestTransition> talkRoutes(QuestDefinition definition, QuestStatus status,
			Map<String, Integer> vars, int npcId, QuestDialogAction action) {
		List<QuestTransition> hits = new ArrayList<>();
		for (QuestTransition transition : talkRoutes(definition, status, vars, npcId)) {
			if (((QuestEvent.TalkToNpc) transition.event()).dialogId() == action.id()) {
				hits.add(transition);
			}
		}
		return hits;
	}

	/** 该投影节点上、指定 NPC 与动作的唯一对话转移。 / The single talk transition for npc and action. */
	static QuestTransition singleTalkRoute(QuestDefinition definition, QuestStatus status,
			Map<String, Integer> vars, int npcId, QuestDialogAction action) {
		List<QuestTransition> hits = talkRoutes(definition, status, vars, npcId, action);
		assertEquals(1, hits.size(), () -> "对话转移不唯一：" + status + " " + vars + " npc=" + npcId
			+ " action=" + action);
		return hits.getFirst();
	}

	/** 断言转移的目标节点落在给定投影上。 / Asserts the transition target carries that projection. */
	static void assertTarget(QuestDefinition definition, QuestTransition transition, QuestStatus status,
			Map<String, Integer> vars) {
		NodeProjection target = projectionOf(definition, transition.targetNode());
		assertEquals(status, target.status(), "目标节点状态");
		assertEquals(definition.progressLayout().pack(vars),
			definition.progressLayout().pack(target.variables()), "目标节点投影");
	}

	/** 指定目标状态的转移上的全部对话 NPC。 / Every talk npc of the transitions targeting that status. */
	static List<Integer> targetNpcs(QuestDefinition definition, QuestStatus sourceStatus,
			Map<String, Integer> sourceVars, QuestStatus targetStatus) {
		List<Integer> npcs = new ArrayList<>();
		for (QuestTransition transition : routes(definition, sourceStatus, sourceVars)) {
			if (projectionOf(definition, transition.targetNode()).status() != targetStatus) {
				continue;
			}
			if (transition.event() instanceof QuestEvent.TalkToNpc talk && !npcs.contains(talk.npcId())) {
				npcs.add(talk.npcId());
			}
		}
		return npcs;
	}

	static NodeProjection projectionOf(QuestDefinition definition, String label) {
		for (QuestNode node : definition.nodes()) {
			if (label.equals(node.label())) {
				return node.projection();
			}
		}
		throw new IllegalStateException("missing node " + label);
	}

	/** 状态相同且打包值相同即同一投影。 / Same status and same packed variables means the same projection. */
	static boolean matches(QuestDefinition definition, NodeProjection projection, QuestStatus status,
			Map<String, Integer> vars) {
		return projection.status() == status
			&& definition.progressLayout().pack(projection.variables())
				== definition.progressLayout().pack(vars);
	}

	private static Map<String, NodeProjection> projections(QuestDefinition definition) {
		Map<String, NodeProjection> projections = new HashMap<>();
		for (QuestNode node : definition.nodes()) {
			projections.put(node.label(), node.projection());
		}
		return projections;
	}
}
