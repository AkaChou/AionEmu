package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
import com.aionemu.gameserver.questEngine.definition.QuestDefinitionXmlCompiler;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Quest1842RepeatLifecycleTest {
	@Test
	void completedQuestCanRestartAndResetsBothKillCounters() throws Exception {
		CompiledQuestDefinition definition;
		try (InputStream input = Objects.requireNonNull(getClass().getResourceAsStream(
			"/aion/data/static_data/quest_definition/quests/1842.xml"))) {
			definition = QuestDefinitionXmlCompiler.compile(input);
		}
		QuestEvent event = new QuestEvent.TalkToNpc(268080, 1002);
		var transition = definition.definition().transitions().stream()
			.filter(candidate -> candidate.sourceNode().equals("unaccepted"))
			.filter(candidate -> candidate.event().equals(event))
			.findFirst().orElseThrow();
		int completedVariables = definition.definition().progressLayout().pack(Map.of("var0", 80, "var1", 1));
		QuestSnapshot completed = new QuestSnapshot(7, 1842, QuestStatus.COMPLETE, completedVariables,
			Map.of(), Map.of()).withStartEligibility(QuestStartEligibility.allowed());

		var plan = QuestMutationPlanner.plan(definition, completed, event, transition);

		assertTrue(plan.isPresent());
		assertEquals(QuestStatus.START, plan.orElseThrow().nextStatus());
		assertEquals(0, plan.orElseThrow().nextPackedVariables());
	}
}
