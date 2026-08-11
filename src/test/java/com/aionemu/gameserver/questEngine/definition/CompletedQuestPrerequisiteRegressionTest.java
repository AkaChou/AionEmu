package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlan;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlanner;
import com.aionemu.gameserver.questEngine.runtime.QuestSnapshot;
import com.aionemu.gameserver.questEngine.runtime.QuestStartEligibility;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompletedQuestPrerequisiteRegressionTest {
	@Test
	void gatesInggisonMissionAutomaticStartsOnAllFourPriorMissions() throws Exception {
		CompiledQuestDefinition definition = load(10035);
		for (QuestEvent event : new QuestEvent[] {new QuestEvent.LevelUp(), new QuestEvent.ZoneMissionEnd()}) {
			assertFalse(plan(definition, event, Set.of()).isPresent());
			assertFalse(plan(definition, event, Set.of(10031, 10032, 10033)).isPresent());
			assertTrue(plan(definition, event, Set.of(10031, 10032, 10033, 10034)).isPresent());
		}
	}

	@Test
	void gatesViolaLevelUpStartOn10521() throws Exception {
		CompiledQuestDefinition definition = load(10110);
		QuestEvent event = new QuestEvent.LevelUp();
		assertFalse(plan(definition, event, Set.of()).isPresent());
		assertTrue(plan(definition, event, Set.of(10521)).isPresent());
	}

	@Test
	void gatesFragmentsInTheSkyLevelUpStartOn14010() throws Exception {
		CompiledQuestDefinition definition = load(14011);
		QuestEvent event = new QuestEvent.LevelUp();
		assertFalse(plan(definition, event, Set.of()).isPresent());
		assertTrue(plan(definition, event, Set.of(14010)).isPresent());
	}

	@Test
	void startsAFrillOfAFussWhenTheZoneMissionUnlocksAtLevelFourteen() throws Exception {
		CompiledQuestDefinition definition = load(14013);
		QuestMutationPlan plan = plan(definition, new QuestEvent.ZoneMissionEnd(), Set.of()).orElseThrow();
		assertEquals(QuestStatus.START, plan.nextStatus());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(4)), plan.afterCommit());
	}

	@Test
	void gatesOtherCompletedAutomaticOwnersOnTheirLegacyPrerequisites() throws Exception {
		assertAutomaticStart(2007, new QuestEvent.LevelUp(),
			Set.of(2100, 2001, 2002, 2003, 2004, 2005, 2006));
		assertAutomaticStart(2007, new QuestEvent.ZoneMissionEnd(),
			Set.of(2100, 2001, 2002, 2003, 2004, 2005, 2006));
		assertAutomaticStart(10032, new QuestEvent.LevelUp(), Set.of(10031));
		assertAutomaticStart(14012, new QuestEvent.LevelUp(), Set.of(14010));
		assertAutomaticStart(14013, new QuestEvent.LevelUp(), Set.of(14010));
		assertAutomaticStart(14014, new QuestEvent.LevelUp(), Set.of(14010));
		assertAutomaticStart(14016, new QuestEvent.LevelUp(), Set.of(14010, 14011, 14012, 14013, 14014, 14015));
		assertAutomaticStart(14016, new QuestEvent.ZoneMissionEnd(), Set.of(14010, 14011, 14012, 14013, 14014, 14015));
		assertAutomaticStart(20032, new QuestEvent.LevelUp(), Set.of(20031));
		assertAutomaticStart(2947, new QuestEvent.LevelUp(), Set.of(2946));
	}

	@Test
	void gatesThe2022ChainOnUnfinishedAndUnacquired2022InsteadOfACompleted24010() throws Exception {
		// 客户端证据:24011/24016 的 start-conditions 为 unfinished+noacquired Q2022,
		// 无 24010 完成前置(客户端 quest.xml 无该依赖);快照含 Q2022 的 START/COMPLETE
		// 均拒,从未接取(且事实已采集)时可接,未采集事实失败关闭。
		for (int questId : new int[] {24011, 24016}) {
			CompiledQuestDefinition definition = load(questId);
			for (QuestEvent event : new QuestEvent[] {new QuestEvent.LevelUp(), new QuestEvent.ZoneMissionEnd()}) {
				assertFalse(planUncaptured(definition, event).isPresent(),
					"quest " + questId + " must fail closed without captured quest facts");
				assertFalse(plan(definition, event, Set.of(2022), Set.of()).isPresent(),
					"quest " + questId + " must reject while 2022 is completed");
				assertFalse(plan(definition, event, Set.of(), Set.of(2022)).isPresent(),
					"quest " + questId + " must reject while 2022 is in progress");
				assertTrue(plan(definition, event, Set.of(), Set.of()).isPresent(),
					"quest " + questId + " must start when 2022 is neither finished nor acquired");
			}
		}
	}

	private static void assertAutomaticStart(int questId, QuestEvent event, Set<Integer> prerequisites)
		throws Exception {
		CompiledQuestDefinition definition = load(questId);
		assertFalse(plan(definition, event, Set.of()).isPresent(),
			"quest " + questId + " must fail without prerequisites");
		assertTrue(plan(definition, event, prerequisites).isPresent(),
			"quest " + questId + " must start after prerequisites");
	}

	private static java.util.Optional<QuestMutationPlan> plan(CompiledQuestDefinition definition, QuestEvent event,
			Set<Integer> completedQuestIds) {
		return plan(definition, event, completedQuestIds, Set.of());
	}

	private static java.util.Optional<QuestMutationPlan> plan(CompiledQuestDefinition definition, QuestEvent event,
			Set<Integer> completedQuestIds, Set<Integer> activeQuestIds) {
		return QuestMutationPlanner.plan(definition,
			new QuestSnapshot(7, definition.definition().id(), QuestStatus.NONE, 0, Map.of())
				.withStartEligibility(QuestStartEligibility.allowed())
				.withCompletedQuestIds(completedQuestIds)
				.withActiveQuestIds(activeQuestIds),
			event, automaticStart(definition, event));
	}

	/** No completed/active facts captured: the planner must fail closed instead of guessing. */
	private static java.util.Optional<QuestMutationPlan> planUncaptured(CompiledQuestDefinition definition,
			QuestEvent event) {
		return QuestMutationPlanner.plan(definition,
			new QuestSnapshot(7, definition.definition().id(), QuestStatus.NONE, 0, Map.of())
				.withStartEligibility(QuestStartEligibility.allowed()),
			event, automaticStart(definition, event));
	}

	private static QuestTransition automaticStart(CompiledQuestDefinition definition, QuestEvent event) {
		return definition.definition().transitions().stream()
			.filter(transition -> transition.sourceNode().equals("unaccepted")
				&& transition.event().equals(event))
			.findFirst().orElseThrow();
	}

	private static CompiledQuestDefinition load(int questId) throws Exception {
		try (InputStream input = CompletedQuestPrerequisiteRegressionTest.class.getResourceAsStream(
			"/aion/data/static_data/quest_definition/quests/" + questId + ".xml")) {
			return QuestDefinitionXmlCompiler.compile(Objects.requireNonNull(input));
		}
	}
}
