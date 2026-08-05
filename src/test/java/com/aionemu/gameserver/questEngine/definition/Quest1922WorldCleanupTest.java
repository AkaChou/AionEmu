package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertTrue;

class Quest1922WorldCleanupTest {
	@Test
	void deletesArenaNpcsAfterTheTenthKill() throws Exception {
		CompiledQuestDefinition definition = load();
		QuestTransition tenthKill = definition.definition().transitions().stream()
			.filter(transition -> transition.sourceNode().equals("s5")
				&& transition.event() instanceof QuestEvent.KillNpcSet
				&& transition.conditions().contains(new QuestCondition.QuestVariableIs("var4", 9)))
			.findFirst().orElseThrow();

		assertTrue(tenthKill.afterCommit().contains(new AfterCommitAction.DeleteWorldNpcs()));
	}

	private static CompiledQuestDefinition load() throws Exception {
		try (InputStream input = Quest1922WorldCleanupTest.class.getResourceAsStream(
			"/aion/data/static_data/quest_definition/quests/1922.xml")) {
			return QuestDefinitionXmlCompiler.compile(Objects.requireNonNull(input));
		}
	}
}
