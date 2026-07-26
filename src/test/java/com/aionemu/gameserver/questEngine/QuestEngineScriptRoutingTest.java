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
	void patchXmlQuestWinsRegardlessOfImportOrder() {
		XMLQuest patch = xmlQuest(handler(1000), false, true);
		XMLQuest retail = xmlQuest(handler(1000), true);
		XMLQuest legacy = xmlQuest(handler(1000), false);

		assertEquals(List.of(patch), List.copyOf(QuestEngine.selectScriptQuests(List.of(patch, retail, legacy))));
		assertEquals(List.of(patch), List.copyOf(QuestEngine.selectScriptQuests(List.of(legacy, retail, patch))));
		assertEquals(List.of(patch), List.copyOf(QuestEngine.selectScriptQuests(List.of(retail, patch, legacy))));
	}

	@Test
	void disabledRetailQuestFallsBackToLegacyWhilePatchIsUnaffected() {
		XMLQuest retail = xmlQuest(handler(1000), true);
		XMLQuest legacy = xmlQuest(handler(1000), false);
		XMLQuest patch = xmlQuest(handler(2000), true, true);

		assertEquals(List.of(legacy), List.copyOf(QuestEngine.selectScriptQuests(List.of(retail, legacy), Set.of(1000))));
		assertEquals(List.of(), List.copyOf(QuestEngine.selectScriptQuests(List.of(retail), Set.of(1000))));
		assertEquals(List.of(patch), List.copyOf(QuestEngine.selectScriptQuests(List.of(patch), Set.of(2000))));
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
			for (String dir : List.of("src/main/resources/aion/definitions/compact/quests/scripts",
					"src/main/resources/aion/definitions/compact/quests/patches")) {
				try (var paths = Files.walk(Path.of(dir))) {
					for (Path path : paths.filter(p -> p.toString().endsWith(".xml")).sorted().toList()) {
						XMLQuests quests = (XMLQuests) unmarshaller.unmarshal(path.toFile());
						if (quests.getQuest() != null) {
							definitions.addAll(quests.getQuest());
						}
					}
				}
			}
			for (XMLQuest quest : QuestEngine.selectScriptQuests(definitions)) {
				questEngine.registerScriptQuest(quest);
				questIds.add(quest.getId());
			}

			assertEquals(questIds.size(), GameEngineServices.scriptEngine().getRegistry().scriptQuestCount());

			// 生成产物与报告所有权对账：漂移（手改产物、报告未随生成更新）在此变红。
			// Reconciles generated ownership against the report: drift (hand-edited output, stale report) fails here.
			String report = Files.readString(Path.of(
					"src/main/resources/aion/definitions/compact/quests/scripts/zz_retail_simple_quests.report.json"));
			List<XMLQuest> selectedWithoutPatches = QuestEngine.selectScriptQuests(
					definitions.stream().filter(quest -> !quest.isPatch()).toList(), Set.of());
			assertEquals(reportNumber(report, "generated_xml"),
					selectedWithoutPatches.stream().filter(XMLQuest::isRetail).count());
			assertEquals(reportNumber(report, "legacy_xml"),
					selectedWithoutPatches.stream().filter(quest -> !quest.isRetail()).count());
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

	private static long reportNumber(String report, String key) {
		java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\"" + key + "\": (\\d+)").matcher(report);
		assertTrue(matcher.find(), "report key not found: " + key);
		return Long.parseLong(matcher.group(1));
	}

	private XMLQuest xmlQuest(QuestHandler handler) {
		return xmlQuest(handler, false);
	}

	private XMLQuest xmlQuest(QuestHandler handler, boolean retailSource) {
		return xmlQuest(handler, retailSource, false);
	}

	private XMLQuest xmlQuest(QuestHandler handler, boolean retailSource, boolean patchSource) {
		return new XMLQuest() {
			{
				id = handler.getQuestId();
				retail = retailSource;
				patch = patchSource;
			}

			@Override
			public void register(QuestEngine questEngine) {
				questEngine.addQuestHandler(handler);
			}
		};
	}
}
