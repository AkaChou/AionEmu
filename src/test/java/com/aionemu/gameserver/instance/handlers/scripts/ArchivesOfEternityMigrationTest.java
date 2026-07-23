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

class ArchivesOfEternityMigrationTest {

	private static final String WORLD = "//world[@id='301540000']";

	@Test
	void retailConditionsOwnBossSecretRoomAndExitSpawns() throws Exception {
		Document conditions = parse("src/main/resources/aion/definitions/compact/ai/condition-spawns.xml");
		assertEquals(20, count(conditions, WORLD + "/variable"));
		assertEquals(1063, count(conditions, WORLD + "/condition"));
		assertEquals(1063, count(conditions, WORLD + "//slot"));
		assertEquals(66, count(conditions, WORLD + "//party"));
		for (String variable : new String[] { "1st_boss_room", "2nd_boss_room", "3rd_boss_room", "4th_boss_room",
			"artifact_a", "artifact_b", "artifact_c", "artifact_d", "artifact_reset", "boss_class", "boss_set",
			"end_boss_die", "race", "secret_room_choice", "spawn_set", "sub_boss_die", "teleport_01",
			"teleport_02", "teleport_03", "teleport_04" }) {
			assertTrue(exists(conditions, WORLD + "/variable[@name='" + variable + "']"), variable);
		}
		for (String npcId : new String[] { "857452", "857456", "857459", "857460", "857462", "857464", "806139" }) {
			assertTrue(exists(conditions, WORLD + "//npc[@id='" + npcId + "']"), npcId);
		}
		assertTrue(exists(conditions, WORLD + "/condition[contains(@expression, 'End_Boss_Die == 2') and "
			+ "contains(@expression, 'Race == 1')]//npc[@id='834053']"));
		assertTrue(exists(conditions, WORLD + "/condition[contains(@expression, 'End_Boss_Die == 2') and "
			+ "contains(@expression, 'Race == 2')]//npc[@id='834054']"));
		for (String bookId : new String[] { "703131", "703132", "703133", "703149", "703150", "703151" }) {
			assertFalse(exists(conditions, WORLD + "//npc[@id='" + bookId + "']"), bookId);
		}
	}

	@Test
	void staticSpawnsKeepTheRetailBookPoolAndConditionProducers() throws Exception {
		Document spawns = parse("src/main/resources/aion/data/static_data/spawns/Instances/301540000_Archives_Of_Eternity.xml");
		assertEquals(4, count(spawns, "//spawn[@npc_id='703134' and @pool='1']/spot"));
		for (String entityId : new String[] { "396", "399", "400", "404" }) {
			assertTrue(exists(spawns, "//spawn[@npc_id='703134' and @pool='1']/spot[@entity_id='" + entityId + "']"),
				entityId);
		}
		for (String npcId : new String[] { "702900", "857770", "857771", "857772", "857773", "857866" }) {
			assertTrue(exists(spawns, "//spawn[@npc_id='" + npcId + "']"), npcId);
		}
	}

	@Test
	void handlerOnlyBridgesUnsupportedRaceBooks() throws Exception {
		String handler = Files.readString(Path.of(
			"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/ArchivesOfEternityInstance.java"));
		for (String legacy : new String[] { "Rnd", "StaticDoor", "onInstanceCreate(", "onDie(", "sendMsg(", "220334",
			"806139", "806191", "806192", "806055", "806057", "857452", "super.onInstanceCreate(instance)" }) {
			assertFalse(handler.contains(legacy), legacy);
		}
		assertTrue(handler.contains("updateNearbyQuests()"));
		for (String bookId : new String[] { "703131", "703132", "703133", "703149", "703150", "703151" }) {
			assertTrue(handler.contains(bookId), bookId);
		}
		assertTrue(handler.contains("spawn(book2, 625.339844f, 500.463898f, 469.338898f"));
		assertTrue(handler.contains("spawn(book3, 527.469543f, 599.944214f, 469.338898f"));
		assertTrue(handler.contains("spawn(book1, 570.225403f, 338.267609f, 469.338898f"));
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
