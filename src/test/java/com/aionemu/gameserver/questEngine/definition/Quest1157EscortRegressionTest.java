package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlanner;
import com.aionemu.gameserver.questEngine.runtime.QuestSnapshot;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Quest1157EscortRegressionTest {
	@Test
	void movieStartsOnlyAfterMimitiReachesGaphyrk() {
		CompiledQuestDefinition definition = load();
		QuestTransition attack = definition.definition().transitions().stream()
			.filter(transition -> transition.sourceNode().equals("started")
				&& transition.event().equals(new QuestEvent.AttackNpc(210319)))
			.findFirst().orElseThrow();

		assertTrue(attack.conditions().isEmpty());
		assertEquals(List.of(new AfterCommitAction.WatchLuredNpcCoordinate(892, 2024, 166, 13)),
			attack.afterCommit());

		QuestSnapshot started = new QuestSnapshot(7, 1157, QuestStatus.START, 0, Map.of());
		var attackPlan = QuestMutationPlanner.plan(definition, started, attack(), attack).orElseThrow();
		assertEquals(QuestStatus.START, attackPlan.nextStatus());
		assertTrue(attackPlan.afterCommit().stream().noneMatch(AfterCommitAction.PlayMovie.class::isInstance));

		QuestTransition reached = definition.definition().transitions().stream()
			.filter(transition -> transition.sourceNode().equals("started")
				&& transition.targetNode().equals("started")
				&& transition.event().equals(new QuestEvent.NpcReachTarget()))
			.findFirst().orElseThrow();
		var reachedPlan = QuestMutationPlanner.plan(
			definition, started, new QuestEvent.NpcReachTarget(), reached).orElseThrow();
		assertEquals(QuestStatus.START, reachedPlan.nextStatus());
		assertEquals(List.of(new AfterCommitAction.PlayMovie(17)), reachedPlan.afterCommit());

		QuestTransition movieEnd = definition.definition().transitions().stream()
			.filter(transition -> transition.sourceNode().equals("started")
				&& transition.targetNode().equals("reward")
				&& transition.event().equals(new QuestEvent.MovieEnd(17)))
			.findFirst().orElseThrow();
		assertEquals(QuestStatus.REWARD,
			QuestMutationPlanner.plan(definition, started, new QuestEvent.MovieEnd(17), movieEnd)
				.orElseThrow().nextStatus());
	}

	private static QuestEvent.AttackNpc attack() {
		return new QuestEvent.AttackNpc(210319,
			new QuestNpcAttackFacts(7, 20, 210319, 1000, 1000, 210030000, 1));
	}

	private static CompiledQuestDefinition load() {
		String resource = "/aion/data/static_data/quest_definition/quests/1157.xml";
		try (InputStream input = Objects.requireNonNull(
				Quest1157EscortRegressionTest.class.getResourceAsStream(resource), resource)) {
			return QuestDefinitionXmlCompiler.compile(input);
		} catch (Exception e) {
			throw new AssertionError("unable to load " + resource, e);
		}
	}
}
