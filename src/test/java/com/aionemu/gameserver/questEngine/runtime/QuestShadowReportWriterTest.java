package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
import com.aionemu.gameserver.questEngine.definition.EvidenceRef;
import com.aionemu.gameserver.questEngine.definition.ImmutableQuestCatalog;
import com.aionemu.gameserver.questEngine.definition.PersistenceMode;
import com.aionemu.gameserver.questEngine.definition.QuestDsl;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.aionemu.gameserver.questEngine.definition.QuestDsl.bitField;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.project;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.setVariable;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.statusIs;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.talkToNpc;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.variableIs;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.vars;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The report writer emits a stable, mergeable, schema-versioned JSON payload. */
class QuestShadowReportWriterTest {
	private static final int PLAYER_ID = 7;
	private static final int QUEST_ID = 1001;

	@TempDir
	Path tempDir;

	@Test
	void cleanReportSerializesToDeterministicJson() {
		QuestShadowRunner runner = new QuestShadowRunner(new ImmutableQuestCatalog(List.of(definition())));
		QuestShadowBatchReport report = batch(runner, observation(true, QuestStatus.REWARD, 1,
			QuestRouteResult.HANDLED, List.of(new com.aionemu.gameserver.questEngine.definition.QuestAction.SetVariable("step", 1))), Set.of(QUEST_ID));

		String json = QuestShadowReportWriter.toJson(report);
		assertEquals("{\"schemaVersion\":2,"
			+ "\"expectedOwners\":[1001],\"coveredOwners\":[1001],\"missingOwners\":[],\"unexpectedOwners\":[],"
			+ "\"expectedCoverage\":[{\"questId\":1001,\"eventType\":\"TALK_TO_NPC\","
			+ "\"eventSelector\":\"TalkToNpc[npcId=700001, dialogId=null, interactionObjectId=0]\",\"sourceNode\":\"start\","
			+ "\"targetNode\":\"reward\",\"priority\":null,\"dispatchContract\":\"EXCLUSIVE\"}],"
			+ "\"coveredCoverage\":[{\"questId\":1001,\"eventType\":\"TALK_TO_NPC\","
			+ "\"eventSelector\":\"TalkToNpc[npcId=700001, dialogId=null, interactionObjectId=0]\",\"sourceNode\":\"start\","
			+ "\"targetNode\":\"reward\",\"priority\":null,\"dispatchContract\":\"EXCLUSIVE\"}],"
			+ "\"missingCoverage\":[],\"unexpectedCoverage\":[],"
			+ "\"expectedCoverageCount\":1,\"coveredCoverageCount\":1,"
			+ "\"expectedInvocations\":1,\"actualInvocations\":1,"
			+ "\"complete\":true,\"clean\":true,\"differenceCounts\":{},"
			+ "\"comparisons\":[{\"eventType\":\"TALK_TO_NPC\",\"differences\":[]}]}",
			json);
	}

	@Test
	void reportWithDifferencesExposesTypedKindsAndMissingOwners() {
		QuestShadowRunner runner = new QuestShadowRunner(new ImmutableQuestCatalog(List.of(definition())));
		QuestShadowBatchReport report = batch(runner, observation(false, QuestStatus.START, 5,
			QuestRouteResult.NOT_HANDLED, List.of()), Set.of(QUEST_ID, 1002));

		String json = QuestShadowReportWriter.toJson(report);
		assertTrue(json.contains("\"missingOwners\":[1002]"));
		assertTrue(json.contains("\"complete\":false"));
		assertTrue(json.contains("\"clean\":false"));
		assertTrue(json.contains("\"CONDITION\":1"));
		assertTrue(json.contains("\"RESULT_CONSUMPTION\":2"));
		assertTrue(json.contains("\"kind\":\"CONDITION\""));
		assertTrue(json.contains("\"kind\":\"RESULT_CONSUMPTION\""));
	}

	@Test
	void writeAtomicPersistsRenameablePayloadAndReadSchemaFailsClosedOnCorruption() throws Exception {
		QuestShadowRunner runner = new QuestShadowRunner(new ImmutableQuestCatalog(List.of(definition())));
		QuestShadowBatchReport report = batch(runner, observation(true, QuestStatus.REWARD, 1,
			QuestRouteResult.HANDLED, List.of(new com.aionemu.gameserver.questEngine.definition.QuestAction.SetVariable("step", 1))), Set.of(QUEST_ID));
		Path target = tempDir.resolve("shadow-batch.json");

		QuestShadowReportWriter.writeAtomic(target, report);

		assertTrue(Files.exists(target));
		assertEquals(QuestShadowReportWriter.SCHEMA_VERSION, QuestShadowReportWriter.readSchemaVersion(target));

		// 字段内部自相矛盾也必须 fail-closed，不能只检查 JSON 尾部和版本号
		String valid = Files.readString(target, StandardCharsets.UTF_8);
		Files.writeString(target, valid.replace("\"coveredCoverageCount\":1", "\"coveredCoverageCount\":0"),
			StandardCharsets.UTF_8);
		assertThrows(IllegalArgumentException.class, () -> QuestShadowReportWriter.readSchemaVersion(target));

		Files.writeString(target, valid.replace("\"schemaVersion\":2", "\"schemaVersion\":1"),
			StandardCharsets.UTF_8);
		assertThrows(IllegalArgumentException.class, () -> QuestShadowReportWriter.readSchemaVersion(target));

		// 截断文件 fail-closed
		String truncated = valid.substring(0, 40);
		Files.writeString(target, truncated, StandardCharsets.UTF_8);
		assertThrows(IllegalArgumentException.class, () -> QuestShadowReportWriter.readSchemaVersion(target));

		// 缺 schemaVersion fail-closed
		Files.writeString(target, "{\"complete\":true}", StandardCharsets.UTF_8);
		assertThrows(IllegalArgumentException.class, () -> QuestShadowReportWriter.readSchemaVersion(target));

		// 非对象 fail-closed
		Files.writeString(target, "[1,2,3]", StandardCharsets.UTF_8);
		assertThrows(IllegalArgumentException.class, () -> QuestShadowReportWriter.readSchemaVersion(target));

		// 空文件 fail-closed
		Files.writeString(target, "", StandardCharsets.UTF_8);
		assertThrows(IllegalArgumentException.class, () -> QuestShadowReportWriter.readSchemaVersion(target));
	}

	private static QuestShadowBatchReport batch(QuestShadowRunner runner, QuestShadowObservation observation,
			Set<Integer> expectedOwners) {
		QuestShadowBatchRunner.Envelope envelope = new QuestShadowBatchRunner.Envelope(talkToNpc(700001),
			Map.of(QUEST_ID, new QuestSnapshot(PLAYER_ID, QUEST_ID, QuestStatus.START, 0, Map.of())), observation,
			List.of(new QuestLegacyInvocation(PLAYER_ID, QUEST_ID, "TALK_TO_NPC",
				QuestDispatchContract.EXCLUSIVE, observation)));
		return QuestShadowBatchRunner.compare(runner, List.of(envelope), expectedOwners);
	}

	private static QuestShadowObservation observation(boolean matched, QuestStatus status, int packedVariables,
			QuestRouteResult result, List<com.aionemu.gameserver.questEngine.definition.QuestAction> actions) {
		return new QuestShadowObservation(Map.of(QUEST_ID, new QuestShadowObservation.Owner(QUEST_ID,
				matched, status, packedVariables, actions, List.of(), result)), result == QuestRouteResult.HANDLED);
	}

	private static CompiledQuestDefinition definition() {
		return QuestDsl.quest(QUEST_ID)
				.evidence(new EvidenceRef("test", "shadow-report-writer", "fixture"))
				.progress(bitField("step", 0, 6, PersistenceMode.PERSISTENT))
				.node("start", project(QuestStatus.START, vars("step", 0)))
				.node("reward", project(QuestStatus.REWARD, vars("step", 1)))
				.on(talkToNpc(700001)).when(statusIs(QuestStatus.START)).when(variableIs("step", 0))
					.then(setVariable("step", 1)).goTo("reward")
				.compile();
	}
}
