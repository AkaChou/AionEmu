package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
import com.aionemu.gameserver.questEngine.definition.EvidenceRef;
import com.aionemu.gameserver.questEngine.definition.ImmutableQuestCatalog;
import com.aionemu.gameserver.questEngine.definition.PersistenceMode;
import com.aionemu.gameserver.questEngine.definition.QuestDsl;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static com.aionemu.gameserver.questEngine.definition.QuestDsl.bitField;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.project;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.quest;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.setVariable;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.statusIs;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.talkToNpc;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.variableIs;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.vars;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestShadowRunnerTest {
	private static final EvidenceRef EVIDENCE = new EvidenceRef("test", "shadow", "fixture");

	@Test
	void shadowOnlyBuildsPlansFromSuppliedImmutableFacts() {
		CompiledQuestDefinition definition = quest(1001)
				.evidence(EVIDENCE)
				.progress(bitField("step", 0, 6, PersistenceMode.PERSISTENT))
				.node("start", project(QuestStatus.START, vars("step", 0)))
				.node("reward", project(QuestStatus.REWARD, vars("step", 1)))
				.on(talkToNpc(700001)).when(statusIs(QuestStatus.START)).when(variableIs("step", 0))
				.then(setVariable("step", 1)).goTo("reward").compile();
		QuestShadowRunner runner = new QuestShadowRunner(new ImmutableQuestCatalog(List.of(definition)));
		QuestSnapshot snapshot = new QuestSnapshot(7, 1001, QuestStatus.START, 0, Map.of());

		QuestShadowRunner.QuestShadowResult result = runner.inspect(new QuestEvent.TalkToNpc(700001), Map.of(1001, snapshot));

		assertEquals(List.of(1001), result.owners().stream().map(QuestShadowRunner.QuestShadowOwner::questId).toList());
		assertTrue(result.hasCandidatePlan());
		assertEquals(QuestStatus.START, snapshot.status());
		assertEquals(0, snapshot.packedVariables());
		assertEquals(1, result.owners().get(0).plan().orElseThrow().nextPackedVariables());
	}

	@Test
	void missingSnapshotProducesRouteOnlyAndNeverInventsState() {
		CompiledQuestDefinition definition = quest(1001)
				.evidence(EVIDENCE)
				.node("start", project(QuestStatus.START, Map.of()))
				.on(talkToNpc(700001)).goTo("start").compile();
		QuestShadowRunner.QuestShadowResult result = new QuestShadowRunner(
				new ImmutableQuestCatalog(List.of(definition))).inspect(talkToNpc(700001), Map.of());

		assertEquals(1, result.owners().size());
		assertTrue(result.owners().get(0).plan().isEmpty());
		assertTrue(!result.hasCandidatePlan());
	}
}
