package com.aionemu.gameserver.dataholders.loadingutils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.aionemu.gameserver.ai.RetailPatternAI2;
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
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import org.junit.jupiter.api.Test;

class RetailAiSkillSlotClosureTest {

	private static final Path AI_DIRECTORY = Path.of("src/main/resources/aion/definitions/compact/ai");
	private static final Path NPC_SKILLS = Path.of("src/main/resources/aion/definitions/compact/skills/npc-skills.xml");
	private static final Path INSTANCE_COVERAGE = Path.of("src/main/resources/aion/definitions/compact/instance/coverage.xml");
	private static final Path INSTANCE_SPAWNS = Path.of("src/main/resources/aion/data/static_data/spawns/Instances");
	private static final Path AI_WAYPOINTS = AI_DIRECTORY.resolve("ai-waypoints.xml");
	private static final Pattern SKILL_INDEX = Pattern.compile("SKILLI_INDEX_(\\d+)");
	private static final Pattern VERSION_SUFFIX = Pattern.compile("_ver\\d+$");

	@Test
	void preservesKnownStructuralPatternGaps() throws Exception {
		String previousDefinitions = System.getProperty("aion.game.definitions.dir");
		try {
			System.setProperty("aion.game.definitions.dir", "src/main/resources/aion/definitions");
			var data = new XmlDataLoader().loadRetailAiData();
			var unsupportedPatterns = StreamSupport.stream(data.patterns().spliterator(), false)
				.filter(pattern -> RetailPatternAI2.unsupportedReason(pattern) != null).toList();
			List<String> patterns = unsupportedPatterns.stream()
				.map(pattern -> pattern.name() + ":" + RetailPatternAI2.unsupportedReason(pattern)).sorted().toList();
			Map<String, Long> issues = StreamSupport.stream(data.patterns().spliterator(), false)
				.map(RetailPatternAI2::unsupportedReason).filter(java.util.Objects::nonNull)
				.collect(Collectors.groupingBy(reason -> reason, Collectors.counting()));
			String report = String.join("\n", patterns);

			assertEquals(Map.ofEntries(
				Map.entry("unsupported action activate_skillarea in on_idle_timer", 2L),
				Map.entry("unsupported action give_abysspoint in on_attacked", 1L),
				Map.entry("unsupported action give_money in on_attacked", 1L),
				Map.entry("unsupported action give_world_score in on_attacked", 1L),
				Map.entry("unsupported action spawn in on_wake_up", 1L),
				Map.entry("unsupported action use_skill in on_attacked", 1L),
				Map.entry("unsupported action use_skill in on_message", 1L),
				Map.entry("unsupported action use_skill in on_see_user", 1L),
				Map.entry("unsupported action use_skill_by_attacker_indicator in on_battle_timer", 4L),
				Map.entry("unsupported condition is_on_time in on_see_user", 1L),
				Map.entry("unsupported condition is_user_class in on_see_user", 1L)), issues, report);
			assertEquals(15, patterns.size(), report);
			assertEquals("6f7fe06244ec38fd925d6b870c20055c8cf9a027f0f28218a4987d44ea8df220", HexFormat.of().formatHex(
				MessageDigest.getInstance("SHA-256").digest(report.getBytes(StandardCharsets.UTF_8))), report);

			Set<String> testPatterns = unsupportedPatterns.stream().map(pattern -> pattern.name())
				.filter(name -> name.toLowerCase(Locale.ROOT).startsWith("test")).collect(Collectors.toSet());
			assertEquals(Set.of("TEST_AI_GIVE_AbyssPoint", "TEST_AI_GIVE_Money", "TEST_AI_GIVE_WorldScore",
				"Test_Basic_Monster_AI_JSM_1", "Test_Basic_Monster_AI_KMD_2"), testPatterns);
			Set<String> emptySkillPatterns = unsupportedPatterns.stream().filter(pattern -> !testPatterns.contains(pattern.name()))
				.filter(pattern -> pattern.events().values().stream().flatMap(List::stream)
					.flatMap(rule -> rule.actions().stream()).anyMatch(action -> "SKILLI_NONE".equals(action.values().get("skill"))))
				.map(pattern -> pattern.name()).collect(Collectors.toSet());
			assertEquals(Set.of("IDArena_S1_D_Monster_4", "IDArena_S1_Monster_4", "IDArena_pvp02_S1_Drakan_02",
				"IDDF3_T_Monster_04", "IDEternity_02_Tower_Area_Ctrl_06", "IDEternity_02_Tower_Area_Ctrl_07",
				"IDEternity_Q_Sado_As_02", "Raksha_Dragon_HNmd", "Raksha_Dragon_NNmd"), emptySkillPatterns);
			var emptyPathPattern = unsupportedPatterns.stream().filter(pattern -> pattern.name().equals("IDSeal_Guardian_Chief_02"))
				.findFirst().orElseThrow();
			assertEquals(1, emptyPathPattern.events().values().stream().flatMap(List::stream).flatMap(rule -> rule.actions().stream())
				.filter(action -> "SPAWN_LOCATION_WAY_POINT_START".equals(action.values().get("spawn_location_type")))
				.filter(action -> action.values().getOrDefault("pathname", "").isEmpty()).count());

			Map<Integer, String> mappings = loadMappings();
			Set<String> unsupportedNames = unsupportedPatterns.stream().map(pattern -> pattern.name().toLowerCase(Locale.ROOT))
				.collect(Collectors.toSet());
			List<String> bindings = new ArrayList<>();
			loadInstanceNpcs().entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(world -> world.getValue().stream()
				.sorted().filter(npcId -> unsupportedNames.contains(mappings.getOrDefault(npcId, "").toLowerCase(Locale.ROOT)))
				.forEach(npcId -> bindings.add(world.getKey() + ":" + npcId + ":" + mappings.get(npcId))));
			assertEquals(List.of("300300000:217478:IDArena_S1_Monster_4", "300300000:217487:IDArena_S1_D_Monster_4",
				"301390000:855461:IDSeal_Guardian_Chief_02", "301550000:220563:IDEternity_02_Tower_Area_Ctrl_06",
				"301550000:220564:IDEternity_02_Tower_Area_Ctrl_07"), bindings);
		} finally {
			if (previousDefinitions == null) {
				System.clearProperty("aion.game.definitions.dir");
			} else {
				System.setProperty("aion.game.definitions.dir", previousDefinitions);
			}
		}
	}

	@Test
	void preservesKnownRetailNpcSkillSlotGaps() throws Exception {
		Map<String, Set<Integer>> patternIndexes = loadPatternIndexes();
		Map<Integer, String> mappings = loadMappings();
		Map<Integer, boolean[]> assignments = loadAssignments();
		Map<String, Integer> issues = new HashMap<>();
		Map<Integer, String> issuesByNpc = new HashMap<>();

		for (var mapping : mappings.entrySet()) {
			String ai = mapping.getValue().toLowerCase(Locale.ROOT);
			Set<Integer> indexes = patternIndexes.get(ai);
			if (indexes == null) {
				indexes = patternIndexes.get(VERSION_SUFFIX.matcher(ai).replaceFirst(""));
			}
			if (indexes == null || indexes.isEmpty()) {
				continue;
			}
			boolean[] slots = assignments.get(mapping.getKey());
			if (slots == null) {
				issues.merge("missing_assignment", 1, Integer::sum);
				issuesByNpc.put(mapping.getKey(), "missing_assignment");
			} else if (indexes.stream().anyMatch(index -> index >= slots.length)) {
				issues.merge("out_of_range", 1, Integer::sum);
				issuesByNpc.put(mapping.getKey(), "out_of_range");
			} else if (indexes.stream().anyMatch(index -> !slots[index])) {
				issues.merge("orphan", 1, Integer::sum);
				issuesByNpc.put(mapping.getKey(), "orphan");
			}
		}

			assertEquals(Map.of("missing_assignment", 828, "out_of_range", 1620, "orphan", 7), issues);
			assertEquals(2455, issues.values().stream().mapToInt(Integer::intValue).sum());

		Map<Integer, Set<Integer>> instanceNpcs = loadInstanceNpcs();
		Map<String, Integer> instanceIssues = new HashMap<>();
		Set<Integer> affectedWorlds = new HashSet<>();
		Set<Integer> affectedNpcs = new HashSet<>();
		List<String> details = new ArrayList<>();
		instanceNpcs.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(world -> world.getValue().stream()
			.sorted().filter(issuesByNpc::containsKey).forEach(npcId -> {
				String issue = issuesByNpc.get(npcId);
				instanceIssues.merge(issue, 1, Integer::sum);
				affectedWorlds.add(world.getKey());
				affectedNpcs.add(npcId);
				details.add(world.getKey() + ":" + npcId + ":" + mappings.get(npcId) + ":" + issue);
			}));
		String report = String.join("\n", details);
			assertEquals(Map.of("missing_assignment", 54, "out_of_range", 90), instanceIssues, report);
			assertEquals(35, affectedWorlds.size(), report);
			assertEquals(139, affectedNpcs.size(), report);
			assertEquals(144, details.size(), report);
			assertEquals("5b29857f7430107f210ed05da8f3da11a3e9f705986e1167f1115b2fef9acd75",
				HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(report.getBytes(StandardCharsets.UTF_8))), report);
	}

	@Test
	void preservesKnownRetailRuntimeDataGaps() throws Exception {
		String previousDefinitions = System.getProperty("aion.game.definitions.dir");
		try {
			System.setProperty("aion.game.definitions.dir", "src/main/resources/aion/definitions");
			RetailAiData data = new XmlDataLoader().loadRetailAiData();
			var patterns = StreamSupport.stream(data.patterns().spliterator(), false).toList();
			Set<String> globalGaps = new HashSet<>();
			for (var pattern : patterns) {
				pattern.events().values().stream().flatMap(List::stream).flatMap(rule -> rule.actions().stream())
					.map(action -> runtimeDataGap(data, action)).filter(java.util.Objects::nonNull)
					.forEach(gap -> globalGaps.add(pattern.name() + ':' + gap));
			}
			assertEquals(Map.of("empty_spawn_path", 1L, "missing_cross_world_variable", 29L, "unknown_npc_name", 24L),
				gapCounts(globalGaps, 1));
			assertEquals(54, globalGaps.size());
			assertEquals("7f7d4444b88fdef7d5efc777a7aa693282bd15b2be12e7d48257b2b6a8b1cd37",
				hashReport(globalGaps));
			assertEquals(14, globalGaps.stream().filter(gap -> gap.contains(":unknown_npc_name:"))
				.map(gap -> gap.substring(gap.lastIndexOf(':') + 1)).distinct().count());

			Map<String, RetailAiData.Pattern> patternsByName = patterns.stream()
				.collect(Collectors.toMap(pattern -> pattern.name().toLowerCase(Locale.ROOT), pattern -> pattern));
			Map<Integer, String> mappings = loadMappings();
			Set<String> walkerIds = loadWalkerIds();
			Set<String> instanceGaps = new HashSet<>();
			for (var world : loadInstanceNpcs().entrySet()) {
				for (int npcId : world.getValue()) {
					String ai = mappings.getOrDefault(npcId, "");
					RetailAiData.Pattern pattern = patternsByName.get(ai.toLowerCase(Locale.ROOT));
					if (pattern == null) {
						pattern = patternsByName.get(VERSION_SUFFIX.matcher(ai.toLowerCase(Locale.ROOT)).replaceFirst(""));
					}
					if (pattern == null) {
						continue;
					}
					for (Operation action : pattern.events().values().stream().flatMap(List::stream)
							.flatMap(rule -> rule.actions().stream()).toList()) {
						String gap = runtimeDataGap(data, action);
						String pathname = value(action, "pathname");
						if (action.type().equals("spawn") && !pathname.isBlank()
								&& !walkerIds.contains(("retail:" + world.getKey() + ':' + pathname).toLowerCase(Locale.ROOT))) {
							gap = "missing_spawn_path:" + pathname;
						}
						if (gap != null) {
							instanceGaps.add(world.getKey() + ":" + npcId + ":" + ai + ':' + gap);
						}
					}
				}
			}
			assertEquals(Map.of("empty_spawn_path", 1L, "missing_spawn_path", 74L, "unknown_npc_name", 2L),
				gapCounts(instanceGaps, 3));
			assertEquals(77, instanceGaps.size());
			assertEquals("31a028e0c74096a4d3dfad08277ae39d9930af110a4436c63cfa9306f410e3a2",
				hashReport(instanceGaps));
		} finally {
			if (previousDefinitions == null) {
				System.clearProperty("aion.game.definitions.dir");
			} else {
				System.setProperty("aion.game.definitions.dir", previousDefinitions);
			}
		}
	}

	@Test
	void locksAreaSceneAndSpawnConsumerMatrix() throws Exception {
		String previousDefinitions = System.getProperty("aion.game.definitions.dir");
		try {
			System.setProperty("aion.game.definitions.dir", "src/main/resources/aion/definitions");
			RetailAiData data = new XmlDataLoader().loadRetailAiData();
			Set<String> actionTypes = Set.of("activate_skillarea", "change_world_scene_status", "despawn_by_nameid",
				"enable_area", "on_off_moving_collision", "on_off_windpath", "spawn", "spawn_on_multi_target",
				"spawn_on_target", "spawn_on_target_by_attacker_indicator");
			var patterns = StreamSupport.stream(data.patterns().spliterator(), false).toList();
			Map<String, Long> actionCounts = new java.util.TreeMap<>(patterns.stream()
				.flatMap(pattern -> pattern.events().values().stream()).flatMap(List::stream)
				.flatMap(rule -> rule.actions().stream()).map(Operation::type).filter(actionTypes::contains)
				.collect(Collectors.groupingBy(type -> type, Collectors.counting())));
			Map<String, Long> patternCounts = new java.util.TreeMap<>(patterns.stream()
				.flatMap(pattern -> pattern.events().values().stream().flatMap(List::stream)
					.flatMap(rule -> rule.actions().stream()).map(Operation::type).filter(actionTypes::contains).distinct())
				.collect(Collectors.groupingBy(type -> type, Collectors.counting())));

			Map<String, RetailAiData.Pattern> patternsByName = patterns.stream()
				.collect(Collectors.toMap(pattern -> pattern.name().toLowerCase(Locale.ROOT), pattern -> pattern));
			Map<Integer, String> mappings = loadMappings();
			Set<String> bindings = new HashSet<>();
			for (var world : loadInstanceNpcs().entrySet()) {
				for (int npcId : world.getValue()) {
					String ai = mappings.getOrDefault(npcId, "");
					RetailAiData.Pattern pattern = patternsByName.get(ai.toLowerCase(Locale.ROOT));
					if (pattern == null) {
						pattern = patternsByName.get(VERSION_SUFFIX.matcher(ai.toLowerCase(Locale.ROOT)).replaceFirst(""));
					}
					if (pattern == null) {
						continue;
					}
					pattern.events().values().stream().flatMap(List::stream).flatMap(rule -> rule.actions().stream())
						.map(Operation::type).filter(actionTypes::contains).distinct()
						.forEach(type -> bindings.add(world.getKey() + ":" + npcId + ':' + ai + ':' + type));
				}
			}
			Map<String, Long> bindingCounts = new java.util.TreeMap<>(bindings.stream()
				.collect(Collectors.groupingBy(binding -> binding.substring(binding.lastIndexOf(':') + 1), Collectors.counting())));
			assertEquals(Map.ofEntries(
				Map.entry("activate_skillarea", 552L), Map.entry("change_world_scene_status", 101L),
				Map.entry("despawn_by_nameid", 849L), Map.entry("enable_area", 574L),
				Map.entry("on_off_moving_collision", 85L), Map.entry("on_off_windpath", 54L),
				Map.entry("spawn", 16357L), Map.entry("spawn_on_multi_target", 324L),
				Map.entry("spawn_on_target", 895L), Map.entry("spawn_on_target_by_attacker_indicator", 306L)),
				actionCounts);
			assertEquals(Map.ofEntries(
				Map.entry("activate_skillarea", 114L), Map.entry("change_world_scene_status", 92L),
				Map.entry("despawn_by_nameid", 171L), Map.entry("enable_area", 189L),
				Map.entry("on_off_moving_collision", 38L), Map.entry("on_off_windpath", 25L),
				Map.entry("spawn", 3772L), Map.entry("spawn_on_multi_target", 84L),
				Map.entry("spawn_on_target", 488L), Map.entry("spawn_on_target_by_attacker_indicator", 64L)),
				patternCounts);
			assertEquals(Map.ofEntries(
				Map.entry("activate_skillarea", 34L), Map.entry("change_world_scene_status", 89L),
				Map.entry("despawn_by_nameid", 75L), Map.entry("enable_area", 45L),
				Map.entry("on_off_moving_collision", 17L), Map.entry("on_off_windpath", 2L),
				Map.entry("spawn", 1197L), Map.entry("spawn_on_multi_target", 61L),
				Map.entry("spawn_on_target", 215L), Map.entry("spawn_on_target_by_attacker_indicator", 55L)),
				bindingCounts);
			assertEquals(1790, bindings.size());
			assertEquals("fa12cb4e745e171e9e58c8d28c3ceab4bace8c65b3ab798370e3e533eb4dacdc",
				hashReport(bindings));
		} finally {
			if (previousDefinitions == null) {
				System.clearProperty("aion.game.definitions.dir");
			} else {
				System.setProperty("aion.game.definitions.dir", previousDefinitions);
			}
		}
	}

	private static String runtimeDataGap(RetailAiData data, Operation action) {
		String npcName = switch (action.type()) {
			case "despawn_by_nameid" -> value(action, "target_npc_nameid");
			case "spawn", "spawn_on_target", "spawn_on_target_by_attacker_indicator", "spawn_on_multi_target" ->
				value(action, "npc_nameid");
			default -> "";
		};
		if (!npcName.isBlank() && data.findNpcId(npcName) == null) {
			return "unknown_npc_name:" + npcName;
		}
		if (action.type().equals("set_condition_spawn_variable_to_world")) {
			String world = value(action, "worldid");
			Integer worldId = data.findConditionWorldId(world);
			String variable = value(action, "string");
			if (worldId == null || !data.supportsConditionVariable(worldId, variable)) {
				return "missing_cross_world_variable:" + world + ':' + variable;
			}
		}
		if (action.type().equals("spawn") && value(action, "spawn_location_type").equals("SPAWN_LOCATION_WAY_POINT_START")
				&& value(action, "pathname").isBlank()) {
			return "empty_spawn_path:";
		}
		return null;
	}

	private static String value(Operation operation, String name) {
		return operation.values().getOrDefault(name, "").trim();
	}

	private static Map<String, Long> gapCounts(Set<String> gaps, int categoryIndex) {
		return gaps.stream().collect(Collectors.groupingBy(gap -> gap.split(":")[categoryIndex], Collectors.counting()));
	}

	private static String hashReport(Set<String> gaps) throws Exception {
		String report = gaps.stream().sorted().collect(Collectors.joining("\n"));
		return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(report.getBytes(StandardCharsets.UTF_8)));
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

	private static Map<String, Set<Integer>> loadPatternIndexes() throws Exception {
		Map<String, Set<Integer>> patterns = new HashMap<>();
		java.util.List<Path> files;
		try (var paths = Files.list(AI_DIRECTORY)) {
			files = paths.filter(path -> path.getFileName().toString().startsWith("npcaipatterns"))
				.filter(path -> path.toString().endsWith(".xml")).sorted().toList();
		}
		for (Path file : files) {
			try (InputStream stream = Files.newInputStream(file)) {
			XMLStreamReader reader = xml().createXMLStreamReader(stream);
			String name = null;
			Set<Integer> indexes = null;
			while (reader.hasNext()) {
				int event = reader.next();
				if (event == XMLStreamConstants.START_ELEMENT && reader.getLocalName().equals("npc_ai_pattern")) {
					indexes = new HashSet<>();
				} else if (event == XMLStreamConstants.START_ELEMENT && indexes != null
						&& reader.getLocalName().equals("name")) {
					name = reader.getElementText().toLowerCase(Locale.ROOT);
				} else if (event == XMLStreamConstants.START_ELEMENT && indexes != null
						&& reader.getLocalName().equals("skill")) {
					Matcher matcher = SKILL_INDEX.matcher(reader.getElementText());
					if (matcher.matches()) {
						indexes.add(Integer.parseInt(matcher.group(1)));
					}
				} else if (event == XMLStreamConstants.END_ELEMENT && reader.getLocalName().equals("npc_ai_pattern")) {
					patterns.put(name, Set.copyOf(indexes));
					name = null;
					indexes = null;
				}
			}
			reader.close();
			}
		}
		return patterns;
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

	private static Set<String> loadWalkerIds() throws Exception {
		Set<String> walkerIds = new HashSet<>();
		try (InputStream stream = Files.newInputStream(AI_WAYPOINTS)) {
			XMLStreamReader reader = xml().createXMLStreamReader(stream);
			while (reader.hasNext()) {
				if (reader.next() == XMLStreamConstants.START_ELEMENT && reader.getLocalName().equals("walker_template")) {
					walkerIds.add(reader.getAttributeValue(null, "route_id").toLowerCase(Locale.ROOT));
				}
			}
			reader.close();
		}
		return walkerIds;
	}

	private static Map<Integer, boolean[]> loadAssignments() throws Exception {
		Map<String, boolean[]> groups = new HashMap<>();
		Map<Integer, boolean[]> assignments = new HashMap<>();
		try (InputStream stream = Files.newInputStream(NPC_SKILLS)) {
			XMLStreamReader reader = xml().createXMLStreamReader(stream);
			String group = null;
			java.util.List<Boolean> slots = null;
			while (reader.hasNext()) {
				int event = reader.next();
				if (event == XMLStreamConstants.START_ELEMENT && reader.getLocalName().equals("group")) {
					group = reader.getAttributeValue(null, "id");
					slots = new java.util.ArrayList<>();
				} else if (event == XMLStreamConstants.START_ELEMENT && slots != null
						&& reader.getLocalName().equals("skill")) {
					slots.add(reader.getAttributeValue(null, "id") != null);
				} else if (event == XMLStreamConstants.END_ELEMENT && reader.getLocalName().equals("group")) {
					boolean[] resolved = new boolean[slots.size()];
					for (int i = 0; i < resolved.length; i++) {
						resolved[i] = slots.get(i);
					}
					groups.put(group, resolved);
					group = null;
					slots = null;
				} else if (event == XMLStreamConstants.START_ELEMENT && reader.getLocalName().equals("assign")) {
					boolean[] resolved = groups.get(reader.getAttributeValue(null, "group"));
					for (String npcId : reader.getAttributeValue(null, "npc_ids").trim().split("\\s+")) {
						assignments.put(Integer.parseInt(npcId), resolved);
					}
				}
			}
			reader.close();
		}
		return assignments;
	}

	private static XMLInputFactory xml() {
		XMLInputFactory factory = XMLInputFactory.newFactory();
		factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
		factory.setProperty("javax.xml.stream.isSupportingExternalEntities", false);
		return factory;
	}
}
