package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.model.QuestVars;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlan;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlanner;
import com.aionemu.gameserver.questEngine.runtime.QuestSnapshot;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class ClientQuestSectionAlignmentTest {
	private static final Set<Integer> EXTENDED_COUNTER_QUESTS = Set.of(1842, 1843, 1844, 2843, 2844, 2845);
	/**
	 * 使用 varN 命名、但计数槽仍压在别的 SECTION 上的历史任务。客户端任务书行索引读
	 * SECTION_0（bit 0..5），var1 起必须落在 6N 才能与客户端脚本的 SECTION_N 对齐；
	 * 这 11 个任务是 2026-09-22 审计时仅存的例外（全库 6161 个带位段任务），逐个收口前先锁定清单；
	 * 16800 在批次 28 退役了 var1 影片旗标（行阶梯改用 zone 触发）已从名单移除；18738/28738
	 * 按 DD_ITEMPLAY_VARIANTS 批次退役（多播 ItemPlay 变体家族，原版段布局 var1@6）后离开生产目录，
	 * 相应从本名单移除（P0c-49 顺带偿还，口径 = 生产目录内的例外集）。
	 * Legacy quests that name counters varN yet still park them outside SECTION_N. The client journal
	 * row index reads SECTION_0 (bits 0..5), so var1 and later must sit at 6N to line up with the
	 * client script's SECTION_N; these eleven are the only remaining exceptions (16800 left the list
	 * in batch 28 when its var1 movie flag was retired; 18738/28738 left the production catalog when
	 * the DD_ITEMPLAY_VARIANTS batch retired them — this pin tracks the catalog, so they are dropped
	 * here in the P0c-49 incidental repayment).
	 */
	private static final Set<Integer> SECTION_LAYOUT_DEBT = Set.of(
		4928, 19078, 29074, 29078);
	private static final List<Integer> VILLAGE_HUNT_QUESTS = List.of(
		17106, 17108, 17110, 17112, 17114, 17116, 17118, 17120, 17122, 17124, 17126, 17128,
		17130, 17132, 17134, 17136, 17138, 17140, 17142, 17144, 17146, 17148, 17150, 17152,
		17154);

	@Test
	void clientVisibleKillFieldsUseTheirFixedSixBitSections() {
		QuestCatalog catalog = QuestDefinitionCatalogManifest.compile(
			Path.of("src/main/resources/aion/data/static_data/quest/definitions"));
		int checked = 0;
		for (CompiledQuestDefinition definition : catalog.executables()) {
			int questId = definition.id();
			if (EXTENDED_COUNTER_QUESTS.contains(questId)) {
				continue;
			}
			for (QuestTransition transition : definition.definition().transitions()) {
				if (!(transition.event() instanceof QuestEvent.KillNpc
						|| transition.event() instanceof QuestEvent.KillNpcSet)) {
					continue;
				}
				for (QuestAction action : transition.actions()) {
					String field = touchedField(action);
					if (field == null || !field.matches("var[1-4]")) {
						continue;
					}
					BitField bitField = definition.definition().progressLayout().field(field);
					assertNotNull(bitField, () -> "quest " + questId + " lacks " + field);
					assertEquals(Integer.parseInt(field.substring(3)) * 6, bitField.offset(),
						() -> "quest " + questId + " maps " + field + " outside its client SECTION");
					checked++;
				}
			}
		}
		assertTrue(checked > 100, "production audit must inspect client-visible kill fields");
	}

	@Test
	void talocItemSkillCountersUseTheirFixedSixBitSections() {
		for (int questId : List.of(11468, 21468)) {
			CompiledQuestDefinition definition = load(questId);
			QuestSnapshot state = snapshot(questId, QuestStatus.START, 0);
			for (int skillId : List.of(9832, 9833, 9834)) {
				state = apply(definition, state, new QuestEvent.UseSkill(skillId));
			}

			QuestVars vars = new QuestVars(state.packedVariables());
			assertEquals(0, vars.getVarById(0), () -> "quest " + questId + " moved SECTION_0");
			assertEquals(List.of(1, 1, 1), List.of(vars.getVarById(1), vars.getVarById(2),
				vars.getVarById(3)), () -> "quest " + questId + " did not update SECTION_1..3");
		}
	}

	@Test
	void sectionNamedCountersStayAtTheirFixedSixBitOffset() {
		QuestCatalog catalog = QuestDefinitionCatalogManifest.compile(
			Path.of("src/main/resources/aion/data/static_data/quest/definitions"));
		Set<Integer> offenders = new TreeSet<>();
		for (CompiledQuestDefinition definition : catalog.executables()) {
			for (BitField field : definition.definition().progressLayout().fields()) {
				Matcher matcher = Pattern.compile("var(\\d+)").matcher(field.name());
				if (!matcher.matches() || "0".equals(matcher.group(1))) {
					continue;
				}
				if (field.offset() != Integer.parseInt(matcher.group(1)) * 6) {
					offenders.add(definition.id());
				}
			}
		}
		assertEquals(SECTION_LAYOUT_DEBT, offenders,
			"quests whose varN counters overlap the client journal SECTION_0");
	}

	private static String touchedField(QuestAction action) {
        return switch (action) {
            case QuestAction.SetVariable set -> set.field();
            case QuestAction.IncrementVariable increment -> increment.field();
            default -> null;
        };
	}

	private static QuestSnapshot apply(CompiledQuestDefinition definition, QuestSnapshot snapshot,
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
		return fail("no route for quest " + definition.id() + " event " + event + " variables "
			+ definition.definition().progressLayout().unpack(snapshot.packedVariables()));
	}

	private static QuestSnapshot snapshot(int questId, QuestStatus status, int packedVariables) {
		return new QuestSnapshot(7, questId, status, packedVariables, Map.of());
	}

	/**
	 * 生产视图加载：退役行返回原版合成定义（P0c-9 起 11102 已退役），保留行返回同一 XML 定义。
	 * Loads through the production view: retired rows yield the retail-compiled definition
	 * (11102 retired in P0c-9), retained rows the very same XML definition.
	 */
	private static CompiledQuestDefinition load(int questId) {
		return ProductionQuestDefinitions.definition(questId);
	}
}
