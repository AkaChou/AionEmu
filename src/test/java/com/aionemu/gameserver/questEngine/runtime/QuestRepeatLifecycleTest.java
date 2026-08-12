package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
import com.aionemu.gameserver.questEngine.definition.AfterCommitAction;
import com.aionemu.gameserver.questEngine.definition.QuestDefinitionXmlCompiler;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.definition.QuestTransition;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
			Map.of(), Map.of()).withStartEligibility(QuestStartEligibility.allowed())
			.withCompletedQuestIds(Set.of(15402));

		assertTrue(QuestMutationPlanner.plan(definition, completed, event, transition).isPresent());
	}

	@Test
	void repeatable1963ReopensItsStartPageThenAcceptsFromCompletedState() throws Exception {
		CompiledQuestDefinition definition = definition(1963);
		QuestSnapshot completed = new QuestSnapshot(7, 1963, QuestStatus.COMPLETE, 0,
			Map.of()).withStartEligibility(QuestStartEligibility.allowed());

		QuestEvent reopenEvent = new QuestEvent.TalkToNpc(203726, 31);
		QuestTransition reopen = route(definition, "complete", reopenEvent);
		var reopenPlan = QuestMutationPlanner.plan(definition, completed, reopenEvent, reopen).orElseThrow();
		assertEquals(QuestStatus.COMPLETE, reopenPlan.nextStatus());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(1011)), reopen.afterCommit());

		QuestEvent descriptionEvent = new QuestEvent.TalkToNpc(203726, 1007);
		QuestTransition description = route(definition, "complete", descriptionEvent);
		var descriptionPlan = QuestMutationPlanner.plan(definition, completed, descriptionEvent, description).orElseThrow();
		assertEquals(QuestStatus.COMPLETE, descriptionPlan.nextStatus());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(4)), description.afterCommit());

		QuestEvent acceptEvent = new QuestEvent.TalkToNpc(203726, 1002);
		QuestTransition accept = route(definition, "unaccepted", acceptEvent);
		var acceptPlan = QuestMutationPlanner.plan(definition, completed, acceptEvent, accept).orElseThrow();
		assertEquals(QuestStatus.START, acceptPlan.nextStatus());
		assertEquals(0, acceptPlan.nextPackedVariables());

		QuestSnapshot rejected = completed.withStartEligibility(QuestStartEligibility.rejected("REPEAT_LIMIT"));
		assertFalse(QuestMutationPlanner.plan(definition, rejected, reopenEvent, reopen).isPresent());
		assertFalse(QuestMutationPlanner.plan(definition, rejected, descriptionEvent, description).isPresent());
		assertFalse(QuestMutationPlanner.plan(definition, rejected, acceptEvent, accept).isPresent());
	}

	private static CompiledQuestDefinition definition(int questId) throws Exception {
		try (InputStream input = Objects.requireNonNull(QuestRepeatLifecycleTest.class.getResourceAsStream(
			"/aion/data/static_data/quest_definition/quests/" + questId + ".xml"))) {
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}

	private static QuestTransition route(CompiledQuestDefinition definition, String source, QuestEvent event) {
		return definition.definition().transitions().stream()
			.filter(transition -> source.equals(transition.sourceNode()))
			.filter(transition -> transition.event().equals(event))
			.findFirst().orElseThrow();
	}
}
