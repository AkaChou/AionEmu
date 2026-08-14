package com.aionemu.gameserver.dataholders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;

class RetailOpenWorldSpawnDataTest {

	private static final Path NPCS = Path.of("src/main/resources/aion/data/static_data/spawns/Npcs");
	private static final Path RETAIL_WAYPOINTS = Path.of(
		"src/main/resources/aion/definitions/compact/ai/ai-waypoints.xml");
	private static final Map<String, Integer> RETAIL_WALKER_SPOTS = Map.of(
		"210060000_Theobomos.xml", 6,
		"220030000_Altgard.xml", 1,
		"220070000_Gelkmaros.xml", 9,
		"220140000_Gelkmaros [Master Server].xml", 25,
		"400010000_Reshanta.xml", 23,
		"600100000_Levinshor.xml", 4,
		"700010000_Oriel.xml", 64,
		"710010000_Pernon.xml", 33);

	@Test
	void keepsPoetaQooqooPatrol() throws Exception {
		var document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
			.parse(NPCS.resolve("210010000_Poeta.xml").toFile());
		var spawns = document.getElementsByTagName("spawn");
		int spots = 0;
		int movingSpots = 0;
		for (int i = 0; i < spawns.getLength(); i++) {
			Element spawn = (Element) spawns.item(i);
			if (!"210340".equals(spawn.getAttribute("npc_id"))) {
				continue;
			}
			var qooqooSpots = spawn.getElementsByTagName("spot");
			spots += qooqooSpots.getLength();
			for (int spotIndex = 0; spotIndex < qooqooSpots.getLength(); spotIndex++) {
				Element spot = (Element) qooqooSpots.item(spotIndex);
				if ("2".equals(spot.getAttribute("random_walk"))) {
					movingSpots++;
				}
			}
		}

		assertEquals(6, spots);
		assertEquals(spots, movingSpots);
	}

	@Test
	void keepsNymphGownAtReachableHeightAndRetailNightWindow() throws Exception {
		assertTemporarySpawn("210010000_Poeta.xml", 700008,
			483.675537, 1544.752441, 108.885570);
	}

	@Test
	void keepsRetailOpenWorldWalkerBindingsResolvable() throws Exception {
		SpawnsData2.load(NPCS.toFile(), null);

		var waypointDocument = DocumentBuilderFactory.newInstance().newDocumentBuilder()
			.parse(RETAIL_WAYPOINTS.toFile());
		var templates = waypointDocument.getElementsByTagName("walker_template");
		Set<String> routeIds = new HashSet<>();
		for (int i = 0; i < templates.getLength(); i++) {
			routeIds.add(((Element) templates.item(i)).getAttribute("route_id"));
		}

		int boundSpots = 0;
		Set<String> boundRoutes = new HashSet<>();
		for (var entry : RETAIL_WALKER_SPOTS.entrySet()) {
			String fileName = entry.getKey();
			String routePrefix = "retail:" + fileName.substring(0, 9) + ":";
			var spots = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(NPCS.resolve(fileName).toFile())
				.getElementsByTagName("spot");
			int fileBindings = 0;
			for (int i = 0; i < spots.getLength(); i++) {
				Element spot = (Element) spots.item(i);
				String walkerId = spot.getAttribute("walker_id");
				if (!walkerId.startsWith(routePrefix)) {
					continue;
				}
				assertFalse(spot.hasAttribute("random_walk"), fileName + ": " + walkerId);
				assertTrue(routeIds.contains(walkerId), walkerId);
				boundRoutes.add(walkerId);
				fileBindings++;
			}
			assertEquals(entry.getValue().intValue(), fileBindings, fileName);
			boundSpots += fileBindings;
		}

		assertEquals(165, boundSpots);
		assertEquals(164, boundRoutes.size());
	}

	private static void assertTemporarySpawn(String fileName, int npcId, double x, double y, double z)
			throws Exception {
		var document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
			.parse(NPCS.resolve(fileName).toFile());
		var spawns = document.getElementsByTagName("spawn");
		for (int i = 0; i < spawns.getLength(); i++) {
			Element spawn = (Element) spawns.item(i);
			if (!Integer.toString(npcId).equals(spawn.getAttribute("npc_id"))) {
				continue;
			}
			Element window = (Element) spawn.getElementsByTagName("temporary_spawn").item(0);
			Element spot = (Element) spawn.getElementsByTagName("spot").item(0);
			assertEquals("19.*.*", window.getAttribute("spawn_time"));
			assertEquals("8.*.*", window.getAttribute("despawn_time"));
			assertEquals(x, Double.parseDouble(spot.getAttribute("x")), 0.000001);
			assertEquals(y, Double.parseDouble(spot.getAttribute("y")), 0.000001);
			assertEquals(z, Double.parseDouble(spot.getAttribute("z")), 0.000001);
			return;
		}
		throw new AssertionError("missing NPC " + npcId + " in " + fileName);
	}
}
