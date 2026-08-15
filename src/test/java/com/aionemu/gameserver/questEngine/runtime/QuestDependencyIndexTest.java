package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
import com.aionemu.gameserver.questEngine.definition.ImmutableQuestCatalog;
import com.aionemu.gameserver.questEngine.definition.PersistenceMode;
import com.aionemu.gameserver.questEngine.definition.QuestDefinitionXmlCompiler;
import com.aionemu.gameserver.questEngine.definition.QuestDsl;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;

import static com.aionemu.gameserver.questEngine.definition.QuestDsl.bitField;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.levelUp;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.project;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.questsFinished;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.vars;
import static org.junit.jupiter.api.Assertions.assertEquals;

class QuestDependencyIndexTest {
	@Test
	void indexesOnlyLevelUpOwnersByExplicitQuestDependency() {
		var first = QuestDsl.quest(1001)
			.progress(bitField("var0", 0, 6, PersistenceMode.PERSISTENT))
			.node("unaccepted", project(QuestStatus.NONE, vars("var0", 0)))
			.node("started", project(QuestStatus.START, vars("var0", 0)))
			.on(levelUp()).from("unaccepted").when(questsFinished(1100)).goTo("started")
			.compile();
		var second = QuestDsl.quest(1002)
			.progress(bitField("var0", 0, 6, PersistenceMode.PERSISTENT))
			.node("unaccepted", project(QuestStatus.NONE, vars("var0", 0)))
			.node("started", project(QuestStatus.START, vars("var0", 0)))
			.on(levelUp()).from("unaccepted").when(questsFinished(1100)).goTo("started")
			.compile();
		var unrelatedEvent = QuestDsl.quest(1003)
			.progress(bitField("var0", 0, 6, PersistenceMode.PERSISTENT))
			.node("unaccepted", project(QuestStatus.NONE, vars("var0", 0)))
			.on(QuestDsl.zoneMissionEnd()).from("unaccepted").when(questsFinished(1100)).goTo("unaccepted")
			.compile();

		QuestDependencyIndex index = new QuestDependencyIndex(
			new ImmutableQuestCatalog(List.of(second, unrelatedEvent, first)));

		assertEquals(List.of(1001, 1002), index.dependentsOf(1100));
		assertEquals(List.of(), index.dependentsOf(1200));
	}

	@Test
	void indexesCanonicalMetadataPrerequisitesAndStartConditions() throws Exception {
		var prerequisiteOwner = compile(10011);
		var startConditionOwner = compile(10032);
		QuestDependencyIndex index = new QuestDependencyIndex(
			new ImmutableQuestCatalog(List.of(prerequisiteOwner, startConditionOwner)));

		assertEquals(List.of(10011), index.dependentsOf(10010));
		assertEquals(List.of(10032), index.dependentsOf(10031));
		assertEquals(List.of(10032), index.dependentsOf(10025));
		assertEquals(List.of(10032), index.dependentsOf(14062));
	}

	private static CompiledQuestDefinition compile(int questId) throws Exception {
		try (InputStream input = QuestDependencyIndexTest.class.getResourceAsStream(
				"/aion/data/static_data/quest_definition/quests/" + questId + ".xml")) {
			if (input == null) {
				throw new IllegalStateException("missing quest definition " + questId);
			}
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
