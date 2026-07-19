package com.aionemu.gameserver.quest.handlers.padmarashka_cave;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

class PadmarashkaCutsceneQuestMigrationTest {

	private static final Path QUEST_DATA = Path.of("src/main/resources/aion/definitions/compact/quests/quest_data.xml");
	private static final Path HANDLERS = Path.of("src/main/java/com/aionemu/gameserver/quest/handlers");
	private static final Path QUEST_SCRIPTS = Path.of("src/main/resources/aion/definitions/compact/quests/scripts");

	@Test
	void rejectsWrongWorld() {
		assertFalse(_11295PadmarashkaCutscene.canStart(320150001, Race.ELYOS, 50, 2));
	}

	@Test
	void rejectsWrongRace() {
		assertFalse(_11295PadmarashkaCutscene.canStart(320150000, Race.ASMODIANS, 50, 2));
	}

	@Test
	void rejectsLowLevel() {
		assertFalse(_11295PadmarashkaCutscene.canStart(320150000, Race.ELYOS, 49, 2));
	}

	@Test
	void rejectsMissingRequiredItems() {
		assertFalse(_11295PadmarashkaCutscene.canStart(320150000, Race.ELYOS, 50, 1));
	}

	@Test
	void acceptsTheFixedClientEntryConditions() {
		assertTrue(_11295PadmarashkaCutscene.canStart(320150000, Race.ELYOS, 50, 2));
	}

	@Test
	void rejectsOtherMovies() {
		assertFalse(_11295PadmarashkaCutscene.canFinish(320150000, Race.ELYOS, 50, 487, QuestStatus.START));
	}

	@Test
	void rejectsNonStartedQuestStates() {
		assertFalse(_11295PadmarashkaCutscene.canFinish(320150000, Race.ELYOS, 50, 488, null));
		assertFalse(_11295PadmarashkaCutscene.canFinish(320150000, Race.ELYOS, 50, 488, QuestStatus.NONE));
		assertFalse(_11295PadmarashkaCutscene.canFinish(320150000, Race.ELYOS, 50, 488, QuestStatus.REWARD));
		assertFalse(_11295PadmarashkaCutscene.canFinish(320150000, Race.ELYOS, 50, 488, QuestStatus.COMPLETE));
	}

	@Test
	void acceptsMovie488ForAnActiveQuest() {
		assertTrue(_11295PadmarashkaCutscene.canFinish(320150000, Race.ELYOS, 50, 488, QuestStatus.START));
	}

	@Test
	void keepsTheFixedClientTemplate() throws Exception {
		Element quest = onlyQuest(11295);
		assertEquals("动画播放用隐藏任务", quest.getAttribute("name"));
		assertEquals("1129656", quest.getAttribute("nameId"));
		assertEquals("50", quest.getAttribute("minlevel_permitted"));
		assertEquals("255", quest.getAttribute("max_repeat_count"));
		assertEquals("true", quest.getAttribute("cannot_share"));
		assertEquals("false", quest.getAttribute("cannot_giveup"));
		assertEquals("ELYOS", quest.getAttribute("race_permitted"));
		assertEquals("QUEST", quest.getAttribute("category"));
	}

	@Test
	void keepsOneHandlerAndNoLegacyXmlBehavior() throws Exception {
		assertEquals(1, handlerCount(11295));
		assertFalse(hasXmlBehavior(11295));
		assertEquals(11295, new _11295PadmarashkaCutscene().getQuestId());
		assertEquals(182215009, _11295PadmarashkaCutscene.REQUIRED_ITEM_ID);
		assertEquals(2, _11295PadmarashkaCutscene.REQUIRED_ITEM_COUNT);
		assertEquals(488, _11295PadmarashkaCutscene.MOVIE_ID);
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
}
