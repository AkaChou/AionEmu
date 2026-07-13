package com.aionemu.gameserver.dataholders;

import com.aionemu.gameserver.model.geometry.Area;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** 真端 NPC AI Pattern 及 NPC 到 Pattern 的映射。 */
public final class RetailAiData {

	private final Map<String, Pattern> patterns;
	private final Map<Integer, Npc> npcs;
	private final Map<String, Integer> npcIdsByName;
	private final Map<String, Integer> stringIdsByName;
	private final Map<Integer, String> skillCategories;
	private final Map<Integer, NpcScore> npcScores;
	private final Map<Integer, Map<String, Area>> areasByWorld;
	private final Set<String> areaNames;
	private final Map<Integer, List<ResurrectArea>> resurrectAreasByWorld;
	private final Map<Integer, List<QuestArea>> questAreasByWorld;
	private final Map<Integer, List<LimitArea>> limitAreasByWorld;
	private final Map<Integer, List<GroupControlArea>> groupControlAreasByWorld;
	private final Map<Integer, List<GroupController>> groupControllersByWorld;
	private final Map<Integer, Map<Integer, List<Area>>> skillAreasByWorld;
	private final Set<Integer> skillAreaIds;
	private final Map<Integer, Map<String, List<LocationAliasPoint>>> locationAliasesByWorld;
	private final Map<Integer, List<ConditionSpawn>> conditionSpawnsByWorld;
	private final Map<Integer, Set<String>> conditionVariablesByWorld;
	private final Map<String, Integer> conditionWorldIdsByName;
	private final Map<Integer, DirectPortal> directPortals;
	private final Map<Integer, Map<Integer, List<ConditionSpawnNpc>>> sensorySpawnsByWorld;
	private final Map<Integer, List<NpcParty>> npcPartiesByWorld;
	private final Map<Integer, Map<String, Map<Integer, DynamicArea>>> dynamicAreasByWorld;

	public RetailAiData(Map<String, Pattern> patterns, Map<Integer, Npc> npcs, Map<String, Integer> stringIdsByName,
			Map<Integer, String> skillCategories, Map<Integer, NpcScore> npcScores,
			Map<Integer, Map<String, Area>> areasByWorld,
			Map<Integer, List<ResurrectArea>> resurrectAreasByWorld,
			Map<Integer, List<QuestArea>> questAreasByWorld,
			Map<Integer, List<LimitArea>> limitAreasByWorld,
			Map<Integer, List<GroupControlArea>> groupControlAreasByWorld,
			Map<Integer, List<GroupController>> groupControllersByWorld,
			Map<Integer, Map<Integer, List<Area>>> skillAreasByWorld,
			Map<Integer, Map<String, List<LocationAliasPoint>>> locationAliasesByWorld,
			Map<Integer, List<ConditionSpawn>> conditionSpawnsByWorld,
			Map<Integer, Set<String>> conditionVariablesByWorld,
			Map<String, Integer> conditionWorldIdsByName,
			Map<Integer, DirectPortal> directPortals) {
		this(patterns, npcs, stringIdsByName, skillCategories, npcScores, areasByWorld, resurrectAreasByWorld,
			questAreasByWorld, limitAreasByWorld, groupControlAreasByWorld, groupControllersByWorld, skillAreasByWorld,
			locationAliasesByWorld, conditionSpawnsByWorld, conditionVariablesByWorld, conditionWorldIdsByName,
			directPortals, Map.of(), Map.of());
	}

	public RetailAiData(Map<String, Pattern> patterns, Map<Integer, Npc> npcs, Map<String, Integer> stringIdsByName,
			Map<Integer, String> skillCategories,
			Map<Integer, NpcScore> npcScores,
			Map<Integer, Map<String, Area>> areasByWorld,
			Map<Integer, List<ResurrectArea>> resurrectAreasByWorld,
			Map<Integer, List<QuestArea>> questAreasByWorld,
			Map<Integer, List<LimitArea>> limitAreasByWorld,
			Map<Integer, List<GroupControlArea>> groupControlAreasByWorld,
			Map<Integer, List<GroupController>> groupControllersByWorld,
			Map<Integer, Map<Integer, List<Area>>> skillAreasByWorld,
			Map<Integer, Map<String, List<LocationAliasPoint>>> locationAliasesByWorld,
			Map<Integer, List<ConditionSpawn>> conditionSpawnsByWorld,
			Map<Integer, Set<String>> conditionVariablesByWorld,
			Map<String, Integer> conditionWorldIdsByName,
			Map<Integer, DirectPortal> directPortals,
			Map<Integer, List<NpcParty>> npcPartiesByWorld) {
		this(patterns, npcs, stringIdsByName, skillCategories, npcScores, areasByWorld, resurrectAreasByWorld,
			questAreasByWorld, limitAreasByWorld, groupControlAreasByWorld, groupControllersByWorld, skillAreasByWorld,
			locationAliasesByWorld, conditionSpawnsByWorld, conditionVariablesByWorld, conditionWorldIdsByName,
			directPortals, npcPartiesByWorld, Map.of());
	}

	public RetailAiData(Map<String, Pattern> patterns, Map<Integer, Npc> npcs, Map<String, Integer> stringIdsByName,
			Map<Integer, String> skillCategories, Map<Integer, NpcScore> npcScores,
			Map<Integer, Map<String, Area>> areasByWorld, Map<Integer, List<ResurrectArea>> resurrectAreasByWorld,
			Map<Integer, List<QuestArea>> questAreasByWorld, Map<Integer, List<LimitArea>> limitAreasByWorld,
			Map<Integer, List<GroupControlArea>> groupControlAreasByWorld,
			Map<Integer, List<GroupController>> groupControllersByWorld,
			Map<Integer, Map<Integer, List<Area>>> skillAreasByWorld,
			Map<Integer, Map<String, List<LocationAliasPoint>>> locationAliasesByWorld,
			Map<Integer, List<ConditionSpawn>> conditionSpawnsByWorld, Map<Integer, Set<String>> conditionVariablesByWorld,
			Map<String, Integer> conditionWorldIdsByName, Map<Integer, DirectPortal> directPortals,
			Map<Integer, List<NpcParty>> npcPartiesByWorld,
			Map<Integer, Map<String, Map<Integer, DynamicArea>>> dynamicAreasByWorld) {
		this.patterns = Map.copyOf(patterns);
		this.npcs = Map.copyOf(npcs);
		this.stringIdsByName = Map.copyOf(stringIdsByName);
		this.skillCategories = Map.copyOf(skillCategories);
		this.npcScores = Map.copyOf(npcScores);
		Map<String, Integer> names = new HashMap<>();
		for (Npc npc : npcs.values()) {
			if (npc.name() != null && !npc.name().isBlank()) {
				names.putIfAbsent(npc.name().toLowerCase(), npc.id());
			}
		}
		this.npcIdsByName = Map.copyOf(names);
		Map<Integer, Map<String, Area>> areas = new HashMap<>();
		Set<String> areaNames = new HashSet<>();
		areasByWorld.forEach((worldId, worldAreas) -> {
			areas.put(worldId, Map.copyOf(worldAreas));
			areaNames.addAll(worldAreas.keySet());
		});
		this.areasByWorld = Map.copyOf(areas);
		this.areaNames = Set.copyOf(areaNames);
		Map<Integer, List<ResurrectArea>> resurrectAreas = new HashMap<>();
		resurrectAreasByWorld.forEach((worldId, entries) -> resurrectAreas.put(worldId, List.copyOf(entries)));
		this.resurrectAreasByWorld = Map.copyOf(resurrectAreas);
		Map<Integer, List<QuestArea>> questAreas = new HashMap<>();
		questAreasByWorld.forEach((worldId, entries) -> questAreas.put(worldId, List.copyOf(entries)));
		this.questAreasByWorld = Map.copyOf(questAreas);
		Map<Integer, List<LimitArea>> limitAreas = new HashMap<>();
		limitAreasByWorld.forEach((worldId, entries) -> limitAreas.put(worldId, List.copyOf(entries)));
		this.limitAreasByWorld = Map.copyOf(limitAreas);
		Map<Integer, List<GroupControlArea>> groupControlAreas = new HashMap<>();
		groupControlAreasByWorld.forEach((worldId, entries) -> groupControlAreas.put(worldId, List.copyOf(entries)));
		this.groupControlAreasByWorld = Map.copyOf(groupControlAreas);
		Map<Integer, List<GroupController>> groupControllers = new HashMap<>();
		groupControllersByWorld.forEach((worldId, entries) -> groupControllers.put(worldId, List.copyOf(entries)));
		this.groupControllersByWorld = Map.copyOf(groupControllers);
		Map<Integer, Map<Integer, List<Area>>> skillAreas = new HashMap<>();
		Set<Integer> skillAreaIds = new HashSet<>();
		skillAreasByWorld.forEach((worldId, worldAreas) -> {
			Map<Integer, List<Area>> copied = new HashMap<>();
			worldAreas.forEach((id, entries) -> copied.put(id, List.copyOf(entries)));
			skillAreas.put(worldId, Map.copyOf(copied));
			skillAreaIds.addAll(worldAreas.keySet());
		});
		this.skillAreasByWorld = Map.copyOf(skillAreas);
		this.skillAreaIds = Set.copyOf(skillAreaIds);
		Map<Integer, Map<String, List<LocationAliasPoint>>> aliases = new HashMap<>();
		locationAliasesByWorld.forEach((worldId, worldAliases) -> aliases.put(worldId, Map.copyOf(worldAliases)));
		this.locationAliasesByWorld = Map.copyOf(aliases);
		validateGroupControllers();
		this.conditionSpawnsByWorld = Map.copyOf(conditionSpawnsByWorld);
		this.conditionVariablesByWorld = Map.copyOf(conditionVariablesByWorld);
		this.conditionWorldIdsByName = Map.copyOf(conditionWorldIdsByName);
		this.directPortals = Map.copyOf(directPortals);
		Map<Integer, List<NpcParty>> parties = new HashMap<>();
		npcPartiesByWorld.forEach((worldId, entries) -> parties.put(worldId, List.copyOf(entries)));
		this.npcPartiesByWorld = Map.copyOf(parties);
		Map<Integer, Map<String, Map<Integer, DynamicArea>>> dynamicAreas = new HashMap<>();
		dynamicAreasByWorld.forEach((worldId, types) -> {
			Map<String, Map<Integer, DynamicArea>> copied = new HashMap<>();
			types.forEach((type, entries) -> copied.put(type, Map.copyOf(entries)));
			dynamicAreas.put(worldId, Map.copyOf(copied));
		});
		this.dynamicAreasByWorld = Map.copyOf(dynamicAreas);
		Map<Integer, Map<Integer, List<ConditionSpawnNpc>>> sensorySpawns = new HashMap<>();
		conditionSpawnsByWorld.forEach((worldId, conditions) -> conditions.stream()
			.flatMap(condition -> condition.groups().stream())
			.flatMap(group -> group.slots().stream())
			.flatMap(List::stream)
			.flatMap(choice -> choice.members().stream())
			.filter(npc -> npc.sensoryArea() != null)
			.forEach(npc -> sensorySpawns.computeIfAbsent(worldId, ignored -> new HashMap<>())
				.computeIfAbsent(npc.id(), ignored -> new java.util.ArrayList<>()).add(npc)));
		Map<Integer, Map<Integer, List<ConditionSpawnNpc>>> immutableSensorySpawns = new HashMap<>();
		sensorySpawns.forEach((worldId, npcEntries) -> {
			Map<Integer, List<ConditionSpawnNpc>> copied = new HashMap<>();
			npcEntries.forEach((npcId, entries) -> copied.put(npcId, List.copyOf(entries)));
			immutableSensorySpawns.put(worldId, Map.copyOf(copied));
		});
		this.sensorySpawnsByWorld = Map.copyOf(immutableSensorySpawns);
	}

	private void validateGroupControllers() {
		groupControllersByWorld.forEach((worldId, controllers) -> {
			Set<String> areas = new HashSet<>();
			groupControlAreasByWorld.getOrDefault(worldId, List.of()).forEach(
				area -> areas.add(area.name().toLowerCase(Locale.ROOT)));
			for (GroupController controller : controllers) {
				if (!areas.contains(controller.area1().toLowerCase(Locale.ROOT))
						|| !areas.contains(controller.area2().toLowerCase(Locale.ROOT))) {
					throw new IllegalArgumentException("Incomplete GROUPCTRL areas: " + controller.name());
				}
				if (controller.exitWorldId() > 0) {
					List<LocationAliasPoint> points = findLocationAlias(controller.exitWorldId(), controller.exitAlias());
					if (controller.exitAlias().isBlank() || points == null || points.isEmpty()) {
						throw new IllegalArgumentException("Incomplete GROUPCTRL exit alias: " + controller.name());
					}
				}
			}
		});
	}

	public Pattern getPattern(int npcId) {
		Npc npc = npcs.get(npcId);
		if (npc == null) {
			return null;
		}
		String name = npc.aiName().toLowerCase();
		Pattern pattern = patterns.get(name);
		return pattern != null ? pattern : patterns.get(name.replaceFirst("_ver\\d+$", ""));
	}

	public Npc getNpc(int npcId) {
		return npcs.get(npcId);
	}

	public Integer findNpcId(String name) {
		return name == null ? null : npcIdsByName.get(name.toLowerCase());
	}

	public Integer findStringId(String name) {
		return name == null ? null : stringIdsByName.get(name.toLowerCase());
	}

	public String getSkillCategory(int skillId) {
		return skillCategories.get(skillId);
	}

	public NpcScore getNpcScore(int npcId) {
		return npcScores.get(npcId);
	}

	public int npcScoreCount() {
		return npcScores.size();
	}

	public int skillCategoryCount() {
		return skillCategories.size();
	}

	public Area findArea(int worldId, String name) {
		Map<String, Area> areas = areasByWorld.get(worldId);
		return areas == null || name == null ? null : areas.get(name.toLowerCase());
	}

	public boolean hasArea(String name) {
		return name != null && areaNames.contains(name.toLowerCase());
	}

	public List<ResurrectArea> getResurrectAreas(int worldId) {
		return resurrectAreasByWorld.getOrDefault(worldId, List.of());
	}

	public boolean hasResurrectArea(int worldId, String prefix) {
		return prefix != null && getResurrectAreas(worldId).stream()
			.anyMatch(area -> area.name().regionMatches(true, 0, prefix, 0, prefix.length()));
	}

	public List<QuestArea> findQuestAreas(int worldId, String prefix) {
		if (prefix == null) {
			return List.of();
		}
		return questAreasByWorld.getOrDefault(worldId, List.of()).stream()
			.filter(area -> area.name().regionMatches(true, 0, prefix, 0, prefix.length())).toList();
	}

	public boolean hasQuestArea(int worldId, String prefix) {
		return !findQuestAreas(worldId, prefix).isEmpty();
	}

	public List<QuestArea> getQuestAreas(int worldId) {
		return questAreasByWorld.getOrDefault(worldId, List.of());
	}

	public List<LimitArea> findLimitAreas(int worldId, String prefix) {
		if (prefix == null) {
			return List.of();
		}
		return limitAreasByWorld.getOrDefault(worldId, List.of()).stream()
			.filter(area -> area.name().regionMatches(true, 0, prefix, 0, prefix.length())).toList();
	}

	public List<LimitArea> getLimitAreas(int worldId) {
		return limitAreasByWorld.getOrDefault(worldId, List.of());
	}

	public List<GroupControlArea> findGroupControlAreas(int worldId, String prefix) {
		if (prefix == null) {
			return List.of();
		}
		return groupControlAreasByWorld.getOrDefault(worldId, List.of()).stream()
			.filter(area -> area.name().regionMatches(true, 0, prefix, 0, prefix.length())).toList();
	}

	public List<GroupController> getGroupControllers(int worldId) {
		return groupControllersByWorld.getOrDefault(worldId, List.of());
	}

	public List<Area> findSkillAreas(int worldId, int id) {
		Map<Integer, List<Area>> areas = skillAreasByWorld.get(worldId);
		return areas == null ? List.of() : areas.getOrDefault(id, List.of());
	}

	public boolean hasSkillArea(int id) {
		return skillAreaIds.contains(id);
	}

	public List<LocationAliasPoint> findLocationAlias(int worldId, String name) {
		Map<String, List<LocationAliasPoint>> aliases = locationAliasesByWorld.get(worldId);
		return aliases == null || name == null ? null : aliases.get(name.toLowerCase());
	}

	public List<ConditionSpawn> getConditionSpawns(int worldId) {
		return conditionSpawnsByWorld.getOrDefault(worldId, List.of());
	}

	public boolean supportsConditionVariable(int worldId, String name) {
		Set<String> variables = conditionVariablesByWorld.get(worldId);
		return variables != null && name != null && variables.contains(name.toLowerCase());
	}

	public Integer findConditionWorldId(String name) {
		return name == null ? null : conditionWorldIdsByName.get(name.toLowerCase());
	}

	public int conditionSpawnCount() {
		return conditionSpawnsByWorld.values().stream().mapToInt(List::size).sum();
	}

	public Area findSensoryArea(int worldId, int npcId, float x, float y, float z) {
		Map<Integer, List<ConditionSpawnNpc>> npcs = sensorySpawnsByWorld.get(worldId);
		if (npcs == null) {
			return null;
		}
		return npcs.getOrDefault(npcId, List.of()).stream()
			.filter(npc -> Math.abs(npc.x() - x) < 0.01f && Math.abs(npc.y() - y) < 0.01f
				&& Math.abs(npc.z() - z) < 0.01f)
			.map(ConditionSpawnNpc::sensoryArea)
			.findFirst().orElse(null);
	}

	public int sensoryAreaCount() {
		return sensorySpawnsByWorld.values().stream().flatMap(npcs -> npcs.values().stream())
			.mapToInt(List::size).sum();
	}

	public DirectPortal getDirectPortal(int id) {
		return directPortals.get(id);
	}

	public boolean hasDirectPortal(int id) {
		return directPortals.containsKey(id);
	}

	public int directPortalCount() {
		return directPortals.size();
	}

	public List<NpcParty> getNpcParties(int worldId) {
		return npcPartiesByWorld.getOrDefault(worldId, List.of());
	}

	public int npcPartyCount() {
		return npcPartiesByWorld.values().stream().mapToInt(List::size).sum();
	}

	public int npcPartyMemberCount() {
		return npcPartiesByWorld.values().stream().flatMap(List::stream)
			.mapToInt(party -> party.members().size()).sum();
	}

	public DynamicArea getDynamicArea(int worldId, String type, int id) {
		Map<String, Map<Integer, DynamicArea>> types = dynamicAreasByWorld.get(worldId);
		return types == null ? null : types.getOrDefault(type, Map.of()).get(id);
	}

	public List<DynamicArea> getDynamicAreas(int worldId) {
		return dynamicAreasByWorld.getOrDefault(worldId, Map.of()).values().stream()
			.flatMap(areas -> areas.values().stream()).toList();
	}

	public int dynamicAreaCount() {
		return dynamicAreasByWorld.values().stream().flatMap(types -> types.values().stream())
			.mapToInt(Map::size).sum();
	}

	public int patternCount() {
		return patterns.size();
	}

	public Iterable<Pattern> patterns() {
		return patterns.values();
	}

	public int npcCount() {
		return npcs.size();
	}

	public int stringCount() {
		return stringIdsByName.size();
	}

	public int areaCount() {
		return areasByWorld.values().stream().mapToInt(Map::size).sum();
	}

	public int skillAreaCount() {
		return skillAreasByWorld.values().stream()
			.flatMap(areas -> areas.values().stream()).mapToInt(List::size).sum();
	}

	public int resurrectAreaCount() {
		return resurrectAreasByWorld.values().stream().mapToInt(List::size).sum();
	}

	public int questAreaCount() {
		return questAreasByWorld.values().stream().mapToInt(List::size).sum();
	}

	public int limitAreaCount() {
		return limitAreasByWorld.values().stream().mapToInt(List::size).sum();
	}

	public int groupControlAreaCount() {
		return groupControlAreasByWorld.values().stream().mapToInt(List::size).sum();
	}

	public int groupControllerCount() {
		return groupControllersByWorld.values().stream().mapToInt(List::size).sum();
	}

	public int locationAliasCount() {
		return locationAliasesByWorld.values().stream().mapToInt(Map::size).sum();
	}

	public enum PathfindFailReaction {
		RETURN_TO_SP,
		PULL_TARGET,
		ABANDON_TARGET
	}

	public record Npc(int id, String name, String aiName, float sensoryRange, float sensoryRangeShort,
			float sensoryAngle, int talkDelay, String maxChaseTime, PathfindFailReaction pathfindFailReaction,
			String returnMoveType, int returnSpeedPercent, int returnSensoryPercent) {
	}

	public record NpcScore(int id, int npcId, String name, String nameId, int scoreApplyType,
			int equalizingScore, int value) {
	}

	public record LocationAliasPoint(float x, float y, float z, float direction) {
	}

	public record ResurrectArea(String name, String locationAlias, int race, String tribe, Area area,
			List<LocationAliasPoint> destinations) {
		public ResurrectArea {
			destinations = List.copyOf(destinations);
		}
	}

	public record QuestArea(String name, List<Integer> questIds, Area area) {
		public QuestArea {
			questIds = List.copyOf(questIds);
		}
	}

	public record LimitArea(String name, boolean dynamic, String noBind, boolean noRecall, String noPark,
			int noParkReenterInterval, boolean noRide, boolean noShop, int priority, Area area) {
	}

	public record GroupControlArea(String name, Area area) {
	}

	public record GroupController(int id, String name, int worldId, int type, String area1, String area2,
			String race, int controlTargetType, int exitWorldId, String exitWorld, String exitAlias) {
	}

	public record DirectPortal(int id, String name, int time, int count, int minLevel, int maxLevel,
			String needItem, int groupId, int invadeType,
			DirectPortalEndpoint start, DirectPortalEndpoint destination) {
	}

	public record DirectPortalEndpoint(int worldId, int npcId, List<DirectPortalGroup> groups) {
		public DirectPortalEndpoint {
			groups = List.copyOf(groups);
		}
	}

	public record DirectPortalGroup(int weight, List<DirectPortalPoint> points) {
		public DirectPortalGroup {
			points = List.copyOf(points);
		}
	}

	public record DirectPortalPoint(float x, float y, float z, float direction) {
	}

	public record NpcParty(String token, List<NpcPartyMember> members) {
		public NpcParty {
			members = List.copyOf(members);
		}
	}

	public record NpcPartyMember(int id, float x, float y, float z) {
	}

	public record DynamicArea(int worldId, String worldName, String type, int id, String name,
			boolean aiPattern, int startTime, int endTime, int lifeTime, boolean alwaysEnabled) {
	}

	public record Pattern(String name, Map<String, List<Rule>> events) {
		public Pattern {
			events = Map.copyOf(events);
		}

		public List<Rule> event(String name) {
			return events.getOrDefault(name, List.of());
		}
	}

	public record Rule(int priority, String category, List<Operation> conditions, List<Operation> actions) {
		public Rule {
			conditions = List.copyOf(conditions);
			actions = List.copyOf(actions);
		}
	}

	public record Operation(String type, Map<String, String> values) {
		public Operation {
			values = Map.copyOf(values);
		}

		public String value(String name) {
			return values.get(name);
		}
	}

	public record ConditionSpawn(int id, String expression, boolean despawnAtOther, String groupMode,
			List<ConditionSpawnGroup> groups) {
		public ConditionSpawn {
			groups = List.copyOf(groups);
		}
	}

	public record ConditionSpawnGroup(int probability, List<List<ConditionSpawnChoice>> slots) {
		public ConditionSpawnGroup {
			slots = slots.stream().map(List::copyOf).toList();
		}
	}

	public record ConditionSpawnChoice(int probability, String partyId, List<ConditionSpawnNpc> members) {
		public ConditionSpawnChoice {
			members = List.copyOf(members);
		}
	}

	public record ConditionSpawnNpc(int id, float x, float y, float z, int heading,
				int initialDelay, int initialDelayExtra, String walkerId, Area sensoryArea) {
	}
}
