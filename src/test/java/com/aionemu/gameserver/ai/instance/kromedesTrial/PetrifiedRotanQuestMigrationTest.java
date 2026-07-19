package com.aionemu.gameserver.ai.instance.kromedesTrial;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.aionemu.gameserver.quest.handlers.kromedes_trial._18604Petrified_Rotan;
import com.aionemu.gameserver.quest.handlers.kromedes_trial._28604Petrified_Rotan;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

class PetrifiedRotanQuestMigrationTest {

	private static final Path QUEST_DATA = Path.of("src/main/resources/aion/data/static_data/quest_data/quest_data.xml");
	private static final Path QUEST_ZONES = Path.of("src/main/resources/aion/data/static_data/zones/zones_quest.xml");

	@Test
	void keepsFixedClientQuestIdsAndTemplates() throws Exception {
		assertEquals(18604, new _18604Petrified_Rotan().getQuestId());
		assertEquals(28604, new _28604Petrified_Rotan().getQuestId());
		assertTemplate(18604, "1117004", "ELYOS");
		assertTemplate(28604, "1117054", "ASMODIANS");
	}

	@Test
	void corpseRecoveryRequiresAQuestRecordAndMissingItem() {
		QuestState started = state(18604, QuestStatus.START);
		QuestState completed = state(28604, QuestStatus.COMPLETE);

		assertFalse(Grave_Robber_CorpseAI2.canRecover(null, null, 0));
		assertFalse(Grave_Robber_CorpseAI2.canRecover(started, null, 1));
		assertTrue(Grave_Robber_CorpseAI2.canRecover(started, null, 0));
		assertTrue(Grave_Robber_CorpseAI2.canRecover(null, completed, 0));
	}

	@Test
	void keepsTheRetailSensoryPolygonExact() throws Exception {
		Element zone = onlyElement(QUEST_ZONES, "zone", "name", "IDCROMEDE_SENSORYAREA_Q18604_300230000");
		assertEquals("300230000", zone.getAttribute("mapid"));
		assertEquals("POLYGON", zone.getAttribute("area_type"));
		Element points = (Element) zone.getElementsByTagName("points").item(0);
		assertEquals("185.814651", points.getAttribute("top"));
		assertEquals("165.814651", points.getAttribute("bottom"));
		List<String> expected = List.of(
			"438.712433,322.185089", "425.309204,341.497498", "431.659515,360.352539",
			"444.443787,375.940948", "465.173889,386.596375", "478.553131,391.850922",
			"497.470459,383.569153", "513.166870,363.375946", "501.188080,326.227295",
			"476.068573,310.793915", "456.712494,322.712097");
		NodeList nodes = points.getElementsByTagName("point");
		assertEquals(expected.size(), nodes.getLength());
		for (int index = 0; index < nodes.getLength(); index++) {
			Element point = (Element) nodes.item(index);
			assertEquals(expected.get(index), point.getAttribute("x") + "," + point.getAttribute("y"));
		}
	}

	private static void assertTemplate(int questId, String nameId, String race) throws Exception {
		Element quest = onlyElement(QUEST_DATA, "quest", "id", Integer.toString(questId));
		assertEquals("[副本]和罗坦的会面", quest.getAttribute("name"));
		assertEquals(nameId, quest.getAttribute("nameId"));
		assertEquals("37", quest.getAttribute("minlevel_permitted"));
		assertEquals("1", quest.getAttribute("max_repeat_count"));
		assertEquals("true", quest.getAttribute("cannot_share"));
		assertEquals("false", quest.getAttribute("cannot_giveup"));
		assertEquals(race, quest.getAttribute("race_permitted"));
		assertEquals("NON_COUNT", quest.getAttribute("category"));
		Element rewards = (Element) quest.getElementsByTagName("rewards").item(0);
		assertEquals("150", rewards.getAttribute("exp"));
		Element item = (Element) rewards.getElementsByTagName("reward_item").item(0);
		assertEquals("164000141", item.getAttribute("item_id"));
		assertEquals("1", item.getAttribute("count"));
	}

	private static Element onlyElement(Path path, String tagName, String attribute, String value) throws Exception {
		NodeList nodes = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(path.toFile()).getElementsByTagName(tagName);
		Element match = null;
		for (int index = 0; index < nodes.getLength(); index++) {
			Element element = (Element) nodes.item(index);
			if (value.equals(element.getAttribute(attribute))) {
				assertEquals(null, match, "duplicate " + tagName + " " + value);
				match = element;
			}
		}
		assertTrue(match != null, "missing " + tagName + " " + value);
		return match;
	}

	private static QuestState state(int questId, QuestStatus status) {
		return new QuestState(questId, status, 0, 0, null, null, null);
	}
}
