package com.aionemu.gameserver.quest.handlers.raksang_ruins;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.file.Files;
import java.nio.file.Path;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.aionemu.gameserver.quest.handlers.raksang_ruins.AbstractRaksangIntro.Action;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

class RaksangIntroQuestMigrationTest {

	private static final Path QUEST_DATA = Path.of("src/main/resources/aion/data/static_data/quest_data/quest_data.xml");
	private static final Path HANDLERS = Path.of("src/main/java/com/aionemu/gameserver/quest/handlers");
	private static final Path QUEST_SCRIPTS = Path.of("src/main/resources/aion/data/static_data/quest_script_data");

	@Test
	void keepsFixedClientTemplatesAndSharing() throws Exception {
		assertTemplate(18744, "阿比索提供的勒门图的情报", "1800942", "ELYOS");
		assertTemplate(28744, "普罗库拉提供的勒门图的情报", "1800950", "ASMODIANS");
	}

	@Test
	void startsOrReplaysTheMovieUntilCompletion() {
		assertEquals(Action.START_MOVIE, AbstractRaksangIntro.nextAction(null));
		assertEquals(Action.START_MOVIE, AbstractRaksangIntro.nextAction(state(QuestStatus.NONE)));
		assertEquals(Action.REPLAY_MOVIE, AbstractRaksangIntro.nextAction(state(QuestStatus.START)));
		assertEquals(Action.REPLAY_MOVIE, AbstractRaksangIntro.nextAction(state(QuestStatus.REWARD)));
		assertEquals(Action.NONE, AbstractRaksangIntro.nextAction(state(QuestStatus.COMPLETE)));
		assertEquals(300610000, AbstractRaksangIntro.WORLD_ID);
		assertEquals(60, AbstractRaksangIntro.MIN_LEVEL);
		assertEquals(18744, new _18744Avisos_Intelligence().getQuestId());
		assertEquals(28744, new _28744Procuras_Intelligence().getQuestId());
	}

	@Test
	void removesLegacyAndIsolatedQuestImplementations() throws Exception {
		assertEquals(1, handlerCount(18744));
		assertEquals(1, handlerCount(28744));
		assertEquals(0, handlerCount(18412));
		assertEquals(0, handlerCount(28412));
		assertFalse(hasQuestTemplate(18412));
		assertFalse(hasQuestTemplate(28412));
		assertFalse(hasXmlBehavior(18744));
		assertFalse(hasXmlBehavior(28744));
		assertFalse(hasXmlBehavior(18412));
		assertFalse(hasXmlBehavior(28412));
	}

	private static void assertTemplate(int questId, String name, String nameId, String race) throws Exception {
		Element quest = onlyQuest(questId);
		assertEquals(name, quest.getAttribute("name"));
		assertEquals(nameId, quest.getAttribute("nameId"));
		assertEquals("60", quest.getAttribute("minlevel_permitted"));
		assertEquals("1", quest.getAttribute("max_repeat_count"));
		assertEquals("false", quest.getAttribute("cannot_share"));
		assertEquals("false", quest.getAttribute("cannot_giveup"));
		assertEquals(race, quest.getAttribute("race_permitted"));
		assertEquals("QUEST", quest.getAttribute("category"));
	}

	private static Element onlyQuest(int questId) throws Exception {
		NodeList nodes = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(QUEST_DATA.toFile())
			.getElementsByTagName("quest");
		Element match = null;
		for (int index = 0; index < nodes.getLength(); index++) {
			Element quest = (Element) nodes.item(index);
			if (Integer.toString(questId).equals(quest.getAttribute("id"))) {
				assertEquals(null, match, "duplicate Quest " + questId);
				match = quest;
			}
		}
		assertNotNull(match, "missing Quest " + questId);
		return match;
	}

	private static boolean hasQuestTemplate(int questId) throws Exception {
		NodeList nodes = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(QUEST_DATA.toFile())
			.getElementsByTagName("quest");
		for (int index = 0; index < nodes.getLength(); index++) {
			if (Integer.toString(questId).equals(((Element) nodes.item(index)).getAttribute("id"))) {
				return true;
			}
		}
		return false;
	}

	private static long handlerCount(int questId) throws Exception {
		try (var paths = Files.walk(HANDLERS)) {
			return paths.filter(path -> path.getFileName().toString().startsWith("_" + questId)).count();
		}
	}

	private static boolean hasXmlBehavior(int questId) throws Exception {
		try (var paths = Files.walk(QUEST_SCRIPTS)) {
			for (Path path : paths.filter(value -> value.toString().endsWith(".xml")).toList()) {
				NodeList nodes = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(path.toFile())
					.getElementsByTagName("*");
				for (int index = 0; index < nodes.getLength(); index++) {
					if (Integer.toString(questId).equals(((Element) nodes.item(index)).getAttribute("id"))) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private static QuestState state(QuestStatus status) {
		return new QuestState(18744, status, 0, 0, null, null, null);
	}
}
