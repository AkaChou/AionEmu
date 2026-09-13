package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class Quest14123ZoneSpawnTest {
	private static final Path XML = Path.of(
		"src/main/resources/aion/data/static_data/quest_definition/quests/14123.xml");
	private static final String SLOT = "peddler-hippola";
	private static final int TEMPLATE_ID = 206360;
	private static final AfterCommitAction SPAWN = new AfterCommitAction.SpawnNpc(SLOT, TEMPLATE_ID,
		new QuestSpawnLocation.Fixed(210020000, QuestInstanceTarget.currentOrDefault(),
			1768.16f, 924.47f, 422.02f, (byte) 0));

	@Test
	void spawnsPeddlerOnBothAcceptanceRoutes() throws Exception {
		QuestDefinition definition = load().definition();

		QuestTransition zoneAccept = transition(definition, "unaccepted", "started",
			new QuestEvent.EnterZone("ELTNEN_OBSERVATORY_210020000"));
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH),
			SPAWN,
			new AfterCommitAction.CloseDialog()), zoneAccept.afterCommit());

		QuestTransition dialogAccept = transition(definition, "unaccepted", "started",
			new QuestEvent.TalkToNpc(203933, QuestDialogAction.QUEST_ACCEPT_1.id()));
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH),
			SPAWN,
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.QUEST_ACCEPT_1.id())),
			dialogAccept.afterCommit());
	}

	@Test
	void restoresPeddlerWhenReenteringTheQuestZone() throws Exception {
		QuestDefinition definition = load().definition();

		QuestTransition reenter = transition(definition, "started", "started",
			new QuestEvent.EnterZone("ELTNEN_OBSERVATORY_210020000"));
		assertEquals(List.of(new QuestCondition.QuestVariableIs("var0", 0)), reenter.conditions());
		assertEquals(List.of(SPAWN), reenter.afterCommit());
	}

	@Test
	void restoresPeddlerAfterLoginAtTheQuestWorld() throws Exception {
		QuestDefinition definition = load().definition();

		QuestTransition login = transition(definition, "started", "started", new QuestEvent.EnterWorld());
		assertEquals(List.of(
			new QuestCondition.WorldIs(210020000, true),
			new QuestCondition.QuestVariableIs("var0", 0)), login.conditions());
		assertEquals(List.of(SPAWN), login.afterCommit());
	}

	@Test
	void keepsThePeddlerKillCountAdvancingToReport() throws Exception {
		QuestDefinition definition = load().definition();

		QuestTransition kill = transition(definition, "started", "report",
			new QuestEvent.KillNpc(TEMPLATE_ID));
		assertEquals(List.of(), kill.conditions());
		assertEquals(List.of(), kill.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
			kill.afterCommit());
	}

	private static QuestTransition transition(QuestDefinition definition, String source, String target,
			QuestEvent event) {
		return definition.transitions().stream()
			.filter(candidate -> source.equals(candidate.sourceNode()))
			.filter(candidate -> target.equals(candidate.targetNode()))
			.filter(candidate -> event.equals(candidate.event()))
			.findFirst().orElseThrow();
	}

	private static CompiledQuestDefinition load() throws Exception {
		try (InputStream input = Files.newInputStream(XML)) {
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
