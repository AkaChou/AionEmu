package com.aionemu.gameserver.instance.handlers.scripts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.instance.playerreward.IronWallWarfrontPlayerReward;

class IronWallWarfrontMigrationTest {
	private static final Path CONDITION_SPAWNS = Path.of(
		"src/main/resources/aion/definitions/compact/ai/condition-spawns.xml");
	private static final Path STATIC_SPAWNS = Path.of(
		"src/main/resources/aion/data/static_data/spawns/Instances/301220000_Iron_Wall_Warfront.xml");

	@Test
	void generatedSpawnsCoverTheRetailWorldWithoutStaticDuplicates() throws Exception {
		Element world = byAttribute(document(CONDITION_SPAWNS).getDocumentElement(), "world", "id", "301220000");
		assertNotNull(world);
		assertEquals(28, world.getElementsByTagName("variable").getLength());
		assertEquals(486, world.getElementsByTagName("condition").getLength());
		assertEquals(542, world.getElementsByTagName("npc").getLength());

		Element boss = byAttribute(world, "npc", "id", "233544");
		assertNotNull(boss);
		assertEquals("743.685364", boss.getAttribute("x"));
		assertEquals("293.568237", boss.getAttribute("y"));
		assertEquals("237.799255", boss.getAttribute("z"));
		assertEquals("95", boss.getAttribute("heading"));
		assertEquals("5", boss.getAttribute("initial_delay"));
		assertEquals("true", boss.getAttribute("despawn_at_attack_state"));
		assertEquals("idf5_td_war/world_N.xml#selected-unconditional",
			boss.getParentNode().getParentNode().getParentNode().getAttributes().getNamedItem("source").getNodeValue());

		Set<String> conditionalIds = attributes(world.getElementsByTagName("npc"), "id");
		Element staticMap = byAttribute(document(STATIC_SPAWNS).getDocumentElement(), "spawn_map", "map_id", "301220000");
		Set<String> duplicateIds = attributes(staticMap.getElementsByTagName("spawn"), "npc_id");
		duplicateIds.retainAll(conditionalIds);
		assertTrue(duplicateIds.isEmpty(), duplicateIds.toString());
		assertNotNull(byAttribute(staticMap, "spawn", "npc_id", "831919"));
		assertNotNull(byAttribute(staticMap, "spawn", "npc_id", "831975"));
	}

	@Test
	void retailScoresCoverFixedFactionsOfficerLossAndBoss() throws Exception {
		Document scores = document(Path.of("src/main/resources/aion/definitions/compact/ai/npc-scores.xml"));
		Map<Integer, Element> byNpc = new HashMap<>();
		NodeList scoreNodes = scores.getDocumentElement().getElementsByTagName("npc_score");
		for (int i = 0; i < scoreNodes.getLength(); i++) {
			Element score = (Element) scoreNodes.item(i);
			byNpc.put(Integer.parseInt(score.getAttribute("npc_id")), score);
		}
		for (int npcId = 284881; npcId <= 284890; npcId++) {
			assertScore(byNpc.get(npcId), 1, npcId == 284881 || npcId == 284886 || npcId == 284887 ? 150 : 100);
		}
		for (int npcId = 284891; npcId <= 284900; npcId++) {
			assertScore(byNpc.get(npcId), 2, npcId == 284891 || npcId == 284896 || npcId == 284897 ? 150 : 100);
		}
		for (int npcId = 855019; npcId <= 855028; npcId++) {
			assertScore(byNpc.get(npcId), 2, 400);
		}
		for (int npcId = 855029; npcId <= 855038; npcId++) {
			assertScore(byNpc.get(npcId), 1, 400);
		}
		assertScore(byNpc.get(233498), 2, -400);
		assertScore(byNpc.get(233518), 1, -400);
		assertScore(byNpc.get(233544), 0, 200000);
		assertTrue(IronWallWarfrontInstance.requiresScorePlayer(0));
		assertFalse(IronWallWarfrontInstance.requiresScorePlayer(1));
		assertFalse(IronWallWarfrontInstance.requiresScorePlayer(2));
		assertFalse(IronWallWarfrontInstance.requiresScorePlayer(3));
	}

	@Test
	void fixedFactionScoreNpcsUseTheRetailSelfKillChain() throws Exception {
		Element npcAi = byAttribute(document(Path.of(
			"src/main/resources/aion/definitions/compact/ai/npc-ai.xml")).getDocumentElement(), "npc", "id", "284881");
		assertEquals("IDF5_TD_War_TEMP_02", npcAi.getAttribute("ai"));

		Document patterns = document(Path.of(
			"src/main/resources/aion/definitions/compact/ai/npcaipatterns_idf5_td_war_yjh.xml"));
		Element pattern = byChildText(patterns.getDocumentElement(), "npc_ai_pattern", "name", "IDF5_TD_War_TEMP_02");
		assertNotNull(pattern);
		assertEquals("SKILLI_INDEX_0", pattern.getElementsByTagName("skill").item(0).getTextContent());

		Document npcSkills = document(Path.of(
			"src/main/resources/aion/definitions/compact/skills/npc-skills.xml"));
		Element group = byAttribute(npcSkills.getDocumentElement(), "group", "id", "NS_F026E0128506BA00");
		assertEquals("20549", ((Element) group.getElementsByTagName("skill").item(0)).getAttribute("id"));
		Element assignment = byAttribute(npcSkills.getDocumentElement(), "assign", "group", "NS_F026E0128506BA00");
		Set<String> assignedNpcs = Set.of(assignment.getAttribute("npc_ids").split(" "));
		assertTrue(assignedNpcs.contains("284881"));
		assertTrue(assignedNpcs.contains("855038"));

		Element skill = byAttribute(document(Path.of(
			"src/main/resources/aion/definitions/compact/skills/skill_templates_part_025.xml")).getDocumentElement(),
			"skill_template", "skill_id", "20549");
		assertEquals("19713", skill.getAttribute("penalty_skill_id"));
	}

	@Test
	void handlerUsesPersistentRetailLifecycleAndSettlement() throws Exception {
		assertNotNull(IronWallWarfrontPlayerReward.class
			.getConstructor(int.class, byte.class, Race.class, long.class));
		String handler = Files.readString(Path.of(
			"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/IronWallWarfrontInstance.java"));
		for (String required : new String[] {
			"scheduleDeadline(\"adjustment\"", "scheduleDeadline(\"preparation\"",
			"scheduleDeadline(\"battle\"", "scheduleDeadline(\"exit\"",
			"requiredInt(\"adjust_time\")", "requiredInt(\"wait_time\")", "requiredInt(\"limit_time\")",
			"findStringId(name)", "InstanceSettlementService.queueBattleground", "DataManager.RETAIL_AI_DATA.getNpcScore",
			"npc.getSpawn().getStableKey()", "runtimeState().getBoolean(eventKey, false)",
			"setDoorState(doorId, true)"
		}) {
			assertTrue(handler.contains(required), required);
		}
		for (String legacy : new String[] {
			"Future<", "GameThreadPoolServices", "HANDLERS", "onDropRegistered", "onEnterZone", "regDropItem",
			"ironWallBase", "loosingGroupMultiplier", "stopInstanceTask", " sp(", "spawn(233544"
		}) {
			assertFalse(handler.contains(legacy), legacy);
		}
		String reward = Files.readString(Path.of(
			"src/main/java/com/aionemu/gameserver/model/instance/instancereward/IronWallWarfrontReward.java"));
		assertFalse(reward.contains("capPoints"));
		assertFalse(reward.contains("instanceTime"));

		Element row = byAttribute(document(Path.of(
			"src/main/resources/aion/definitions/compact/instance/rewards.xml")).getDocumentElement(),
			"row", "world_id", "301220000");
		assertEquals("120", row.getAttribute("adjust_time"));
		assertEquals("60", row.getAttribute("wait_time"));
		assertEquals("2400", row.getAttribute("limit_time"));
		assertEquals("STR_MSG_BATTLEGROUND_ADJUST_TIME", row.getAttribute("adjust_time_start_msg"));
		assertEquals("STR_MSG_BATTLEGROUND_WAIT_TIME", row.getAttribute("wait_time_start_msg"));
	}

	@Test
	void stableScoreKeysDeduplicateOneGenerationWithoutMergingTheNext() {
		String first = IronWallWarfrontInstance.scoreEventKey("condition:42:generation.1", 100);
		assertEquals(first, IronWallWarfrontInstance.scoreEventKey("condition:42:generation.1", 101));
		assertNotEquals(first, IronWallWarfrontInstance.scoreEventKey("condition:42:generation.2", 100));
		assertEquals("ironWall.score.event.object.100", IronWallWarfrontInstance.scoreEventKey(null, 100));
	}

	private static void assertScore(Element score, int applyType, int value) {
		assertNotNull(score);
		assertEquals(Integer.toString(applyType), score.getAttribute("score_apply_type"));
		assertEquals(Integer.toString(value), score.getAttribute("value"));
	}

	private static Document document(Path path) throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
		factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
		factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
		return factory.newDocumentBuilder().parse(path.toFile());
	}

	private static Element byAttribute(Element root, String tag, String attribute, String value) {
		NodeList nodes = root.getElementsByTagName(tag);
		for (int i = 0; i < nodes.getLength(); i++) {
			Element element = (Element) nodes.item(i);
			if (value.equals(element.getAttribute(attribute))) {
				return element;
			}
		}
		return null;
	}

	private static Element byChildText(Element root, String tag, String child, String value) {
		NodeList nodes = root.getElementsByTagName(tag);
		for (int i = 0; i < nodes.getLength(); i++) {
			Element element = (Element) nodes.item(i);
			NodeList children = element.getElementsByTagName(child);
			if (children.getLength() > 0 && value.equals(children.item(0).getTextContent())) {
				return element;
			}
		}
		return null;
	}

	private static Set<String> attributes(NodeList nodes, String name) {
		Set<String> result = new HashSet<>();
		for (int i = 0; i < nodes.getLength(); i++) {
			result.add(((Element) nodes.item(i)).getAttribute(name));
		}
		return result;
	}
}
