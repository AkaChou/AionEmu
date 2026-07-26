package com.aionemu.gameserver.dataholders.loadingutils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.aionemu.gameserver.dataholders.RetailAiData;
import com.aionemu.gameserver.dataholders.RetailAiData.Operation;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import org.junit.jupiter.api.Test;

class RetailGiveScoreClosureTest {

	private static final Path AI_DIRECTORY = Path.of("src/main/resources/aion/definitions/compact/ai");
	private static final Path INSTANCE_COVERAGE = Path.of("src/main/resources/aion/definitions/compact/instance/coverage.xml");
	private static final Path INSTANCE_SPAWNS = Path.of("src/main/resources/aion/data/static_data/spawns/Instances");

	@Test
	void preservesReachableInstanceGiveScoreMatrix() throws Exception {
		String previousDefinitions = System.getProperty("aion.game.definitions.dir");
		try {
			System.setProperty("aion.game.definitions.dir", "src/main/resources/aion/definitions");
			RetailAiData data = new XmlDataLoader().loadRetailAiData();
			Map<Integer, String> mappings = loadMappings();
			Map<Integer, String> scoreTypes = loadScoreTypes();
			List<String> bindings = new ArrayList<>();
			Set<Integer> worlds = new HashSet<>();
			Set<Integer> npcs = new HashSet<>();
			Map<String, Long> events = new HashMap<>();
			Map<Integer, Long> applyTypes = new HashMap<>();

			for (var world : loadInstanceNpcs().entrySet()) {
				for (int npcId : world.getValue()) {
					RetailAiData.Pattern pattern = data.getPattern(npcId);
					if (pattern == null) {
						continue;
					}
					List<ScoreAction> actions = scoreActions(pattern);
					if (actions.isEmpty()) {
						continue;
					}
					var score = data.getNpcScore(npcId);
					assertNotNull(score, world.getKey() + ":" + npcId + ":" + pattern.name());
					assertEquals("NPC", scoreTypes.get(npcId), world.getKey() + ":" + npcId);
					worlds.add(world.getKey());
					npcs.add(npcId);
					applyTypes.merge(score.scoreApplyType(), (long) actions.size(), Long::sum);
					for (ScoreAction action : actions) {
						events.merge(action.event(), 1L, Long::sum);
						bindings.add(world.getKey() + "|" + npcId + "|" + mappings.get(npcId) + "|"
							+ action.event() + "|" + action.target() + "|NPC|" + score.scoreApplyType() + "|"
							+ score.equalizingScore() + "|" + score.value());
					}
				}
			}
			bindings.sort(String::compareTo);
			String report = String.join("\n", bindings);

			assertEquals(13, worlds.size(), report);
			assertEquals(58, npcs.size(), report);
			assertEquals(129, bindings.size(), report);
			assertEquals(Map.of("on_killed_by_user", 64L, "on_talked_by_user", 48L,
				"on_die", 14L, "on_user_enter_sensory_area", 3L), events, report);
			assertEquals(Map.of(0, 127L, 1, 1L, 2, 1L), applyTypes, report);
			assertEquals("d95f7f3a0cc24f23e8d3d2d15f6e94384ca447b30a7f09194c5c921175e43635",
				HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
					.digest(report.getBytes(StandardCharsets.UTF_8))), report);
		} finally {
			if (previousDefinitions == null) {
				System.clearProperty("aion.game.definitions.dir");
			} else {
				System.setProperty("aion.game.definitions.dir", previousDefinitions);
			}
		}
	}

	@Test
	void locksKamarInteractionScoreFamily() throws Exception {
		String previousDefinitions = System.getProperty("aion.game.definitions.dir");
		try {
			System.setProperty("aion.game.definitions.dir", "src/main/resources/aion/definitions");
			RetailAiData data = new XmlDataLoader().loadRetailAiData();
			Map<Integer, Integer> expected = Map.of(
				730861, 200, 730878, 200, 801766, 225, 801767, 225,
				801818, 225, 801819, 225, 801820, 225, 801821, 225);

			for (var entry : expected.entrySet()) {
				RetailAiData.Pattern pattern = data.getPattern(entry.getKey());
				assertNotNull(pattern, Integer.toString(entry.getKey()));
				assertEquals(List.of(new ScoreAction("on_talked_by_user", "USERI_TALKER")),
					scoreActions(pattern), pattern.name());
				var score = data.getNpcScore(entry.getKey());
				assertNotNull(score, Integer.toString(entry.getKey()));
				assertEquals(0, score.scoreApplyType(), pattern.name());
				assertEquals(0, score.equalizingScore(), pattern.name());
				assertEquals(entry.getValue(), score.value(), pattern.name());
				assertEquals("despawn_self", pattern.event("on_talked_by_user").getFirst().actions().getLast().type(),
					pattern.name());
			}
		} finally {
			if (previousDefinitions == null) {
				System.clearProperty("aion.game.definitions.dir");
			} else {
				System.setProperty("aion.game.definitions.dir", previousDefinitions);
			}
		}
	}

	@Test
	void locksIdgelDomeLandmarkTerminalScoreFamily() throws Exception {
		String previousDefinitions = System.getProperty("aion.game.definitions.dir");
		try {
			System.setProperty("aion.game.definitions.dir", "src/main/resources/aion/definitions");
			RetailAiData data = new XmlDataLoader().loadRetailAiData();
			Map<Integer, Integer> expectedApplyTypes = Map.of(833914, 1, 833922, 2);

			for (var entry : expectedApplyTypes.entrySet()) {
				RetailAiData.Pattern pattern = data.getPattern(entry.getKey());
				assertNotNull(pattern, Integer.toString(entry.getKey()));
				assertEquals(List.of(new ScoreAction("on_killed_by_user", "USERI_KILLER")),
					scoreActions(pattern), pattern.name());
				var score = data.getNpcScore(entry.getKey());
				assertNotNull(score, Integer.toString(entry.getKey()));
				assertEquals(entry.getValue(), score.scoreApplyType(), pattern.name());
				assertEquals(0, score.equalizingScore(), pattern.name());
				assertEquals(30_000, score.value(), pattern.name());
			}
		} finally {
			if (previousDefinitions == null) {
				System.clearProperty("aion.game.definitions.dir");
			} else {
				System.setProperty("aion.game.definitions.dir", previousDefinitions);
			}
		}
	}

	@Test
	void locksHarmonyScoreOwnership() throws Exception {
		String previousDefinitions = System.getProperty("aion.game.definitions.dir");
		try {
			System.setProperty("aion.game.definitions.dir", "src/main/resources/aion/definitions");
			RetailAiData data = new XmlDataLoader().loadRetailAiData();
			Map<Integer, Set<Integer>> scoreGroups = Map.of(
				200, Set.of(207099),
				400, Set.of(207102, 207116, 207117, 219277, 219278, 219279, 219481, 219485, 219486, 219648, 219652, 243678),
				100, Set.of(207101, 219280, 219281, 219282, 219649, 243679),
				50, Set.of(219283, 219284, 219285, 219328, 219650, 243680));

			Set<Integer> owned = new HashSet<>();
			for (var scoreGroup : scoreGroups.entrySet()) {
				for (int npcId : scoreGroup.getValue()) {
					owned.add(npcId);
					var score = data.getNpcScore(npcId);
					assertNotNull(score, Integer.toString(npcId));
					assertEquals(0, score.scoreApplyType(), Integer.toString(npcId));
					assertEquals(0, score.equalizingScore(), Integer.toString(npcId));
					assertEquals(scoreGroup.getKey(), score.value(), Integer.toString(npcId));
					assertEquals(1, scoreActions(data.getPattern(npcId)).size(), Integer.toString(npcId));
				}
			}
			assertEquals(25, owned.size());
		} finally {
			if (previousDefinitions == null) {
				System.clearProperty("aion.game.definitions.dir");
			} else {
				System.setProperty("aion.game.definitions.dir", previousDefinitions);
			}
		}
	}

	private static List<ScoreAction> scoreActions(RetailAiData.Pattern pattern) {
		return pattern.events().entrySet().stream()
			.flatMap(event -> event.getValue().stream().flatMap(rule -> rule.actions().stream()
				.filter(action -> action.type().equals("give_score"))
				.map(action -> new ScoreAction(event.getKey(), value(action, "target")))))
			.distinct().toList();
	}

	private static String value(Operation operation, String name) {
		return operation.values().getOrDefault(name, "").trim();
	}

	private static Map<Integer, Set<Integer>> loadInstanceNpcs() throws Exception {
		Set<Integer> coveredWorlds = new HashSet<>();
		try (InputStream stream = Files.newInputStream(INSTANCE_COVERAGE)) {
			XMLStreamReader reader = xml().createXMLStreamReader(stream);
			while (reader.hasNext()) {
				if (reader.next() == XMLStreamConstants.START_ELEMENT && reader.getLocalName().equals("world")) {
					coveredWorlds.add(Integer.parseInt(reader.getAttributeValue(null, "id")));
				}
			}
			reader.close();
		}

		Map<Integer, Set<Integer>> result = new HashMap<>();
		List<Path> files;
		try (var paths = Files.list(INSTANCE_SPAWNS)) {
			files = paths.filter(path -> path.toString().endsWith(".xml")).sorted().toList();
		}
		for (Path file : files) {
			loadSpawnNpcs(file, "spawn_map", "map_id", "spawn", "npc_id", coveredWorlds, result);
		}
		loadSpawnNpcs(AI_DIRECTORY.resolve("condition-spawns.xml"), "world", "id", "npc", "id", coveredWorlds, result);
		return result;
	}

	private static void loadSpawnNpcs(Path file, String worldElement, String worldAttribute, String npcElement,
			String npcAttribute, Set<Integer> coveredWorlds, Map<Integer, Set<Integer>> result) throws Exception {
		try (InputStream stream = Files.newInputStream(file)) {
			XMLStreamReader reader = xml().createXMLStreamReader(stream);
			Integer worldId = null;
			while (reader.hasNext()) {
				int event = reader.next();
				if (event == XMLStreamConstants.START_ELEMENT && reader.getLocalName().equals(worldElement)) {
					int candidate = Integer.parseInt(reader.getAttributeValue(null, worldAttribute));
					worldId = coveredWorlds.contains(candidate) ? candidate : null;
				} else if (event == XMLStreamConstants.START_ELEMENT && worldId != null
						&& reader.getLocalName().equals(npcElement)) {
					result.computeIfAbsent(worldId, ignored -> new HashSet<>())
						.add(Integer.parseInt(reader.getAttributeValue(null, npcAttribute)));
				} else if (event == XMLStreamConstants.END_ELEMENT && reader.getLocalName().equals(worldElement)) {
					worldId = null;
				}
			}
			reader.close();
		}
	}

	private static Map<Integer, String> loadMappings() throws Exception {
		Map<Integer, String> mappings = new HashMap<>();
		try (InputStream stream = Files.newInputStream(AI_DIRECTORY.resolve("npc-ai.xml"))) {
			XMLStreamReader reader = xml().createXMLStreamReader(stream);
			while (reader.hasNext()) {
				if (reader.next() == XMLStreamConstants.START_ELEMENT && reader.getLocalName().equals("npc")) {
					mappings.put(Integer.parseInt(reader.getAttributeValue(null, "id")), reader.getAttributeValue(null, "ai"));
				}
			}
			reader.close();
		}
		return mappings;
	}

	private static Map<Integer, String> loadScoreTypes() throws Exception {
		Map<Integer, String> scoreTypes = new HashMap<>();
		try (InputStream stream = Files.newInputStream(AI_DIRECTORY.resolve("npc-scores.xml"))) {
			XMLStreamReader reader = xml().createXMLStreamReader(stream);
			while (reader.hasNext()) {
				if (reader.next() == XMLStreamConstants.START_ELEMENT && reader.getLocalName().equals("npc_score")) {
					scoreTypes.put(Integer.parseInt(reader.getAttributeValue(null, "npc_id")),
						reader.getAttributeValue(null, "type"));
				}
			}
			reader.close();
		}
		return scoreTypes;
	}

	private static XMLInputFactory xml() {
		XMLInputFactory factory = XMLInputFactory.newFactory();
		factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
		factory.setProperty("javax.xml.stream.isSupportingExternalEntities", false);
		return factory;
	}

	private record ScoreAction(String event, String target) {
	}
}
