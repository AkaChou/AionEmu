package com.aionemu.gameserver.quest.handlers.reshanta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

class ReshantaQuestMigrationTest {
	private static final Path RETAIL_QUESTS = Path.of(
			"src/main/resources/aion/definitions/compact/quests/scripts/zz_retail_simple_quests.xml");

	@Test
	void emergencyPvpRequiresFivePlayerKills() {
		assertEquals(5, AbstractReshantaEmergencyPvp.REQUIRED_KILLS);
	}

	@Test
	void surveysKeepTheFixedClientRouteOrder() throws Exception {
		assertRoute(1868, 278501, 278503, 833587, 833594, 833590, 833593, 833589, 833592, 833588, 833591);
		assertRoute(2868, 278001, 278003, 833590, 833594, 833587, 833591, 833588, 833592, 833589, 833593);
	}

	private static void assertRoute(int questId, int startNpc, int firstNpc, int... sensoryNpcs) throws Exception {
		NodeList quests = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(RETAIL_QUESTS.toFile())
			.getElementsByTagName("data_driven_quest");
		Element quest = null;
		for (int index = 0; index < quests.getLength(); index++) {
			Element candidate = (Element) quests.item(index);
			if (Integer.toString(questId).equals(candidate.getAttribute("id"))) {
				quest = candidate;
				break;
			}
		}
		assertNotNull(quest);
		assertEquals("TALK", quest.getAttribute("start_type"));
		assertEquals(Integer.toString(startNpc), quest.getAttribute("start_ids"));
		assertEquals(Integer.toString(startNpc), quest.getAttribute("end_npc_ids"));
		NodeList steps = quest.getElementsByTagName("step");
		List<Integer> actual = new ArrayList<>();
		for (int index = 0; index < steps.getLength(); index++) {
			actual.add(Integer.parseInt(((Element) steps.item(index)).getAttribute("ids")));
		}
		List<Integer> expected = new ArrayList<>(List.of(firstNpc));
		for (int sensoryNpc : sensoryNpcs) {
			expected.add(sensoryNpc);
		}
		assertEquals(expected, actual);
	}
}
