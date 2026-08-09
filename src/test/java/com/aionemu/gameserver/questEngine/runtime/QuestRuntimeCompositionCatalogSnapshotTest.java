package com.aionemu.gameserver.questEngine.runtime;

import static com.aionemu.gameserver.questEngine.definition.QuestDsl.bitField;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.project;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.vars;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.util.List;
import java.util.function.IntFunction;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.questEngine.definition.ImmutableQuestCatalog;
import com.aionemu.gameserver.questEngine.definition.PersistenceMode;
import com.aionemu.gameserver.questEngine.definition.QuestDsl;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.definition.QuestMetadata;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

class QuestRuntimeCompositionCatalogSnapshotTest {

	@Test
	void metadataResolverStaysPinnedToTheCatalogUsedAtCompositionTime() throws Exception {
		int questId = 990030;
		var oldDefinition = definition(questId, "old metadata");
		var newDefinition = definition(questId, "new metadata");
		QuestRuntimeComposition oldComposition = QuestRuntimeComposition.production(
			new ImmutableQuestCatalog(List.of(oldDefinition)));
		QuestRuntimeComposition newComposition = QuestRuntimeComposition.production(
			new ImmutableQuestCatalog(List.of(newDefinition)));

		assertEquals("old metadata", metadataResolver(oldComposition).apply(questId).name());
		assertEquals("new metadata", metadataResolver(newComposition).apply(questId).name());
		assertEquals("old metadata", metadataResolver(oldComposition).apply(questId).name());
	}

	@SuppressWarnings("unchecked")
	private static IntFunction<QuestMetadata> metadataResolver(QuestRuntimeComposition composition) throws Exception {
		PlayerQuestEventPort eventPort = (PlayerQuestEventPort) composition.eventPort();
		Field eligibilityField = PlayerQuestEventPort.class.getDeclaredField("startEligibilityPort");
		eligibilityField.setAccessible(true);
		PlayerQuestStartEligibilityPort eligibility =
			(PlayerQuestStartEligibilityPort) eligibilityField.get(eventPort);
		Field metadataField = PlayerQuestStartEligibilityPort.class.getDeclaredField("metadataByQuest");
		metadataField.setAccessible(true);
		return (IntFunction<QuestMetadata>) metadataField.get(eligibility);
	}

	private static com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition definition(
			int questId, String name) {
		return QuestDsl.quest(questId)
			.metadata(QuestMetadata.minimal(name, questId, "QUEST"))
			.progress(bitField("var0", 0, 6, PersistenceMode.PERSISTENT))
			.node("start", project(QuestStatus.START, vars("var0", 0)))
			.on(new QuestEvent.LevelUp()).from("start").goTo("start")
			.compile();
	}
}
