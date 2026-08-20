package com.aionemu.gameserver.questEngine.e2e;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
import com.aionemu.gameserver.questEngine.definition.QuestDefinitionXmlCompiler;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.definition.QuestTransition;
import com.aionemu.gameserver.questEngine.e2e.client.ClientResourceOracle;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * 验证全量任务审计不会把缺失生产世界出生点的 NPC 对话误报为 PASS。
 * Verifies that the full quest audit does not report NPC dialogs as PASS when production-world spawns are absent.
 */
class QuestWorldReachabilityOracleTest {
	private static final Path CLIENT_MAPPING = Path.of("docs/quest/client-dialog-mapping");
	private static QuestWorldReachabilityOracle worldReachability;
	private static ClientResourceOracle clientResources;

	@BeforeAll
	static void loadProductionEvidence() throws Exception {
		worldReachability = QuestWorldReachabilityOracle.loadProductionData();
		clientResources = ClientResourceOracle.load(CLIENT_MAPPING);
	}

	@Test
	void quest49713MissingNpcSpawnsRequireRuntimeEvidence() throws Exception {
		CompiledQuestDefinition definition = definition(49713);
		Set<Integer> missingNpcIds = Set.of(800936, 800937, 800938);
		List<QuestTransition> dialogTransitions = definition.definition().transitions().stream()
			.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
				&& missingNpcIds.contains(talk.npcId()))
			.toList();
		assertEquals(66, dialogTransitions.size());

		for (QuestTransition transition : dialogTransitions) {
			QuestE2eAuditRow row = QuestE2eBatchAudit.auditTransition(definition, transition, clientResources,
				LegacyQuestEvidenceOracle.empty(), worldReachability);
			assertEquals(QuestE2eStatus.RUNTIME_REQUIRED, row.status(), row::toString);
			assertEquals("STATIC_WORLD", row.validationMode());
			assertTrue(row.reason().contains("has no usable static spawn"), row::reason);
		}
	}

	@Test
	void ordinaryNpcWithLoadedStaticSpawnStillPasses() throws Exception {
		CompiledQuestDefinition definition = definition(1913);
		QuestTransition transition = definition.definition().transitions().stream()
			.filter(candidate -> candidate.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == 203726 && talk.dialogId() != null && talk.dialogId() == 31)
			.findFirst().orElseThrow();

		QuestE2eAuditRow row = QuestE2eBatchAudit.auditTransition(definition, transition, clientResources,
			LegacyQuestEvidenceOracle.empty(), worldReachability);
		assertEquals(QuestE2eStatus.PASS, row.status(), row::toString);
	}

	@Test
	void questOwnedNpcSpawnSatisfiesReachabilityEvidence() throws Exception {
		CompiledQuestDefinition definition = definition(14112);
		QuestTransition transition = definition.definition().transitions().stream()
			.filter(candidate -> candidate.event() instanceof QuestEvent.TalkToNpc talk && talk.npcId() == 203195)
			.findFirst().orElseThrow();

		assertTrue(worldReachability.runtimeRequiredReason(definition, transition).isEmpty());
	}

	private static CompiledQuestDefinition definition(int questId) throws Exception {
		try (InputStream input = QuestWorldReachabilityOracleTest.class.getResourceAsStream(
			"/aion/data/static_data/quest_definition/quests/" + questId + ".xml")) {
			if (input == null) throw new IllegalStateException("missing quest resource " + questId);
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
