package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlanner;
import com.aionemu.gameserver.questEngine.runtime.QuestSnapshot;
import com.aionemu.gameserver.questEngine.runtime.QuestStartEligibility;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Quest24053PrerequisiteTest {
	@Test
	void keepsTheLegacyPrerequisitePerAutomaticStartEvent() throws Exception {
		CompiledQuestDefinition definition = load(24053);
		QuestTransition levelUp = definition.definition().transitions().stream()
			.filter(transition -> transition.event() instanceof QuestEvent.LevelUp)
			.findFirst().orElseThrow();
		QuestTransition zoneMissionEnd = definition.definition().transitions().stream()
			.filter(transition -> transition.event() instanceof QuestEvent.ZoneMissionEnd)
			.findFirst().orElseThrow();

		QuestSnapshot noPrerequisites = snapshot(Set.of());
		QuestSnapshot levelUpReady = snapshot(Set.of(24050));
		QuestSnapshot zoneMissionReady = snapshot(Set.of(24040));

		assertFalse(QuestMutationPlanner.plan(definition, noPrerequisites,
			new QuestEvent.LevelUp(), levelUp).isPresent());
		assertTrue(QuestMutationPlanner.plan(definition, levelUpReady,
			new QuestEvent.LevelUp(), levelUp).isPresent());
		assertFalse(QuestMutationPlanner.plan(definition, noPrerequisites,
			new QuestEvent.ZoneMissionEnd(), zoneMissionEnd).isPresent());
		assertTrue(QuestMutationPlanner.plan(definition, zoneMissionReady,
			new QuestEvent.ZoneMissionEnd(), zoneMissionEnd).isPresent());
	}

	@Test
	void broadcastsTheBeluslanMissionChainAfterTheNoRewardCompletionPath() throws Exception {
		CompiledQuestDefinition definition = load(24050);
		QuestTransition noReward = definition.definition().transitions().stream()
			.filter(transition -> transition.event().equals(new QuestEvent.TalkToNpc(204702, 23)))
			.findFirst().orElseThrow();

		assertTrue(noReward.targetNode().equals("complete"));
		AfterCommitAction.BroadcastZoneMissionEnd broadcast = noReward.afterCommit().stream()
			.filter(AfterCommitAction.BroadcastZoneMissionEnd.class::isInstance)
			.map(AfterCommitAction.BroadcastZoneMissionEnd.class::cast)
			.findFirst().orElseThrow();
		assertArrayEquals(new int[] {24051, 24052, 24053, 24054}, broadcast.questIds());
	}

	private static QuestSnapshot snapshot(Set<Integer> completedQuestIds) {
		return new QuestSnapshot(7, 24053, QuestStatus.NONE, 0, Map.of())
			.withStartEligibility(QuestStartEligibility.allowed())
			.withCompletedQuestIds(completedQuestIds);
	}

	private static CompiledQuestDefinition load(int questId) throws Exception {
		try (InputStream input = Quest24053PrerequisiteTest.class.getResourceAsStream(
			"/aion/data/static_data/quest_definition/quests/" + questId + ".xml")) {
			return QuestDefinitionXmlCompiler.compile(Objects.requireNonNull(input));
		}
	}
}
