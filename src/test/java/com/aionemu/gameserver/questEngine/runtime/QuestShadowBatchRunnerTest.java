package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
import com.aionemu.gameserver.questEngine.definition.EvidenceRef;
import com.aionemu.gameserver.questEngine.definition.ImmutableQuestCatalog;
import com.aionemu.gameserver.questEngine.definition.PersistenceMode;
import com.aionemu.gameserver.questEngine.definition.QuestDsl;
import com.aionemu.gameserver.questEngine.definition.QuestStateSyncMode;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.aionemu.gameserver.questEngine.definition.QuestDsl.bitField;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.completeQuest;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.project;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.setVariable;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.statusIs;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.syncQuestState;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.talkToNpc;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.variableIs;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.vars;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestShadowBatchRunnerTest {
	private static final int PLAYER_ID = 7;
	private static final int QUEST_ID = 1001;

	@Test
	void cleanReportRequiresFullUniqueOwnerCoverage() {
		CompiledQuestDefinition definition = definition();
		QuestShadowRunner runner = new QuestShadowRunner(new ImmutableQuestCatalog(List.of(definition)));
		QuestShadowBatchRunner.Envelope envelope = envelope(observation(true, QuestStatus.REWARD, 1,
			QuestRouteResult.HANDLED, List.of(new com.aionemu.gameserver.questEngine.definition.QuestAction.SetVariable("step", 1))));

		QuestShadowBatchReport complete = QuestShadowBatchRunner.compare(runner, List.of(envelope), Set.of(QUEST_ID));
		assertTrue(complete.complete());
		assertTrue(complete.clean());
		assertEquals(Map.of(), complete.differenceCounts());
		assertEquals(Set.of(), complete.missingOwners());
		assertEquals(Set.of(), complete.missingCoverage());

		// 期望额外 owner 未出现 → 不完整,不能以部分样本关闭门禁
		QuestShadowBatchReport partial = QuestShadowBatchRunner.compare(runner, List.of(envelope), Set.of(QUEST_ID, 1002));
		assertFalse(partial.complete());
		assertFalse(partial.clean());
		assertEquals(Set.of(1002), partial.missingOwners());
	}

	@Test
	void repeatingOneOwnerPathCannotCoverAnotherPath() {
		var builder = QuestDsl.quest(QUEST_ID)
			.evidence(new EvidenceRef("test", "shadow-batch", "multi-path fixture"))
			.progress(bitField("step", 0, 6, PersistenceMode.PERSISTENT))
			.node("start", project(QuestStatus.START, vars("step", 0)))
			.node("middle", project(QuestStatus.START, vars("step", 1)))
			.node("reward", project(QuestStatus.REWARD, vars("step", 2)));
		builder.on(talkToNpc(700001)).from("start").when(variableIs("step", 0))
			.then(setVariable("step", 1)).goTo("middle");
		builder.on(talkToNpc(700001)).from("middle").when(variableIs("step", 1))
			.then(setVariable("step", 2)).goTo("reward");
		QuestShadowRunner runner = new QuestShadowRunner(new ImmutableQuestCatalog(List.of(builder.compile())));
		QuestShadowObservation observation = observation(true, QuestStatus.START, 1, QuestRouteResult.HANDLED,
			List.of(new com.aionemu.gameserver.questEngine.definition.QuestAction.SetVariable("step", 1)));

		QuestShadowBatchReport report = QuestShadowBatchRunner.compare(runner,
			List.of(envelope(observation)), Set.of(QUEST_ID));

		assertEquals(report.expectedOwners(), report.coveredOwners());
		assertEquals(2, report.expectedCoverage().size());
		assertEquals(1, report.coveredCoverage().size());
		assertEquals(1, report.missingCoverage().size());
		assertFalse(report.complete());
	}

	@Test
	void oneInvocationCoversOnlyTheWinningPriorityPath() {
		var builder = QuestDsl.quest(QUEST_ID)
			.evidence(new EvidenceRef("test", "shadow-batch", "priority fixture"))
			.progress(bitField("step", 0, 6, PersistenceMode.PERSISTENT))
			.node("start", project(QuestStatus.START, vars("step", 0)))
			.node("preferred", project(QuestStatus.REWARD, vars("step", 1)))
			.node("fallback", project(QuestStatus.COMPLETE, vars("step", 2)));
		builder.on(talkToNpc(700001)).from("start").priority(10)
			.then(setVariable("step", 1)).goTo("preferred");
		builder.on(talkToNpc(700001)).from("start").priority(20)
			.then(setVariable("step", 2)).then(completeQuest(0)).goTo("fallback")
			.afterCommit(syncQuestState(QuestStateSyncMode.COMPLETION));
		QuestShadowRunner runner = new QuestShadowRunner(new ImmutableQuestCatalog(List.of(builder.compile())));
		QuestShadowObservation observation = observation(true, QuestStatus.REWARD, 1, QuestRouteResult.HANDLED,
			List.of(new com.aionemu.gameserver.questEngine.definition.QuestAction.SetVariable("step", 1)));

		QuestShadowBatchReport report = QuestShadowBatchRunner.compare(runner,
			List.of(envelope(observation)), Set.of(QUEST_ID));

		assertEquals(Set.of(10), report.coveredCoverage().stream()
			.map(QuestShadowCoverageKey::priority).collect(java.util.stream.Collectors.toSet()));
		assertEquals(Set.of(20), report.missingCoverage().stream()
			.map(QuestShadowCoverageKey::priority).collect(java.util.stream.Collectors.toSet()));
		assertTrue(report.comparisons().get(0).clean());
		assertFalse(report.complete());
	}

	@Test
	void reportCountsTypedDifferencesByDimension() {
		CompiledQuestDefinition definition = definition();
		QuestShadowRunner runner = new QuestShadowRunner(new ImmutableQuestCatalog(List.of(definition)));
		QuestShadowBatchRunner.Envelope envelope = envelope(observation(true, QuestStatus.START, 2, QuestRouteResult.NOT_HANDLED,
			List.of(new com.aionemu.gameserver.questEngine.definition.QuestAction.GrantReward("AP", 0, 1))));
		QuestShadowBatchReport report = QuestShadowBatchRunner.compare(runner, List.of(envelope), Set.of(QUEST_ID));

		assertFalse(report.clean());
		assertEquals(1, report.differenceCounts().get(QuestShadowDifferenceKind.VARIABLES));
		assertEquals(1, report.differenceCounts().get(QuestShadowDifferenceKind.REWARD));
		assertEquals(2, report.differenceCounts().get(QuestShadowDifferenceKind.RESULT_CONSUMPTION));
		QuestShadowComparison comparison = report.comparisons().get(0);
		assertEquals("TalkToNpc[npcId=700001, dialogId=null, interactionObjectId=0]",
			comparison.eventSelector());
		assertEquals(List.of(new QuestShadowComparison.OwnerInput(QUEST_ID, QuestStatus.START, 0)),
			comparison.inputs());
	}

	@Test
	void everyTypedPathParticipatesInDiagnosticCoverage() {
		var builder = QuestDsl.quest(QUEST_ID)
			.evidence(new EvidenceRef("test", "shadow-batch", "offline coverage fixture"))
			.progress(bitField("step", 0, 6, PersistenceMode.PERSISTENT))
			.node("start", project(QuestStatus.START, vars("step", 0)))
			.node("reward", project(QuestStatus.REWARD, vars("step", 1)));
		builder.on(talkToNpc(700001)).from("start").when(variableIs("step", 0))
			.then(setVariable("step", 1)).goTo("reward");
		builder.on(talkToNpc(700002)).from("start").when(variableIs("step", 0))
			.then(setVariable("step", 1)).goTo("reward");
		QuestShadowRunner runner = new QuestShadowRunner(new ImmutableQuestCatalog(List.of(builder.compile())));
		QuestShadowObservation actual = observation(true, QuestStatus.START, 2, QuestRouteResult.NOT_HANDLED,
			List.of(new com.aionemu.gameserver.questEngine.definition.QuestAction.GrantReward("AP", 0, 1)));

		QuestShadowBatchReport report = QuestShadowBatchRunner.compare(runner,
			List.of(envelope(actual)), Set.of(QUEST_ID));

		assertEquals(2, report.expectedCoverage().size());
		assertEquals(1, report.coveredCoverage().size());
		assertTrue(report.unexpectedCoverage().isEmpty());
		assertFalse(report.comparisons().get(0).clean(), "诊断路径必须保留 typed difference");
		assertFalse(report.complete(), "另一条任务路径尚未观察");
	}

	@Test
	void unexpectedOwnersAreExposedAsCatalogDrift() {
		QuestShadowRunner runner = new QuestShadowRunner(new ImmutableQuestCatalog(List.of(definition())));
		QuestShadowBatchRunner.Envelope envelope = envelope(observation(true, QuestStatus.REWARD, 1,
			QuestRouteResult.HANDLED, List.of(new com.aionemu.gameserver.questEngine.definition.QuestAction.SetVariable("step", 1))));

		QuestShadowBatchReport report = QuestShadowBatchRunner.compare(runner, List.of(envelope), Set.of(1002));
		assertFalse(report.complete());
		assertEquals(Set.of(1002), report.missingOwners());
		assertEquals(Set.of(QUEST_ID), report.unexpectedOwners());
	}

	@Test
	void legacySiblingOwnersOutsideTheCandidateCatalogAreIgnored() {
		QuestShadowRunner runner = new QuestShadowRunner(new ImmutableQuestCatalog(List.of(definition())));
		QuestShadowObservation.Owner candidateOwner = observation(true, QuestStatus.REWARD, 1,
			QuestRouteResult.HANDLED,
			List.of(new com.aionemu.gameserver.questEngine.definition.QuestAction.SetVariable("step", 1)))
			.owners().get(QUEST_ID);
		QuestShadowObservation.Owner siblingOwner = new QuestShadowObservation.Owner(1002, true,
			QuestStatus.START, 0, List.of(), List.of(), QuestRouteResult.HANDLED);
		QuestShadowObservation observation = new QuestShadowObservation(
			Map.of(QUEST_ID, candidateOwner, 1002, siblingOwner), true);
		QuestShadowBatchRunner.Envelope envelope = new QuestShadowBatchRunner.Envelope(talkToNpc(700001),
			Map.of(
				QUEST_ID, new QuestSnapshot(PLAYER_ID, QUEST_ID, QuestStatus.START, 0, Map.of()),
				1002, new QuestSnapshot(PLAYER_ID, 1002, QuestStatus.START, 0, Map.of())),
			observation,
			List.of(
				new QuestLegacyInvocation(PLAYER_ID, QUEST_ID, "TALK_TO_NPC",
					QuestDispatchContract.EXCLUSIVE,
					new QuestShadowObservation(Map.of(QUEST_ID, candidateOwner), true)),
				new QuestLegacyInvocation(PLAYER_ID, 1002, "TALK_TO_NPC",
					QuestDispatchContract.EXCLUSIVE,
					new QuestShadowObservation(Map.of(1002, siblingOwner), true))));

		QuestShadowBatchReport report = QuestShadowBatchRunner.compare(runner, List.of(envelope), Set.of(QUEST_ID));

		assertTrue(report.clean());
		assertEquals(Set.of(QUEST_ID), report.coveredOwners());
		assertEquals(Set.of(), report.unexpectedOwners());
	}

	@Test
	void consecutiveBatchesUnionPathCoverageAndKeepDifferencesStickyWithoutUnboundedSamples() {
		var builder = QuestDsl.quest(QUEST_ID)
			.evidence(new EvidenceRef("test", "shadow-batch", "merge fixture"))
			.progress(bitField("step", 0, 6, PersistenceMode.PERSISTENT))
			.node("start", project(QuestStatus.START, vars("step", 0)))
			.node("middle", project(QuestStatus.START, vars("step", 1)))
			.node("reward", project(QuestStatus.REWARD, vars("step", 2)));
		builder.on(talkToNpc(700001)).from("start").when(variableIs("step", 0))
			.then(setVariable("step", 1)).goTo("middle");
		builder.on(talkToNpc(700001)).from("middle").when(variableIs("step", 1))
			.then(setVariable("step", 2)).goTo("reward");
		QuestShadowRunner runner = new QuestShadowRunner(new ImmutableQuestCatalog(List.of(builder.compile())));
		QuestShadowBatchRunner.Envelope firstEnvelope = new QuestShadowBatchRunner.Envelope(talkToNpc(700001),
			Map.of(QUEST_ID, new QuestSnapshot(PLAYER_ID, QUEST_ID, QuestStatus.START, 0, Map.of())),
			observation(true, QuestStatus.START, 5, QuestRouteResult.HANDLED,
				List.of(new com.aionemu.gameserver.questEngine.definition.QuestAction.SetVariable("step", 1))),
			List.of(new QuestLegacyInvocation(PLAYER_ID, QUEST_ID, "TALK_TO_NPC", QuestDispatchContract.EXCLUSIVE,
				observation(true, QuestStatus.START, 5, QuestRouteResult.HANDLED,
					List.of(new com.aionemu.gameserver.questEngine.definition.QuestAction.SetVariable("step", 1))))));
		QuestShadowBatchRunner.Envelope secondEnvelope = new QuestShadowBatchRunner.Envelope(talkToNpc(700001),
			Map.of(QUEST_ID, new QuestSnapshot(PLAYER_ID, QUEST_ID, QuestStatus.START, 1, Map.of())),
			observation(true, QuestStatus.REWARD, 2, QuestRouteResult.HANDLED,
				List.of(new com.aionemu.gameserver.questEngine.definition.QuestAction.SetVariable("step", 2))),
			List.of(new QuestLegacyInvocation(PLAYER_ID, QUEST_ID, "TALK_TO_NPC", QuestDispatchContract.EXCLUSIVE,
				observation(true, QuestStatus.REWARD, 2, QuestRouteResult.HANDLED,
					List.of(new com.aionemu.gameserver.questEngine.definition.QuestAction.SetVariable("step", 2))))));

		QuestShadowBatchReport first = QuestShadowBatchRunner.compare(runner, List.of(firstEnvelope), Set.of(QUEST_ID));
		QuestShadowBatchReport second = QuestShadowBatchRunner.compare(runner, List.of(secondEnvelope), Set.of(QUEST_ID));
		QuestShadowBatchReport merged = first.merge(second).merge(first).merge(second);

		assertTrue(merged.complete());
		assertFalse(merged.clean());
		assertEquals(2, merged.coveredCoverage().size());
		assertEquals(Map.of(QuestShadowDifferenceKind.VARIABLES, 1), merged.differenceCounts());
		assertEquals(1, merged.comparisons().size(), "重复样本不得使累计报告无界增长");
	}

	private static QuestShadowBatchRunner.Envelope envelope(QuestShadowObservation observation) {
		return new QuestShadowBatchRunner.Envelope(talkToNpc(700001),
			Map.of(QUEST_ID, new QuestSnapshot(PLAYER_ID, QUEST_ID, QuestStatus.START, 0, Map.of())), observation,
			List.of(new QuestLegacyInvocation(PLAYER_ID, QUEST_ID, "TALK_TO_NPC",
				QuestDispatchContract.EXCLUSIVE, observation)));
	}

	private static QuestShadowObservation observation(boolean matched, QuestStatus status, int packedVariables,
			QuestRouteResult result, List<com.aionemu.gameserver.questEngine.definition.QuestAction> actions) {
		return new QuestShadowObservation(Map.of(QUEST_ID, new QuestShadowObservation.Owner(QUEST_ID,
				matched, status, packedVariables, actions, List.of(), result)), result == QuestRouteResult.HANDLED);
	}

	private static CompiledQuestDefinition definition() {
		return QuestDsl.quest(QUEST_ID)
				.evidence(new EvidenceRef("test", "shadow-batch", "fixture"))
				.progress(bitField("step", 0, 6, PersistenceMode.PERSISTENT))
				.node("start", project(QuestStatus.START, vars("step", 0)))
				.node("reward", project(QuestStatus.REWARD, vars("step", 1)))
				.on(talkToNpc(700001)).when(statusIs(QuestStatus.START)).when(variableIs("step", 0))
					.then(setVariable("step", 1)).goTo("reward")
				.compile();
	}
}
