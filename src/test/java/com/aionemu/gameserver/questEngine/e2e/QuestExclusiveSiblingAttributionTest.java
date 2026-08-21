package com.aionemu.gameserver.questEngine.e2e;

import com.aionemu.gameserver.questEngine.e2e.client.ClientResourceOracle;
import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
import com.aionemu.gameserver.questEngine.definition.QuestCondition;
import com.aionemu.gameserver.questEngine.definition.QuestDefinitionXmlCompiler;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.definition.QuestTransition;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 锁定审计归因对确定性兄弟 route 的分类合同：条件互斥（职业分支）、条件相同（双协议注册）
 * 与显式优先级兄弟归入 EXCLUSIVE_SIBLING；非同组源节点或不可判定的重叠仍保持 AMBIGUOUS_ROUTE。
 * Locks the audit attribution contract for deterministic sibling routes: mutually exclusive
 * conditions (class branches), identical conditions (dual-protocol registration), and explicit
 * priority siblings classify as EXCLUSIVE_SIBLING; different source groups or undecidable
 * overlaps stay AMBIGUOUS_ROUTE.
 */
class QuestExclusiveSiblingAttributionTest {

	private static final Path CLIENT_MAPPING = Path.of("docs/quest/client-dialog-mapping");

	@Test
	void conditionListsDetectFactAndRangeExclusivity() {
		QuestCondition gladiator = new QuestCondition.AdvancedClassIs(PlayerClass.GLADIATOR);
		QuestCondition templar = new QuestCondition.AdvancedClassIs(PlayerClass.TEMPLAR);
		assertTrue(QuestCondition.areMutuallyExclusive(gladiator, templar));
		assertFalse(QuestCondition.areMutuallyExclusive(gladiator, gladiator));

		assertTrue(QuestCondition.listsAreMutuallyExclusive(
			List.of(new QuestCondition.VariableBelow("var0", 65)),
			List.of(new QuestCondition.VariableAtLeast("var0", 65))));
		assertFalse(QuestCondition.listsAreMutuallyExclusive(
			List.of(new QuestCondition.VariableAtLeast("var0", 10)),
			List.of(new QuestCondition.VariableAtLeast("var0", 5))));
		assertFalse(QuestCondition.listsAreMutuallyExclusive(
			List.of(),
			List.of(new QuestCondition.VariableAtLeast("var0", 65))));
	}

	@Test
	void auditNeverReportsClassBranchRewardEdgeAsTrueAmbiguity() throws Exception {
		CompiledQuestDefinition definition = definition(13833);
		List<QuestTransition> classBranchEdges = definition.definition().transitions().stream()
			.filter(candidate -> "reward".equals(candidate.sourceNode())
				&& "complete".equals(candidate.targetNode())
				&& candidate.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.dialogId() != null && talk.dialogId() == 8)
			.toList();
		assertEquals(12, classBranchEdges.size());
		ClientResourceOracle oracle = ClientResourceOracle.load(CLIENT_MAPPING);
		for (QuestTransition edge : classBranchEdges) {
			QuestE2eAuditRow row = QuestE2eBatchAudit.auditTransition(definition, edge, oracle);
			assertTrue(row.status() == QuestE2eStatus.PASS
					|| row.status() == QuestE2eStatus.EXCLUSIVE_SIBLING,
				row::toString);
		}
	}

	@Test
	void auditKeepsLegacyRewardSwingEdgeAsAmbiguous() throws Exception {
		CompiledQuestDefinition definition = definition(19900);
		QuestTransition swingEdge = definition.definition().transitions().stream()
			.filter(candidate -> "reward".equals(candidate.sourceNode())
				&& "legacy-reward".equals(candidate.targetNode()))
			.findFirst().orElseThrow();
		ClientResourceOracle oracle = ClientResourceOracle.load(CLIENT_MAPPING);
		QuestE2eAuditRow row = QuestE2eBatchAudit.auditTransition(definition, swingEdge, oracle);
		assertEquals(QuestE2eStatus.AMBIGUOUS_ROUTE, row.status(), row::toString);
	}

	private static CompiledQuestDefinition definition(int questId) throws Exception {
		try (InputStream input = QuestExclusiveSiblingAttributionTest.class.getResourceAsStream(
			"/aion/data/static_data/quest_definition/quests/" + questId + ".xml")) {
			if (input == null) throw new IllegalStateException("missing quest resource " + questId);
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
