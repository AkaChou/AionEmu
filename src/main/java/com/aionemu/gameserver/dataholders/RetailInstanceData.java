package com.aionemu.gameserver.dataholders;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public final class RetailInstanceData {

	private static final String ATTRIBUTE_LIMIT = "http://www.oracle.com/xml/jaxp/properties/elementAttributeLimit";
	private static final String[] FILES = { "definitions.xml", "limits.xml", "matchmaking.xml", "rewards.xml",
		"coverage.xml", "manifest.xml" };

	private final Map<Integer, Row> definitions;
	private final Map<Integer, List<Row>> definitionsByWorld;
	private final Map<Integer, Row> limitsByWorld;
	private final Map<Integer, List<Row>> limitsBySync;
	private final Map<Integer, Row> cooldowns;
	private final Map<Integer, Row> matches;
	private final Map<Integer, Row> teamMatches;
	private final Map<Integer, List<Row>> matchesByWorld;
	private final Map<Integer, List<Row>> matchesByNpc;
	private final Map<String, List<Row>> rewards;
	private final Map<Integer, Row> coverage;
	private final Map<Integer, Row> tournaments;
	private final Map<Integer, Row> tournamentsByLobbyWorld;
	private final Map<Integer, Row> tournamentsByStageWorld;
	private final Map<Integer, Row> tournamentsByMatchmaker;
	private final Map<Integer, Row> lunaDungeons;
	private final Map<Integer, Row> lunaDungeonsByWorld;
	private final Map<Integer, Row> lunaPrices;

	private RetailInstanceData(Map<Integer, Row> definitions, Map<Integer, List<Row>> definitionsByWorld,
		Map<Integer, Row> limitsByWorld, Map<Integer, List<Row>> limitsBySync, Map<Integer, Row> cooldowns, Map<Integer, Row> matches,
		Map<Integer, List<Row>> matchesByWorld, Map<Integer, List<Row>> matchesByNpc, Map<String, List<Row>> rewards,
			Map<Integer, Row> teamMatches, Map<Integer, Row> coverage, Map<Integer, Row> tournaments, Map<Integer, Row> tournamentsByLobbyWorld,
		Map<Integer, Row> tournamentsByStageWorld, Map<Integer, Row> tournamentsByMatchmaker,
		Map<Integer, Row> lunaDungeons, Map<Integer, Row> lunaDungeonsByWorld, Map<Integer, Row> lunaPrices) {
		this.definitions = immutableMap(definitions);
		Map<Integer, List<Row>> byWorld = new LinkedHashMap<>();
		definitionsByWorld.forEach((key, value) -> byWorld.put(key, List.copyOf(value)));
		this.definitionsByWorld = Collections.unmodifiableMap(byWorld);
		this.limitsByWorld = immutableMap(limitsByWorld);
		Map<Integer, List<Row>> bySync = new LinkedHashMap<>();
		limitsBySync.forEach((key, value) -> bySync.put(key, List.copyOf(value)));
		this.limitsBySync = Collections.unmodifiableMap(bySync);
		this.cooldowns = immutableMap(cooldowns);
		this.matches = immutableMap(matches);
		this.teamMatches = immutableMap(teamMatches);
		this.matchesByWorld = immutableLists(matchesByWorld);
		this.matchesByNpc = immutableLists(matchesByNpc);
		Map<String, List<Row>> rewardRows = new LinkedHashMap<>();
		rewards.forEach((key, value) -> rewardRows.put(key, List.copyOf(value)));
		this.rewards = Collections.unmodifiableMap(rewardRows);
		this.coverage = immutableMap(coverage);
		this.tournaments = immutableMap(tournaments);
		this.tournamentsByLobbyWorld = immutableMap(tournamentsByLobbyWorld);
		this.tournamentsByStageWorld = immutableMap(tournamentsByStageWorld);
		this.tournamentsByMatchmaker = immutableMap(tournamentsByMatchmaker);
		this.lunaDungeons = immutableMap(lunaDungeons);
		this.lunaDungeonsByWorld = immutableMap(lunaDungeonsByWorld);
		this.lunaPrices = immutableMap(lunaPrices);
	}

	public static RetailInstanceData load(File directory, File schemaFile) {
		try {
			SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
			schemaFactory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
			schemaFactory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
			schemaFactory.setProperty(ATTRIBUTE_LIMIT, "0");
			Schema schema = schemaFactory.newSchema(schemaFile);
			Map<String, Document> documents = new LinkedHashMap<>();
			for (String name : FILES) {
				File file = new File(directory, name);
				schema.newValidator().validate(new StreamSource(file));
				documents.put(name, parse(file));
			}

			Map<Integer, Row> definitions = rowsById(documents.get("definitions.xml"), "instance", "id");
			Map<Integer, List<Row>> definitionsByWorld = new LinkedHashMap<>();
			for (Row row : definitions.values()) {
				definitionsByWorld.computeIfAbsent(row.requiredInt("world_id"), ignored -> new ArrayList<>()).add(row);
			}
			Map<Integer, Row> limitsByWorld = rowsById(documents.get("limits.xml"), "instance_rule", "world_id");
			Map<Integer, List<Row>> limitsBySync = new LinkedHashMap<>();
			for (Row rule : limitsByWorld.values()) {
				int syncId = rule.intValue("coolt_sync_id", 0);
				if (syncId > 0) {
					limitsBySync.computeIfAbsent(syncId, ignored -> new ArrayList<>()).add(rule);
				}
			}
			Map<Integer, Row> cooldowns = rowsById(documents.get("limits.xml"), "cooldown", "id");
			Map<Integer, Row> matches = rowsById(documents.get("matchmaking.xml"), "match", "id");
			Map<Integer, Row> teamMatches = rowsById(documents.get("matchmaking.xml"), "team_match", "id");
			Map<Integer, List<Row>> matchesByWorld = new LinkedHashMap<>();
			Map<Integer, List<Row>> matchesByNpc = new LinkedHashMap<>();
			for (Row match : matches.values()) {
				Row definition = definitions.get(match.requiredInt("creation_id"));
				if (definition == null || definition.requiredInt("world_id") != match.requiredInt("world_id")) {
					throw new IllegalStateException("Invalid matchmaker definition " + match.value("id"));
				}
				matchesByWorld.computeIfAbsent(match.requiredInt("world_id"), ignored -> new ArrayList<>()).add(match);
				for (String value : match.values().getOrDefault("npc_ids", "").split(",")) {
					if (!value.isEmpty()) {
						matchesByNpc.computeIfAbsent(Integer.parseInt(value), ignored -> new ArrayList<>()).add(match);
					}
				}
			}
			for (Row teamMatch : teamMatches.values()) {
				Row definition = definitions.get(teamMatch.requiredInt("creation_id"));
				if (definition == null || definition.requiredInt("world_id") != teamMatch.requiredInt("world_id")) {
					throw new IllegalStateException("Invalid team matchmaker definition " + teamMatch.value("id"));
				}
			}

			Map<String, List<Row>> rewards = new LinkedHashMap<>();
			NodeList rewardTables = documents.get("rewards.xml").getDocumentElement().getElementsByTagName("table");
			for (int i = 0; i < rewardTables.getLength(); i++) {
				Element table = (Element) rewardTables.item(i);
				String name = table.getAttribute("name");
				if (rewards.put(name, childRows(table, "row")) != null) {
					throw new IllegalStateException("Duplicate reward table " + name);
				}
			}

			Map<Integer, Row> coverage = rowsById(documents.get("coverage.xml"), "world", "id");
			long standard = coverage.values().stream().filter(row -> "standard".equals(row.value("classification"))).count();
			long special = coverage.values().stream().filter(row -> "special".equals(row.value("classification"))).count();
			if (coverage.size() != 139 || standard != 134 || special != 5) {
				throw new IllegalStateException("Retail instance coverage is incomplete");
			}
			Element validation = (Element) documents.get("manifest.xml").getElementsByTagName("validation").item(0);
			if (validation == null || !"0".equals(validation.getAttribute("unresolved_references"))) {
				throw new IllegalStateException("Retail instance manifest contains unresolved references");
			}
			Map<String, Integer> behaviorCounts = new LinkedHashMap<>();
			for (Row row : coverage.values()) {
				String behavior = row.value("behavior");
				if (row.value("behavior_source").isEmpty() || !switch (behavior) {
					case "HANDLER", "RETAIL_AI_QUEST", "MATCHMAKER", "TOURNAMENT", "HOUSING", "EVENT", "DATA_ONLY",
						"EXCLUDED_NON_PRODUCTION" -> true;
					default -> false;
				}) {
					throw new IllegalStateException("Invalid retail instance behavior for world " + row.value("id"));
				}
				behaviorCounts.merge(behavior, 1, Integer::sum);
			}
			if (!Integer.toString(coverage.size()).equals(validation.getAttribute("behavior_total_worlds"))) {
				throw new IllegalStateException("Retail instance behavior closure is incomplete");
			}
			for (String behavior : List.of("HANDLER", "RETAIL_AI_QUEST", "MATCHMAKER", "TOURNAMENT", "HOUSING", "EVENT",
				"DATA_ONLY", "EXCLUDED_NON_PRODUCTION")) {
				String attribute = "behavior_" + behavior.toLowerCase() + "_worlds";
				if (!Integer.toString(behaviorCounts.getOrDefault(behavior, 0)).equals(validation.getAttribute(attribute))) {
					throw new IllegalStateException("Retail instance behavior manifest mismatch: " + behavior);
				}
			}
			Map<Integer, Row> tournaments = rowsById(rewards.get("instant_dungeon_tournament"), "tournament");
			Map<Integer, Row> tournamentsByLobbyWorld = new LinkedHashMap<>();
			Map<Integer, Row> tournamentsByStageWorld = new LinkedHashMap<>();
			Map<Integer, Row> tournamentsByMatchmaker = new LinkedHashMap<>();
			if (tournaments.size() != 5) {
				throw new IllegalStateException("Retail tournament definitions are incomplete");
			}
			for (Row tournament : tournaments.values()) {
				validateTournament(tournament, definitions, matches, tournamentsByLobbyWorld,
						tournamentsByStageWorld, tournamentsByMatchmaker);
			}
			if (tournamentsByMatchmaker.size() != 6) {
				throw new IllegalStateException("Retail tournament matchmaker closure is incomplete");
			}
			Map<Integer, Row> lunaPrices = rowsById(rewards.get("luna_cost"), "Luna price");
			Map<Integer, Row> lunaDungeons = rowsById(rewards.get("luna_indun"), "Luna dungeon");
			Map<Integer, Row> lunaDungeonsByWorld = new LinkedHashMap<>();
			if (lunaDungeons.size() != 2) {
				throw new IllegalStateException("Retail Luna dungeon definitions are incomplete");
			}
			for (Row dungeon : lunaDungeons.values()) {
				validateLunaDungeon(dungeon, definitions, lunaPrices, lunaDungeonsByWorld);
			}
			if (validation == null || !"2".equals(validation.getAttribute("luna_dungeon_mappings"))) {
				throw new IllegalStateException("Retail Luna dungeon manifest closure is incomplete");
			}
				return new RetailInstanceData(definitions, definitionsByWorld, limitsByWorld, limitsBySync, cooldowns, matches,
						matchesByWorld, matchesByNpc, rewards, teamMatches, coverage, tournaments, tournamentsByLobbyWorld,
					tournamentsByStageWorld, tournamentsByMatchmaker, lunaDungeons, lunaDungeonsByWorld, lunaPrices);
		} catch (Exception e) {
			throw new IllegalStateException("Failed to load retail instance data from " + directory.getPath(), e);
		}
	}

	public Row definition(int creationId) {
		return definitions.get(creationId);
	}

	public List<Row> definitionsForWorld(int worldId) {
		return definitionsByWorld.getOrDefault(worldId, List.of());
	}

	public Row limit(int worldId) {
		return limitsByWorld.get(worldId);
	}

	public Collection<Row> limits() {
		return limitsByWorld.values();
	}

	public List<Row> limitsForSync(int syncId) {
		return limitsBySync.getOrDefault(syncId, List.of());
	}

	public Row cooldown(int cooldownId) {
		return cooldowns.get(cooldownId);
	}

	public Collection<Row> cooldowns() {
		return cooldowns.values();
	}

	public Row match(int matchmakerId) {
		return matches.get(matchmakerId);
	}

	public Collection<Row> matches() {
		return matches.values();
	}

	public Row teamMatch(int matchmakerId) {
		return teamMatches.get(matchmakerId);
	}

	public Collection<Row> teamMatches() {
		return teamMatches.values();
	}

	public List<Row> matchesForWorld(int worldId) {
		return matchesByWorld.getOrDefault(worldId, List.of());
	}

	public List<Row> matchesForNpc(int npcId) {
		return matchesByNpc.getOrDefault(npcId, List.of());
	}

	public List<Row> rewards(String table) {
		return rewards.getOrDefault(table, List.of());
	}

	public Row coverage(int worldId) {
		return coverage.get(worldId);
	}

	public Row tournament(int tournamentId) {
		return tournaments.get(tournamentId);
	}

	public Row tournamentForLobbyWorld(int worldId) {
		return tournamentsByLobbyWorld.get(worldId);
	}

	public Row tournamentForStageWorld(int worldId) {
		return tournamentsByStageWorld.get(worldId);
	}

	public Row tournamentForMatchmaker(int matchmakerId) {
		return tournamentsByMatchmaker.get(matchmakerId);
	}

	public Collection<Row> tournaments() {
		return tournaments.values();
	}

	public Row lunaDungeon(int dungeonId) {
		return lunaDungeons.get(dungeonId);
	}

	public Row lunaDungeonForWorld(int worldId) {
		return lunaDungeonsByWorld.get(worldId);
	}

	public Collection<Row> lunaDungeons() {
		return lunaDungeons.values();
	}

	public Row lunaPrice(int priceId) {
		return lunaPrices.get(priceId);
	}

	public int definitionCount() {
		return definitions.size();
	}

	public int limitCount() {
		return limitsByWorld.size();
	}

	public int cooldownCount() {
		return cooldowns.size();
	}

	public int matchCount() {
		return matches.size();
	}

	public int teamMatchCount() {
		return teamMatches.size();
	}

	private static Document parse(File file) throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
		factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
		factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
		factory.setAttribute(ATTRIBUTE_LIMIT, "0");
		return factory.newDocumentBuilder().parse(file);
	}

	private static Map<Integer, Row> rowsById(Document document, String tag, String idAttribute) {
		Map<Integer, Row> result = new LinkedHashMap<>();
		NodeList nodes = document.getElementsByTagName(tag);
		for (int i = 0; i < nodes.getLength(); i++) {
			Row row = row((Element) nodes.item(i));
			int id = row.requiredInt(idAttribute);
			if (result.put(id, row) != null) {
				throw new IllegalStateException("Duplicate " + tag + " " + id);
			}
		}
		return result;
	}

	private static List<Row> childRows(Element parent, String tag) {
		List<Row> result = new ArrayList<>();
		NodeList nodes = parent.getChildNodes();
		for (int i = 0; i < nodes.getLength(); i++) {
			Node node = nodes.item(i);
			if (node instanceof Element element && tag.equals(element.getTagName())) {
				result.add(row(element));
			}
		}
		return result;
	}

	private static Map<Integer, Row> rowsById(List<Row> rows, String name) {
		Map<Integer, Row> result = new LinkedHashMap<>();
		for (Row row : rows) {
			int id = row.requiredInt("id");
			if (result.put(id, row) != null) {
				throw new IllegalStateException("Duplicate " + name + " " + id);
			}
		}
		return result;
	}

	private static void validateTournament(Row tournament, Map<Integer, Row> definitions, Map<Integer, Row> matches,
			Map<Integer, Row> byLobbyWorld, Map<Integer, Row> byStageWorld, Map<Integer, Row> byMatchmaker) {
		int tournamentId = tournament.requiredInt("id");
		int lobbyCreationId = tournament.requiredInt("lobby_creation_id");
		int lobbyWorldId = tournament.requiredInt("lobby_world_id");
		int stageCreationId = tournament.requiredInt("stage_creation_id");
		int stageWorldId = tournament.requiredInt("stage_world_id");
		validateTournamentCreation(definitions.get(lobbyCreationId), tournamentId, "lobby", lobbyWorldId);
		validateTournamentCreation(definitions.get(stageCreationId), tournamentId, "stage", stageWorldId);
		for (String field : List.of("lobby_start_01", "lobby_start_02", "stage_start_01", "stage_start_02")) {
			validatePoints(tournament.value(field), tournamentId, field);
		}
		if (byLobbyWorld.put(lobbyWorldId, tournament) != null || byStageWorld.put(stageWorldId, tournament) != null) {
			throw new IllegalStateException("Duplicate retail tournament world for " + tournamentId);
		}
		int bracketSize = 0;
		for (String value : tournament.value("matchmaker_ids").split(",")) {
			int matchmakerId = Integer.parseInt(value);
			Row match = matches.get(matchmakerId);
			if (match == null || match.requiredInt("tournament_id") != tournamentId
					|| match.requiredInt("creation_id") != lobbyCreationId
					|| match.requiredInt("world_id") != lobbyWorldId
					|| !"TOURNAMENT".equals(match.value("handler"))) {
				throw new IllegalStateException("Invalid retail tournament matchmaker " + matchmakerId);
			}
			if (byMatchmaker.put(matchmakerId, tournament) != null) {
				throw new IllegalStateException("Duplicate retail tournament matchmaker " + matchmakerId);
			}
			bracketSize = Math.max(bracketSize, match.requiredInt("num_matchside"));
		}
		if (bracketSize < 2 || Integer.bitCount(bracketSize) != 1) {
			throw new IllegalStateException("Invalid retail tournament bracket size for " + tournamentId);
		}
		int requiredRounds = Integer.numberOfTrailingZeros(bracketSize);
		if (tournament.requiredInt("round_count") < requiredRounds) {
			throw new IllegalStateException("Incomplete retail tournament rounds for " + tournamentId);
		}
		for (int round = 1; round <= requiredRounds; round++) {
			if (tournament.requiredInt("round_" + round + "_win_kill_point") <= 0) {
				throw new IllegalStateException("Missing retail tournament win threshold " + tournamentId + "/" + round);
			}
			for (int item = 1; item <= 3; item++) {
				String prefix = "round_" + round + "_item" + item;
				if (tournament.value(prefix + "_name") != null && tournament.requiredInt(prefix + "_id") <= 0) {
					throw new IllegalStateException("Unmapped retail tournament item " + tournamentId + "/" + round);
				}
			}
		}
	}

	private static void validateTournamentCreation(Row definition, int tournamentId, String role, int worldId) {
		if (definition == null || definition.requiredInt("world_id") != worldId
				|| definition.requiredInt("tournament_id") != tournamentId || !role.equals(definition.value("tournament_role"))) {
			throw new IllegalStateException("Invalid retail tournament " + role + " creation for " + tournamentId);
		}
	}

	private static void validateLunaDungeon(Row dungeon, Map<Integer, Row> definitions, Map<Integer, Row> prices,
			Map<Integer, Row> byWorld) {
		int dungeonId = dungeon.requiredInt("id");
		int creationId = dungeon.requiredInt("creation_id");
		int worldId = dungeon.requiredInt("world_id");
		Row definition = definitions.get(creationId);
		if (definition == null || definition.requiredInt("world_id") != worldId
				|| !definition.value("type").contains("PRIVATE")) {
			throw new IllegalStateException("Invalid retail Luna creation for dungeon " + dungeonId);
		}
		Row price = prices.get(dungeon.requiredInt("luna_price_id"));
		if (price == null || price.requiredInt("free_turn") < 0 || price.requiredInt("price_max_count") < 0
				|| !("Daily".equals(price.value("reset_type")) || "Weekly".equals(price.value("reset_type")))) {
			throw new IllegalStateException("Invalid retail Luna price for dungeon " + dungeonId);
		}
		for (int number = 1; number <= price.requiredInt("price_max_count"); number++) {
			if (price.requiredInt("price%02d".formatted(number)) < 0) {
				throw new IllegalStateException("Invalid retail Luna price step for dungeon " + dungeonId);
			}
		}
		validatePoints(dungeon.value("start_point"), dungeonId, "start_point");
		if (dungeon.intValue("active", 0) != 1 || dungeon.intValue("price_ratio", -1) < 0
				|| dungeon.intValue("price_ratio", -1) > 100 || byWorld.put(worldId, dungeon) != null) {
			throw new IllegalStateException("Invalid retail Luna dungeon " + dungeonId);
		}
		for (String day : List.of("sun", "mon", "tue", "wed", "thu", "fri", "sat")) {
			for (String half : List.of("am", "pm")) {
				for (int hour = 0; hour < 12; hour++) {
					int minute = dungeon.requiredInt(day + "_" + half + hour);
					if (minute < 0 || minute > 60) {
						throw new IllegalStateException("Invalid retail Luna schedule for dungeon " + dungeonId);
					}
				}
			}
		}
	}

	private static void validatePoints(String value, int tournamentId, String field) {
		if (value == null || value.isBlank()) {
			throw new IllegalStateException("Missing retail tournament points " + tournamentId + "/" + field);
		}
		for (String point : value.split(";")) {
			String[] coordinates = point.split(",");
			if (coordinates.length != 4) {
				throw new IllegalStateException("Invalid retail tournament point " + tournamentId + "/" + field);
			}
			for (String coordinate : coordinates) {
				Float.parseFloat(coordinate);
			}
		}
	}

	private static Row row(Element element) {
		Map<String, String> values = new LinkedHashMap<>();
		NamedNodeMap attributes = element.getAttributes();
		for (int i = 0; i < attributes.getLength(); i++) {
			Node attribute = attributes.item(i);
			if (!attribute.getNodeName().startsWith("xmlns")) {
				values.put(attribute.getNodeName(), attribute.getNodeValue());
			}
		}
		return new Row(values);
	}

	private static <K, V> Map<K, V> immutableMap(Map<K, V> values) {
		return Collections.unmodifiableMap(new LinkedHashMap<>(values));
	}

	private static <K, V> Map<K, List<V>> immutableLists(Map<K, List<V>> values) {
		Map<K, List<V>> result = new LinkedHashMap<>();
		values.forEach((key, value) -> result.put(key, List.copyOf(value)));
		return Collections.unmodifiableMap(result);
	}

	public record Row(Map<String, String> values) {

		public Row {
			values = Collections.unmodifiableMap(new LinkedHashMap<>(values));
		}

		public String value(String name) {
			return values.get(name);
		}

		public int intValue(String name, int defaultValue) {
			String value = values.get(name);
			return value == null || value.isEmpty() ? defaultValue : Integer.parseInt(value);
		}

		public long longValue(String name, long defaultValue) {
			String value = values.get(name);
			return value == null || value.isEmpty() ? defaultValue : Long.parseLong(value);
		}

		public int requiredInt(String name) {
			String value = values.get(name);
			if (value == null || value.isEmpty()) {
				throw new IllegalStateException("Missing required integer " + name);
			}
			return Integer.parseInt(value);
		}

		public boolean booleanValue(String name) {
			return Boolean.parseBoolean(values.getOrDefault(name, "false"));
		}
	}
}
