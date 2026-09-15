package com.aionemu.gameserver.dataholders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
	void keepsNymphGownRetailReferenceHeightAndNightWindow() throws Exception {
		assertTemporarySpawn("210010000_Poeta.xml", 700008,
			483.675537, 1544.752441, 114.441620);
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

	/** 海意隆 D9 亡灵农场的 12 个共用点：白天骷髅、夜间亡灵。 / Heiron D9 undead farm: 12 shared spots, day-night swap. */
	private static final Map<Integer, Set<Integer>> HEIRON_UNDEAD_FARM_PAIRS = Map.of(
		211977, Set.of(211979, 211981),
		211978, Set.of(211980, 211982));

	/**
	 * 校验海意隆 D9 亡灵农场 12 个共用点昼夜互斥：
	 * 白天侧变体块带 04:00-17:00 窗口并保留变体索引（initial_delay），
	 * 夜间四只亡灵带 17:00-04:00 窗口，且两侧点位逐点一致、常驻块不再包含共用点。
	 * Verifies the Heiron D9 undead farm swap: the day variant block owns the 6 shared spots with a
	 * 04:00-17:00 window (and stays indexed as a variant via initial_delay), the four night spirits
	 * carry the complementary 17:00-04:00 window, and the permanent block no longer holds them.
	 */
	@Test
	void keepsHeironUndeadFarmSharedSpotsSwappedByDayAndNight() throws Exception {
		var document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
			.parse(NPCS.resolve("210040000_Heiron.xml").toFile());
		var spawns = document.getElementsByTagName("spawn");

		Map<Integer, List<Element>> blocksByNpc = new HashMap<>();
		for (int i = 0; i < spawns.getLength(); i++) {
			Element spawn = (Element) spawns.item(i);
			blocksByNpc.computeIfAbsent(Integer.parseInt(spawn.getAttribute("npc_id")), ignored -> new ArrayList<>())
				.add(spawn);
		}

		Set<String> nightSpots = new HashSet<>();
		for (var pair : HEIRON_UNDEAD_FARM_PAIRS.entrySet()) {
			for (int nightId : pair.getValue()) {
				Element nightBlock = singleBlock(blocksByNpc, nightId);
				Element nightWindow = blockWindow(nightBlock);
				assertNotNull(nightWindow, "npc " + nightId + " 需要块级 temporary_spawn");
				assertEquals("17.*.*", nightWindow.getAttribute("spawn_time"), "npc " + nightId);
				assertEquals("4.*.*", nightWindow.getAttribute("despawn_time"), "npc " + nightId);
				nightSpots.addAll(spotKeys(nightBlock));
			}
		}
		assertEquals(12, nightSpots.size(), "夜间四只亡灵共用点总数");

		for (var pair : HEIRON_UNDEAD_FARM_PAIRS.entrySet()) {
			int dayId = pair.getKey();
			List<Element> dayBlocks = blocksByNpc.get(dayId);
			assertNotNull(dayBlocks, "npc " + dayId);
			assertEquals(2, dayBlocks.size(), "npc " + dayId + " 应为常驻块 + 白天变体块");

			Element dayVariant = dayBlocks.stream().filter(block -> blockWindow(block) != null).findFirst()
				.orElseThrow();
			Element permanent = dayBlocks.stream().filter(block -> blockWindow(block) == null).findFirst()
				.orElseThrow();

			Element dayWindow = blockWindow(dayVariant);
			assertEquals("4.*.*", dayWindow.getAttribute("spawn_time"), "npc " + dayId);
			assertEquals("17.*.*", dayWindow.getAttribute("despawn_time"), "npc " + dayId);
			// 同一 npc_id 的第二个 <spawn> 块只有带 initial_delay/spawn_page 才会作为变体一起加载。
			// A second <spawn> block of the same npc_id is indexed as a variant only when it carries
			// initial_delay / spawn_page.
			assertFalse("".equals(dayVariant.getAttribute("initial_delay")), "npc " + dayId + " 需要 initial_delay");
			assertFalse("0".equals(dayVariant.getAttribute("initial_delay")), "npc " + dayId + " initial_delay 不能为 0");

			Set<String> swappedSpots = spotKeys(dayVariant);
			Set<String> nightTwinSpots = new HashSet<>();
			for (int nightId : pair.getValue()) {
				nightTwinSpots.addAll(spotKeys(singleBlock(blocksByNpc, nightId)));
			}
			assertEquals(6, swappedSpots.size(), "npc " + dayId + " 白天侧共用点数量");
			assertEquals(nightTwinSpots, swappedSpots, "npc " + dayId + " 白天侧窗口点必须与夜间亡灵逐点一致");
			assertTrue(Collections.disjoint(swappedSpots, spotKeys(permanent)),
				"npc " + dayId + " 共用点不能残留在常驻块");
		}
	}

	private static Element singleBlock(Map<Integer, List<Element>> blocksByNpc, int npcId) {
		List<Element> blocks = blocksByNpc.get(npcId);
		assertNotNull(blocks, "npc " + npcId);
		assertEquals(1, blocks.size(), "npc " + npcId);
		return blocks.get(0);
	}

	/** 只取块级 <temporary_spawn>（spot 级窗口挂在 <spot> 之下，不算）。 / block level only. */
	private static Element blockWindow(Element spawn) {
		var windows = spawn.getElementsByTagName("temporary_spawn");
		for (int i = 0; i < windows.getLength(); i++) {
			Element window = (Element) windows.item(i);
			if (window.getParentNode() == spawn) {
				return window;
			}
		}
		return null;
	}

	private static Set<String> spotKeys(Element spawn) {
		Set<String> keys = new HashSet<>();
		var spots = spawn.getElementsByTagName("spot");
		for (int i = 0; i < spots.getLength(); i++) {
			Element spot = (Element) spots.item(i);
			keys.add(String.format("%.3f,%.3f", Double.parseDouble(spot.getAttribute("x")),
				Double.parseDouble(spot.getAttribute("y"))));
		}
		return keys;
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
			// 夜行窗口与任务目标 203175(21:00-04:00)对齐：机关单独在场会让
			// 任务 1114 的 add-npc-aggro 找不到目标。
			// Night window aligned with quest target 203175 (21:00-04:00): the
			// device being present alone leaves quest 1114's add-npc-aggro
			// without a target.
			assertEquals("21.*.*", window.getAttribute("spawn_time"));
			assertEquals("4.*.*", window.getAttribute("despawn_time"));
			assertEquals(x, Double.parseDouble(spot.getAttribute("x")), 0.000001);
			assertEquals(y, Double.parseDouble(spot.getAttribute("y")), 0.000001);
			assertEquals(z, Double.parseDouble(spot.getAttribute("z")), 0.000001);
			assertEquals("true", spot.getAttribute("resolve_z"));
			return;
		}
		throw new AssertionError("missing NPC " + npcId + " in " + fileName);
	}
}
