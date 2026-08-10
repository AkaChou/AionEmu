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
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class Quest14010EnterZoneProtocolTest {
	@Test
	void enteringVerteronStartsTheMissionWithoutOpeningQuestHtml() throws Exception {
		CompiledQuestDefinition definition = definition();
		QuestTransition enterZone = definition.definition().transitions().stream()
			.filter(transition -> transition.sourceNode().equals("unaccepted"))
			.filter(transition -> transition.targetNode().equals("started"))
			.filter(transition -> transition.event().equals(
				new QuestEvent.EnterZone("VERTERON_CITADEL_210030000")))
			.findFirst().orElseThrow();
		QuestEvent.EnterZone event = new QuestEvent.EnterZone("VERTERON_CITADEL_210030000");
		QuestSnapshot snapshot = new QuestSnapshot(7, 14010, QuestStatus.NONE, 0, Map.of())
			.withStartEligibility(QuestStartEligibility.allowed())
			.withCompletedQuestIds(Set.of())
			.withActiveQuestIds(Set.of());

		QuestMutationPlan plan = QuestMutationPlanner.plan(definition, snapshot, event, enterZone).orElseThrow();

		assertEquals(QuestStatus.START, plan.nextStatus());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH)),
			plan.afterCommit());
		assertFalse(plan.afterCommit().stream().anyMatch(AfterCommitAction.ShowQuestDialog.class::isInstance));
	}

	private CompiledQuestDefinition definition() throws Exception {
		try (InputStream input = getClass().getResourceAsStream(
			"/aion/data/static_data/quest_definition/quests/14010.xml")) {
			if (input == null) {
				throw new IllegalStateException("missing quest 14010 definition");
			}
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
