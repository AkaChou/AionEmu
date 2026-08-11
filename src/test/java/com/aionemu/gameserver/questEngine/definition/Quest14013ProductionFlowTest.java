package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlan;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlanner;
import com.aionemu.gameserver.questEngine.runtime.QuestSnapshot;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Quest14013ProductionFlowTest {
	@Test
	void requiresTheNpcBriefingThenTracksBothClientKillSections() throws Exception {
		CompiledQuestDefinition definition = load();
		QuestSnapshot started = snapshot(QuestStatus.START, 0);

		QuestMutationPlan briefing = plan(definition, started, new QuestEvent.TalkToNpc(203129, 31));
		assertEquals(QuestStatus.START, briefing.nextStatus());
		assertEquals(0, briefing.nextPackedVariables());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(1011)), briefing.afterCommit());
		assertTrue(QuestMutationPlanner.plan(definition, started, new QuestEvent.KillNpc(210126),
			route(definition, "hunting", new QuestEvent.KillNpc(210126))).isEmpty());

		QuestMutationPlan accepted = plan(definition, started, new QuestEvent.TalkToNpc(203129, 10000));
		assertEquals(Map.of("var0", 1, "var1", 0, "var2", 0),
			definition.definition().progressLayout().unpack(accepted.nextPackedVariables()));
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
			new AfterCommitAction.CloseDialog()), accepted.afterCommit());

		QuestSnapshot hunting = snapshot(accepted.nextStatus(), accepted.nextPackedVariables());
		assertTrue(QuestMutationPlanner.plan(definition, hunting, new QuestEvent.KillNpc(210202),
			route(definition, "hunting", new QuestEvent.KillNpc(210202))).isEmpty());
		for (int i = 0; i < 5; i++) {
			QuestMutationPlan kill = plan(definition, hunting, new QuestEvent.KillNpc(210126));
			hunting = snapshot(kill.nextStatus(), kill.nextPackedVariables());
		}
		for (int i = 0; i < 7; i++) {
			QuestMutationPlan kill = plan(definition, hunting, new QuestEvent.KillNpc(210200));
			hunting = snapshot(kill.nextStatus(), kill.nextPackedVariables());
		}
		assertEquals(Map.of("var0", 1, "var1", 5, "var2", 7),
			definition.definition().progressLayout().unpack(hunting.packedVariables()));

		QuestMutationPlan finalKill = plan(definition, hunting, new QuestEvent.KillNpc(210202));
		assertEquals(QuestStatus.REWARD, finalKill.nextStatus());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(
			QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)), finalKill.afterCommit());
	}

	private static QuestMutationPlan plan(CompiledQuestDefinition definition, QuestSnapshot snapshot,
			QuestEvent event) {
		return QuestMutationPlanner.plan(definition, snapshot, event,
			route(definition, source(snapshot), event)).orElseThrow();
	}

	private static QuestTransition route(CompiledQuestDefinition definition, String source, QuestEvent event) {
		return definition.definition().transitions().stream()
			.filter(transition -> source.equals(transition.sourceNode())
				&& QuestEvent.matches(transition.event(), event))
			.findFirst().orElseThrow();
	}

	private static String source(QuestSnapshot snapshot) {
		return snapshot.packedVariables() == 0 ? "started" : "hunting";
	}

	private static QuestSnapshot snapshot(QuestStatus status, int packedVariables) {
		return new QuestSnapshot(7, 14013, status, packedVariables, Map.of());
	}

	private static CompiledQuestDefinition load() throws Exception {
		try (InputStream input = Quest14013ProductionFlowTest.class.getResourceAsStream(
			"/aion/data/static_data/quest_definition/quests/14013.xml")) {
			return QuestDefinitionXmlCompiler.compile(Objects.requireNonNull(input));
		}
	}
}
