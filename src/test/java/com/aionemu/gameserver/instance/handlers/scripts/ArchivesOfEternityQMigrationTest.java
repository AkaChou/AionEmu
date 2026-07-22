package com.aionemu.gameserver.instance.handlers.scripts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

class ArchivesOfEternityQMigrationTest {

	private static final String WORLD = "//world[@id='301570000']";

	@Test
	void retailConditionsOwnTheCompleteSceneFlow() throws Exception {
		Document conditions = parse("src/main/resources/aion/definitions/compact/ai/condition-spawns.xml");
		assertEquals(55, count(conditions, WORLD + "/condition"));
		assertEquals(59, count(conditions, WORLD + "//slot"));
		for (String variable : new String[] { "scene", "user_gender", "user_race" }) {
			assertTrue(exists(conditions, WORLD + "/variable[@name='" + variable + "']"), variable);
		}
		for (String npcId : new String[] { "857833", "857948", "857915", "857916", "857788", "857795",
				"857882", "806179", "806180" }) {
			assertTrue(exists(conditions, WORLD + "//npc[@id='" + npcId + "']"), npcId);
		}

		String staticSpawns = Files.readString(Path.of(
			"src/main/resources/aion/data/static_data/spawns/Instances/301570000_Archives_Of_Eternity.xml"));
		for (String npcId : new String[] { "703130", "806136", "806137", "857781", "857784", "857824",
				"857825", "857826", "857829", "857831", "857833", "857834", "857835", "857836",
				"857901", "857902", "857904", "857905", "857914", "857948" }) {
			assertFalse(staticSpawns.contains("npc_id=\"" + npcId + "\""), npcId);
		}
		for (String npcId : new String[] { "857782", "857783", "857827", "857828", "857830", "857832" }) {
			assertTrue(staticSpawns.contains("npc_id=\"" + npcId + "\""), npcId);
		}
	}

	@Test
	void retailDoorIdsAndInitialStatesMatchTheMap() throws Exception {
		Document doors = parse("src/main/resources/aion/data/static_data/staticdoors/staticdoor_templates.xml");
		String world = "//world[@world='301570000']";
		for (String mapping : new String[] { "1:384", "2:252", "3:67", "4:449", "5:64", "6:311",
				"7:421", "8:90", "9:77", "10:65" }) {
			String[] ids = mapping.split(":");
			assertTrue(exists(doors, world + "/staticdoor[@retailid='" + ids[0] + "' and @doorid='" + ids[1] + "']"), mapping);
		}
		for (String doorId : new String[] { "21", "22", "24", "26", "27", "28", "33", "52", "53", "54",
				"55", "56", "65" }) {
			assertTrue(exists(doors, world + "/staticdoor[@doorid='" + doorId + "' and @state='0x1']"), doorId);
		}
		String worldMapInstance = Files.readString(Path.of(
			"src/main/java/com/aionemu/gameserver/world/WorldMapInstance.java"));
		assertTrue(worldMapInstance.contains("getObjectTemplate().getRetailId()"));
	}

	@Test
	void handlersOnlySupplyCoreVariablesAndUnrepresentedAsmodianSpawn() throws Exception {
		String handler = Files.readString(Path.of(
			"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/ArchivesOfEternityQInstance.java"));
		assertTrue(handler.contains("\"SCENE\", 13, 0"));
		for (String legacy : new String[] { "StaticDoor", "GameThreadPoolServices", "spawn(", "knowledgeOf",
				"857782", "857783", "857901", "857902", "857948", "857903", "857916" }) {
			assertFalse(handler.contains(legacy), legacy);
		}

		String elyos = Files.readString(Path.of(
			"src/main/java/com/aionemu/gameserver/quest/handlers/archdaeva/_10521Memories_Of_Eternity.java"));
		String asmodian = Files.readString(Path.of(
			"src/main/java/com/aionemu/gameserver/quest/handlers/archdaeva/_20521Recovered_Destiny.java"));
		for (String source : new String[] { elyos, asmodian }) {
			for (String required : new String[] { "\"SCENE\", 2, 0", "\"SCENE\", 9, 0", "\"SCENE\", 11, 0",
					"\"SCENE\", 12, 0", "\"USER_GENDER\"", "\"USER_RACE\"", "playQuestMovie(env, 935)" }) {
				assertTrue(source.contains(required), required);
			}
			assertFalse(source.contains("GameThreadPoolServices"));
		}
		assertFalse(elyos.contains("857788"));
		assertFalse(elyos.contains("857795"));
		assertTrue(asmodian.contains("857799"));
		assertTrue(asmodian.contains("857803"));

		String aggressive = Files.readString(Path.of(
			"src/main/java/com/aionemu/gameserver/ai/AggressiveNpcAI2.java"));
		for (String legacy : new String[] { "knowledgeOfFlame", "knowledgeOfEarth", "knowledgeOfWater", "knowledgeOfAir" }) {
			assertFalse(aggressive.contains(legacy), legacy);
		}
	}

	private static Document parse(String path) throws Exception {
		return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(Path.of(path).toFile());
	}

	private static boolean exists(Document document, String expression) throws Exception {
		return (boolean) XPathFactory.newInstance().newXPath().evaluate(expression, document, XPathConstants.BOOLEAN);
	}

	private static int count(Document document, String expression) throws Exception {
		return ((Double) XPathFactory.newInstance().newXPath().evaluate("count(" + expression + ")", document,
			XPathConstants.NUMBER)).intValue();
	}
}
