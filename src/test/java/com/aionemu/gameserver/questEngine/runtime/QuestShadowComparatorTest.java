package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
import com.aionemu.gameserver.questEngine.definition.EvidenceRef;
import com.aionemu.gameserver.questEngine.definition.ImmutableQuestCatalog;
import com.aionemu.gameserver.questEngine.definition.PersistenceMode;
import com.aionemu.gameserver.questEngine.definition.QuestAction;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static com.aionemu.gameserver.questEngine.definition.QuestDsl.bitField;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.project;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.quest;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.removeItem;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.setVariable;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.statusIs;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.talkToNpc;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.variableIs;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.vars;
import static org.junit.jupiter.api.Assertions.assertEquals;

class QuestShadowComparatorTest {
	private static final EvidenceRef EVIDENCE = new EvidenceRef("test", "shadow-diff", "fixture");

	@Test
	void reportsTypedVariableRewardProtocolAndConsumptionDifferences() {
		CompiledQuestDefinition definition = quest(1001)
				.evidence(EVIDENCE)
				.progress(bitField("step", 0, 6, PersistenceMode.PERSISTENT))
				.node("start", project(QuestStatus.START, vars("step", 0)))
				.node("reward", project(QuestStatus.REWARD, vars("step", 1)))
				.on(talkToNpc(700001)).when(statusIs(QuestStatus.START)).when(variableIs("step", 0))
				.then(setVariable("step", 1)).goTo("reward").compile();
		QuestShadowRunner.QuestShadowResult candidate = new QuestShadowRunner(
				new ImmutableQuestCatalog(List.of(definition))).inspect(talkToNpc(700001), Map.of(1001,
				new QuestSnapshot(7, 1001, QuestStatus.START, 0, Map.of())));
		QuestShadowObservation actual = new QuestShadowObservation(Map.of(1001,
				new QuestShadowObservation.Owner(1001, true, QuestStatus.START, 2,
						List.of(new QuestAction.GrantReward("AP", 0, 1)), List.of(), QuestRouteResult.NOT_HANDLED)), false);

		assertEquals(List.of(
				new QuestShadowDifference(QuestShadowDifferenceKind.RESULT_CONSUMPTION, 0),
				new QuestShadowDifference(QuestShadowDifferenceKind.VARIABLES, 1001),
				new QuestShadowDifference(QuestShadowDifferenceKind.REWARD, 1001),
				new QuestShadowDifference(QuestShadowDifferenceKind.RESULT_CONSUMPTION, 1001)),
				QuestShadowComparator.compare(candidate, actual));
	}

	@Test
	void reportsMissingRequiredItemRemoval() {
		CompiledQuestDefinition definition = quest(1004)
			.evidence(EVIDENCE)
			.progress(bitField("step", 0, 6, PersistenceMode.PERSISTENT))
			.node("start", project(QuestStatus.START, vars("step", 0)))
			.node("reward", project(QuestStatus.REWARD, vars("step", 1)))
			.on(talkToNpc(700001)).when(statusIs(QuestStatus.START))
			.then(removeItem(182400001, 1)).then(setVariable("step", 1)).goTo("reward").compile();
		QuestShadowRunner.QuestShadowResult candidate = new QuestShadowRunner(
			new ImmutableQuestCatalog(List.of(definition))).inspect(talkToNpc(700001), Map.of(1004,
			new QuestSnapshot(7, 1004, QuestStatus.START, 0, Map.of(182400001, 1))));
		QuestShadowObservation actual = new QuestShadowObservation(Map.of(1004,
			new QuestShadowObservation.Owner(1004, true, QuestStatus.REWARD, 1,
				List.of(), List.of(), QuestRouteResult.HANDLED)), true);

		assertEquals(List.of(new QuestShadowDifference(QuestShadowDifferenceKind.ACTION, 1004)),
			QuestShadowComparator.compare(candidate, actual));
	}

	@Test
	void groupsMultipleVariableBranchesForOneOwner() {
		var builder = quest(1002)
				.evidence(EVIDENCE)
				.progress(bitField("step", 0, 6, PersistenceMode.PERSISTENT))
				.node("start", project(QuestStatus.START, vars("step", 0)))
				.node("middle", project(QuestStatus.START, vars("step", 1)))
				.node("end", project(QuestStatus.REWARD, vars("step", 2)));
		builder.on(talkToNpc(700001)).from("start").when(variableIs("step", 0))
				.then(setVariable("step", 1)).goTo("middle");
		builder.on(talkToNpc(700001)).from("middle").when(variableIs("step", 1))
				.then(setVariable("step", 2)).goTo("end");
		CompiledQuestDefinition definition = builder.compile();
		QuestShadowRunner.QuestShadowResult candidate = new QuestShadowRunner(
				new ImmutableQuestCatalog(List.of(definition))).inspect(talkToNpc(700001), Map.of(1002,
				new QuestSnapshot(7, 1002, QuestStatus.START, 0, Map.of())));
		QuestShadowObservation actual = new QuestShadowObservation(Map.of(1002,
				new QuestShadowObservation.Owner(1002, true, QuestStatus.START, 1,
						List.of(new QuestAction.SetVariable("step", 1)), List.of(), QuestRouteResult.HANDLED)), true);

		assertEquals(List.of(), QuestShadowComparator.compare(candidate, actual));
	}

	@Test
	void comparesAnActualLegacyInvocationAndKeepsItInTheObservationStore() {
		CompiledQuestDefinition definition = quest(1003)
			.evidence(EVIDENCE)
			.progress(bitField("step", 0, 6, PersistenceMode.PERSISTENT))
			.node("start", project(QuestStatus.START, vars("step", 0)))
			.node("reward", project(QuestStatus.REWARD, vars("step", 1)))
			.on(talkToNpc(700001)).when(statusIs(QuestStatus.START)).when(variableIs("step", 0))
			.then(setVariable("step", 1)).goTo("reward").compile();
		QuestShadowRunner runner = new QuestShadowRunner(new ImmutableQuestCatalog(List.of(definition)));
		QuestLegacyObservationStore store = new QuestLegacyObservationStore();
		QuestLegacyInvocation actual = new QuestLegacyInvocation(7, 1003, "TALK_TO_NPC",
			QuestDispatchContract.EXCLUSIVE, new QuestShadowObservation(Map.of(1003,
				new QuestShadowObservation.Owner(1003, true, QuestStatus.REWARD, 1,
					List.of(new QuestAction.SetVariable("step", 1)), List.of(), QuestRouteResult.HANDLED)), true));
		store.record(actual);

		assertEquals(List.of(), store.compare(runner, actual, talkToNpc(700001), Map.of(1003,
			new QuestSnapshot(7, 1003, QuestStatus.START, 0, Map.of()))));
		assertEquals(List.of(actual), store.snapshot());
	}
}
