package com.aionemu.gameserver.questEngine;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.dataholders.NpcData;
import com.aionemu.gameserver.dataholders.QuestsData;
import com.aionemu.gameserver.dataholders.StaticData;
import com.aionemu.gameserver.dataholders.WorldMapsData;
import com.aionemu.gameserver.dataholders.XMLQuests;
import com.aionemu.gameserver.lifecycle.GameEngineServices;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.handlers.models.XMLQuest;

class QuestEngineScriptRoutingTest {

	private final QuestEngine questEngine = QuestEngine.getInstance();

	@AfterEach
	void clearRegistry() {
		questEngine.clear();
	}

	@Test
	void scriptQuestTakesPriorityAndJavaHandlerRemainsFallback() {
		QuestHandler javaHandler = handler(1000);
		QuestHandler scriptHandler = handler(1000);
		questEngine.addQuestHandler(javaHandler);
		GameEngineServices.scriptEngine().getRegistry().registerScriptQuest(() -> scriptHandler);

		assertSame(scriptHandler, questEngine.getQuestHandlerByQuestId(1000));

		GameEngineServices.scriptEngine().getRegistry().clearScriptQuests();
		assertSame(javaHandler, questEngine.getQuestHandlerByQuestId(1000));
	}

	@Test
	void xmlQuestHandlerIsTransferredToScriptRegistry() {
		QuestHandler xmlHandler = handler(1000);
		questEngine.registerScriptQuest(xmlQuest(xmlHandler));

		assertSame(xmlHandler, GameEngineServices.scriptEngine().getRegistry().getScriptQuest(1000).getHandler());
		assertSame(xmlHandler, questEngine.getQuestHandlerByQuestId(1000));
	}

	@Test
	void xmlQuestTransferRetainsExistingJavaFallback() {
		QuestHandler javaHandler = handler(1000);
		QuestHandler xmlHandler = handler(1000);
		questEngine.addQuestHandler(javaHandler);
		Logger logger = (Logger) LoggerFactory.getLogger(QuestEngine.class);
		ListAppender<ILoggingEvent> appender = new ListAppender<>();
		appender.start();
		logger.addAppender(appender);
		try {
			questEngine.registerScriptQuest(xmlQuest(xmlHandler));
			assertTrue(appender.list.stream().noneMatch(event -> event.getLevel().isGreaterOrEqual(Level.WARN)));
		} finally {
			logger.detachAppender(appender);
			appender.stop();
		}

		assertSame(xmlHandler, questEngine.getQuestHandlerByQuestId(1000));

		GameEngineServices.scriptEngine().getRegistry().clearScriptQuests();
		assertSame(javaHandler, questEngine.getQuestHandlerByQuestId(1000));
	}

	@Test
	void retailXmlQuestWinsRegardlessOfImportOrder() {
		XMLQuest retail = xmlQuest(handler(1000), true);
		XMLQuest legacy = xmlQuest(handler(1000), false);

		assertEquals(List.of(retail), List.copyOf(QuestEngine.selectScriptQuests(List.of(retail, legacy))));
		assertEquals(List.of(retail), List.copyOf(QuestEngine.selectScriptQuests(List.of(legacy, retail))));
	}

	@Test
	void allXmlQuestDefinitionsTransferToScriptRegistry() throws Exception {
		Unmarshaller unmarshaller = JAXBContext.newInstance(StaticData.class).createUnmarshaller();
		QuestsData previousQuestData = DataManager.QUEST_DATA;
		WorldMapsData previousWorldMapsData = DataManager.WORLD_MAPS_DATA;
		NpcData previousNpcData = DataManager.NPC_DATA;
		try {
			DataManager.QUEST_DATA = (QuestsData) unmarshaller.unmarshal(Path.of(
					"src/main/resources/aion/definitions/compact/quests/quest_data.xml").toFile());
			DataManager.WORLD_MAPS_DATA = (WorldMapsData) unmarshaller.unmarshal(Path.of(
					"src/main/resources/aion/data/static_data/world_maps.xml").toFile());
			DataManager.NPC_DATA = (NpcData) unmarshaller.unmarshal(Path.of(
					"src/main/resources/aion/data/static_data/npcs/npc_template.xml").toFile());
			Set<Integer> questIds = new HashSet<>();
			List<XMLQuest> definitions = new ArrayList<>();
			try (var paths = Files.walk(Path.of("src/main/resources/aion/definitions/compact/quests/scripts"))) {
				for (Path path : paths.filter(p -> p.toString().endsWith(".xml")).sorted().toList()) {
					XMLQuests quests = (XMLQuests) unmarshaller.unmarshal(path.toFile());
					definitions.addAll(quests.getQuest());
				}
			}
			for (XMLQuest quest : QuestEngine.selectScriptQuests(definitions)) {
				questEngine.registerScriptQuest(quest);
				questIds.add(quest.getId());
			}

			assertEquals(questIds.size(), GameEngineServices.scriptEngine().getRegistry().scriptQuestCount());
		} finally {
			DataManager.QUEST_DATA = previousQuestData;
			DataManager.WORLD_MAPS_DATA = previousWorldMapsData;
			DataManager.NPC_DATA = previousNpcData;
		}
	}

	private QuestHandler handler(int questId) {
		return new QuestHandler(questId) {
			@Override
			public void register() {
			}
		};
	}

	private XMLQuest xmlQuest(QuestHandler handler) {
		return xmlQuest(handler, false);
	}

	private XMLQuest xmlQuest(QuestHandler handler, boolean retailSource) {
		return new XMLQuest() {
			{
				id = handler.getQuestId();
				retail = retailSource;
			}

			@Override
			public void register(QuestEngine questEngine) {
				questEngine.addQuestHandler(handler);
			}
		};
	}
}
