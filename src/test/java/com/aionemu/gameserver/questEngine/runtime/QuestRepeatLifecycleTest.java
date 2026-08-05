package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
import com.aionemu.gameserver.questEngine.definition.QuestDefinitionXmlCompiler;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestRepeatLifecycleTest {
	@Test
	void repeatable15476CanStartAgainFromCompletedState() throws Exception {
		CompiledQuestDefinition definition;
		try (InputStream input = Objects.requireNonNull(getClass().getResourceAsStream(
			"/aion/data/static_data/quest_definition/quests/15476.xml"))) {
			definition = QuestDefinitionXmlCompiler.compile(input);
		}
		QuestEvent event = new QuestEvent.TalkToNpc(805809, 1002);
		var transition = definition.definition().transitions().stream()
			.filter(candidate -> candidate.sourceNode().equals("unaccepted"))
			.filter(candidate -> candidate.event().equals(event))
			.findFirst().orElseThrow();
		QuestSnapshot completed = new QuestSnapshot(7, 15476, QuestStatus.COMPLETE, 0,
			Map.of(), Map.of()).withStartEligibility(QuestStartEligibility.allowed());

		assertTrue(QuestMutationPlanner.plan(definition, completed, event, transition).isPresent());
	}
}
