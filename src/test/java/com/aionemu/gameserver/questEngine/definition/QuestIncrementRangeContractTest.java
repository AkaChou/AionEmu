package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 全服门禁：被精确匹配（variable-is）消费的进度字段，其增量动作必须携带上限守卫且不得越过声明范围。
 * Production gate: a progress field consumed by exact matching (variable-is) must bound its increment actions
 * inside the declared field range.
 *
 * <p>背景：{@code IncrementVariable} 与解包后的存档值合并后由 {@code ProgressLayout#pack} 校验范围，
 * 越界即抛 IllegalArgumentException，使整笔对话或击杀在 PLAN 阶段失败（QUEST_AUDIT 记录根因
 * value out of range for progress field）。精确匹配的完成阈值只接受单一取值，因此缺少上限守卫的自环
 * 计数一旦越过阈值就永久失去完成路线（10525 的 var1、10529/20529 的 s8 计数即为此类缺陷）。</p>
 * <p>Background: increments are merged with the unpacked save and validated by {@code ProgressLayout#pack};
 * an out-of-range value throws IllegalArgumentException and fails the whole dialog or kill at the PLAN stage
 * (QUEST_AUDIT root cause: value out of range for progress field). Exact completion thresholds accept a single
 * value, so an unguarded self-loop counter that overshoots them loses its completion route for good.</p>
 */
class QuestIncrementRangeContractTest {
	@Test
	void exactlyMatchedCountersStayInsideTheirDeclaredRange() {
		QuestCatalog catalog = QuestDefinitionCatalogManifest.compile(
			Path.of("src/main/resources/aion/data/static_data/quest_definition"));
		List<String> violations = new ArrayList<>();
		int checked = 0;
		for (CompiledQuestDefinition compiled : catalog.executables()) {
			QuestDefinition definition = compiled.definition();
			Set<String> exactlyMatched = definition.transitions().stream()
				.flatMap(transition -> transition.conditions().stream())
				.filter(QuestCondition.QuestVariableIs.class::isInstance)
				.map(condition -> ((QuestCondition.QuestVariableIs) condition).field())
				.collect(Collectors.toSet());
			for (QuestTransition transition : definition.transitions()) {
				for (QuestAction action : transition.actions()) {
					if (!(action instanceof QuestAction.IncrementVariable(String field, int delta))
							|| !exactlyMatched.contains(field)) {
						continue;
					}
					checked++;
					BitField progressField = definition.progressLayout().field(field);
					Integer bound = upperBound(definition, transition, field);
					if (progressField == null || bound == null || bound + delta > progressField.maxValue()) {
						violations.add(compiled.id() + " " + transition.sourceNode() + "->"
							+ transition.targetNode() + " increments " + field + " by " + delta
							+ " without an upper bound <= "
							+ (progressField == null ? "?" : progressField.maxValue()) + " (bound "
							+ (bound == null ? "none" : bound) + ")");
					}
				}
			}
		}
		assertEquals(List.of(), violations, "精确匹配的计数增量必须自带上限守卫");
		assertTrue(checked > 100, "生产审计必须覆盖全服被精确匹配消费的计数增量");
	}

	/**
	 * 取该转移对字段的上界：条件 {@code variable-below}/{@code variable-is} 或 source 节点投影。
	 * Upper bound of the field for one transition: a variable-below/variable-is condition or the source
	 * projection. Returns null when nothing bounds the value; a negative bound (variable-below F 0) is a
	 * legitimately unreachable precondition and therefore a valid bound.
	 */
	private static Integer upperBound(QuestDefinition definition, QuestTransition transition, String field) {
		Integer bound = null;
		for (QuestCondition condition : transition.conditions()) {
			Integer candidate = switch (condition) {
				case QuestCondition.VariableBelow below when below.field().equals(field) -> below.value() - 1;
				case QuestCondition.QuestVariableIs exact when exact.field().equals(field) -> exact.value();
				default -> null;
			};
			if (candidate != null) {
				bound = bound == null ? candidate : Math.min(bound, candidate);
			}
		}
		if (transition.sourceNode() != null) {
			Integer pinned = definition.nodes().stream()
				.filter(node -> node.label().equals(transition.sourceNode()))
				.map(node -> node.projection().variables().get(field))
				.filter(Objects::nonNull)
				.findFirst().orElse(null);
			if (pinned != null) {
				bound = bound == null ? pinned : Math.min(bound, pinned);
			}
		}
		return bound;
	}
}
