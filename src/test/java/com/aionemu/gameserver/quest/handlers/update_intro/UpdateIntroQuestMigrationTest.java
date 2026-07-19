package com.aionemu.gameserver.quest.handlers.update_intro;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.file.Path;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.aionemu.gameserver.quest.handlers.update_intro._30810Ereshkigals_Resurrection.Action;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

class UpdateIntroQuestMigrationTest {

	private static final Path QUEST_DATA = Path.of("src/main/resources/aion/definitions/compact/quests/quest_data.xml");

	@Test
	void keepsTheFixedClientTemplate() throws Exception {
		Element quest = onlyQuest(30810);
		assertEquals("艾莱休奇卡之复活与深层进击", quest.getAttribute("name"));
		assertEquals("1803754", quest.getAttribute("nameId"));
		assertEquals("10", quest.getAttribute("minlevel_permitted"));
		assertEquals("1", quest.getAttribute("max_repeat_count"));
		assertEquals("true", quest.getAttribute("cannot_share"));
		assertEquals("false", quest.getAttribute("cannot_giveup"));
		assertEquals("PC_ALL", quest.getAttribute("race_permitted"));
		assertEquals("QUEST", quest.getAttribute("category"));
	}

	@Test
	void startsTheMovieAtLevelTenAndRecoversAnInterruptedMovie() {
		assertEquals(Action.NONE, _30810Ereshkigals_Resurrection.nextAction(9, null));
		assertEquals(Action.START_MOVIE, _30810Ereshkigals_Resurrection.nextAction(10, null));
		assertEquals(Action.START_MOVIE, _30810Ereshkigals_Resurrection.nextAction(10, state(QuestStatus.NONE)));
		assertEquals(Action.FINISH, _30810Ereshkigals_Resurrection.nextAction(10, state(QuestStatus.START)));
		assertEquals(Action.FINISH, _30810Ereshkigals_Resurrection.nextAction(10, state(QuestStatus.REWARD)));
		assertEquals(Action.NONE, _30810Ereshkigals_Resurrection.nextAction(10, state(QuestStatus.COMPLETE)));
	}

	@Test
	void keepsRetailMovieId() {
		assertEquals(36, _30810Ereshkigals_Resurrection.MOVIE_ID);
		assertEquals(30810, new _30810Ereshkigals_Resurrection().getQuestId());
	}

	private static Element onlyQuest(int questId) throws Exception {
		NodeList nodes = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(QUEST_DATA.toFile()).getElementsByTagName("quest");
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

	private static QuestState state(QuestStatus status) {
		return new QuestState(30810, status, 0, 0, null, null, null);
	}
}
