package com.aionemu.gameserver.dataholders.loadingutils;

import com.aionemu.gameserver.dataholders.RetailAiData;
import com.aionemu.gameserver.dataholders.RetailAiData.ConditionSpawn;
import com.aionemu.gameserver.dataholders.RetailAiData.ConditionSpawnChoice;
import com.aionemu.gameserver.dataholders.RetailAiData.ConditionSpawnGroup;
import com.aionemu.gameserver.dataholders.RetailAiData.ConditionSpawnNpc;
import com.aionemu.gameserver.dataholders.RetailAiData.DirectPortal;
import com.aionemu.gameserver.dataholders.RetailAiData.DirectPortalEndpoint;
import com.aionemu.gameserver.dataholders.RetailAiData.DirectPortalGroup;
import com.aionemu.gameserver.dataholders.RetailAiData.DirectPortalPoint;
import com.aionemu.gameserver.dataholders.RetailAiData.DynamicArea;
import com.aionemu.gameserver.dataholders.RetailAiData.GroupControlArea;
import com.aionemu.gameserver.dataholders.RetailAiData.GroupController;
import com.aionemu.gameserver.dataholders.RetailAiData.LocationAliasPoint;
import com.aionemu.gameserver.dataholders.RetailAiData.LimitArea;
import com.aionemu.gameserver.dataholders.RetailAiData.Npc;
import com.aionemu.gameserver.dataholders.RetailAiData.NpcScore;
import com.aionemu.gameserver.dataholders.RetailAiData.NpcParty;
import com.aionemu.gameserver.dataholders.RetailAiData.NpcPartyMember;
import com.aionemu.gameserver.dataholders.RetailAiData.Operation;
import com.aionemu.gameserver.dataholders.RetailAiData.PathfindFailReaction;
import com.aionemu.gameserver.dataholders.RetailAiData.Pattern;
import com.aionemu.gameserver.dataholders.RetailAiData.Rule;
import com.aionemu.gameserver.dataholders.RetailAiData.ResurrectArea;
import com.aionemu.gameserver.dataholders.RetailAiData.QuestArea;
import com.aionemu.gameserver.model.geometry.Area;
import com.aionemu.gameserver.model.geometry.PolyArea;
import com.aionemu.gameserver.model.templates.zone.Point2D;
import com.aionemu.gameserver.world.zone.ZoneName;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

final class RetailAiDefinitionLoader {

	static RetailAiData load(File patternsDirectory, File mappingsFile, File stringsFile, File areasFile) {
		return load(patternsDirectory, mappingsFile, stringsFile, areasFile, null, null, null, null, null, null, null);
	}

	static RetailAiData load(File patternsDirectory, File mappingsFile, File stringsFile, File areasFile,
			File conditionSpawnsFile) {
		return load(patternsDirectory, mappingsFile, stringsFile, areasFile, conditionSpawnsFile, null, null, null, null, null,
			null);
	}

	static RetailAiData load(File patternsDirectory, File mappingsFile, File stringsFile, File areasFile,
			File conditionSpawnsFile, File skillCategoriesFile) {
		return load(patternsDirectory, mappingsFile, stringsFile, areasFile, conditionSpawnsFile, skillCategoriesFile,
			null, null, null, null, null);
	}

	static RetailAiData load(File patternsDirectory, File mappingsFile, File stringsFile, File areasFile,
			File conditionSpawnsFile, File skillCategoriesFile, File locationAliasesFile) {
		return load(patternsDirectory, mappingsFile, stringsFile, areasFile, conditionSpawnsFile, skillCategoriesFile,
			locationAliasesFile, null, null, null, null);
	}

	static RetailAiData load(File patternsDirectory, File mappingsFile, File stringsFile, File areasFile,
			File conditionSpawnsFile, File skillCategoriesFile, File locationAliasesFile, File directPortalsFile,
			File npcScoresFile, File groupControllersFile) {
		return load(patternsDirectory, mappingsFile, stringsFile, areasFile, conditionSpawnsFile, skillCategoriesFile,
			locationAliasesFile, directPortalsFile, npcScoresFile, groupControllersFile, null);
	}

	static RetailAiData load(File patternsDirectory, File mappingsFile, File stringsFile, File areasFile,
			File conditionSpawnsFile, File skillCategoriesFile, File locationAliasesFile, File directPortalsFile,
			File npcScoresFile, File groupControllersFile, File npcPartiesFile) {
		return load(patternsDirectory, mappingsFile, stringsFile, areasFile, conditionSpawnsFile, skillCategoriesFile,
			locationAliasesFile, directPortalsFile, npcScoresFile, groupControllersFile, npcPartiesFile, null);
	}

	static RetailAiData load(File patternsDirectory, File mappingsFile, File stringsFile, File areasFile,
			File conditionSpawnsFile, File skillCategoriesFile, File locationAliasesFile, File directPortalsFile,
			File npcScoresFile, File groupControllersFile, File npcPartiesFile, File dynamicAreasFile) {
		ConditionSpawns conditionSpawns = conditionSpawnsFile == null
			? new ConditionSpawns(Map.of(), Map.of(), Map.of()) : loadConditionSpawns(conditionSpawnsFile);
		Areas areas = loadAreas(areasFile);
		Map<Integer, Npc> npcs = loadMappings(mappingsFile);
		Map<Integer, List<NpcParty>> npcParties = npcPartiesFile == null ? Map.of() : loadNpcParties(npcPartiesFile);
		npcParties.values().stream().flatMap(List::stream).flatMap(party -> party.members().stream())
			.filter(member -> !npcs.containsKey(member.id())).findFirst().ifPresent(member -> {
				throw new IllegalStateException("Undefined retail NPC party member: " + member.id());
			});
		return new RetailAiData(loadPatterns(patternsDirectory), npcs, loadStrings(stringsFile),
			skillCategoriesFile == null ? Map.of() : loadSkillCategories(skillCategoriesFile),
			npcScoresFile == null ? Map.of() : loadNpcScores(npcScoresFile),
			areas.named(), areas.resurrect(), areas.quests(), areas.limits(), areas.groupControls(),
			groupControllersFile == null ? Map.of() : loadGroupControllers(groupControllersFile), areas.skills(),
			locationAliasesFile == null ? Map.of() : loadLocationAliases(locationAliasesFile),
			conditionSpawns.spawns(), conditionSpawns.variables(), conditionSpawns.worldIdsByName(),
			directPortalsFile == null ? Map.of() : loadDirectPortals(directPortalsFile), npcParties,
			dynamicAreasFile == null ? Map.of() : loadDynamicAreas(dynamicAreasFile));
	}

	static Map<Integer, Map<String, Map<Integer, DynamicArea>>> loadDynamicAreas(File file) {
		Map<Integer, Map<String, Map<Integer, DynamicArea>>> areas = new HashMap<>();
		XMLInputFactory factory = xmlFactory();
		try (FileInputStream stream = new FileInputStream(file)) {
			XMLStreamReader reader = factory.createXMLStreamReader(stream);
			while (reader.hasNext()) {
				if (reader.next() != XMLStreamConstants.START_ELEMENT || !reader.getLocalName().equals("area")) {
					continue;
				}
				int worldId = Integer.parseInt(attribute(reader, "world_id"));
				String type = attribute(reader, "type");
				int id = Integer.parseInt(attribute(reader, "id"));
				if (!Set.of("MOVING_COLLISION_JUMP", "MOVING_COLLISION_WINDBOX").contains(type)) {
					throw new IllegalStateException("Unknown retail dynamic area type: " + type);
				}
				DynamicArea area = new DynamicArea(worldId, attribute(reader, "world_name"), type, id,
					attribute(reader, "name"), Boolean.parseBoolean(attribute(reader, "ai_pattern")),
					Integer.parseInt(attribute(reader, "start_time")), Integer.parseInt(attribute(reader, "end_time")),
					Integer.parseInt(attribute(reader, "life_time")),
					Boolean.parseBoolean(attribute(reader, "always_enabled")));
				if (areas.computeIfAbsent(worldId, ignored -> new HashMap<>())
					.computeIfAbsent(type, ignored -> new HashMap<>()).put(id, area) != null) {
					throw new IllegalStateException("Duplicate retail dynamic area: " + worldId + "/" + type + "/" + id);
				}
			}
			reader.close();
			return areas;
		} catch (Exception e) {
			throw new IllegalStateException("Failed to load retail dynamic areas from " + file.getPath(), e);
		}
	}

	static Map<Integer, List<NpcParty>> loadNpcParties(File file) {
		Map<Integer, List<NpcParty>> parties = new HashMap<>();
		Set<String> tokens = new HashSet<>();
		Set<String> members = new HashSet<>();
		XMLInputFactory factory = xmlFactory();
		try (FileInputStream stream = new FileInputStream(file)) {
			XMLStreamReader reader = factory.createXMLStreamReader(stream);
			int worldId = 0;
			String token = null;
			List<NpcPartyMember> partyMembers = null;
			while (reader.hasNext()) {
				int event = reader.next();
				if (event == XMLStreamConstants.START_ELEMENT) {
					switch (reader.getLocalName()) {
						case "world" -> worldId = Integer.parseInt(attribute(reader, "id"));
						case "party" -> {
							token = attribute(reader, "token");
							if (!tokens.add(worldId + ":" + token)) {
								throw new IllegalStateException("Duplicate retail NPC party: " + worldId + ":" + token);
							}
							partyMembers = new ArrayList<>();
						}
						case "npc" -> {
							NpcPartyMember member = new NpcPartyMember(Integer.parseInt(attribute(reader, "id")),
								Float.parseFloat(attribute(reader, "x")), Float.parseFloat(attribute(reader, "y")),
								Float.parseFloat(attribute(reader, "z")));
							String key = worldId + ":" + member.id() + ":" + member.x() + ":" + member.y() + ":" + member.z();
							if (!members.add(key)) {
								throw new IllegalStateException("Retail NPC party member belongs to multiple parties: " + key);
							}
							partyMembers.add(member);
						}
					}
				} else if (event == XMLStreamConstants.END_ELEMENT && reader.getLocalName().equals("party")) {
					parties.computeIfAbsent(worldId, ignored -> new ArrayList<>()).add(new NpcParty(token, partyMembers));
				}
			}
			reader.close();
			return parties;
		} catch (Exception e) {
			throw new IllegalStateException("Failed to load retail NPC parties from " + file.getPath(), e);
		}
	}

	private static Map<Integer, List<GroupController>> loadGroupControllers(File file) {
		Map<Integer, List<GroupController>> controllers = new HashMap<>();
		XMLInputFactory factory = xmlFactory();
		try (FileInputStream stream = new FileInputStream(file)) {
			XMLStreamReader reader = factory.createXMLStreamReader(stream);
			Set<Integer> ids = new HashSet<>();
			while (reader.hasNext()) {
				if (reader.next() == XMLStreamConstants.START_ELEMENT && reader.getLocalName().equals("controller")) {
					int id = Integer.parseInt(attribute(reader, "id"));
					if (!ids.add(id)) {
						throw new IllegalStateException("Duplicate retail group controller: " + id);
					}
					int worldId = Integer.parseInt(attribute(reader, "world_id"));
					controllers.computeIfAbsent(worldId, ignored -> new ArrayList<>()).add(new GroupController(id,
						attribute(reader, "name"), worldId, Integer.parseInt(attribute(reader, "type")),
						attribute(reader, "area_1"), attribute(reader, "area_2"), attribute(reader, "race"),
						Integer.parseInt(attribute(reader, "control_target_type")),
						Integer.parseInt(attribute(reader, "exit_world_id", "0")), attribute(reader, "exit_world", ""),
						attribute(reader, "exit_alias", "")));
				}
			}
			reader.close();
			return controllers;
		} catch (Exception e) {
			throw new IllegalStateException("Failed to load retail group controllers from " + file.getPath(), e);
		}
	}

	private static Map<Integer, NpcScore> loadNpcScores(File file) {
		Map<Integer, NpcScore> scores = new HashMap<>();
		XMLInputFactory factory = xmlFactory();
		try (FileInputStream stream = new FileInputStream(file)) {
			XMLStreamReader reader = factory.createXMLStreamReader(stream);
			while (reader.hasNext()) {
				if (reader.next() == XMLStreamConstants.START_ELEMENT && reader.getLocalName().equals("npc_score")) {
					int npcId = Integer.parseInt(attribute(reader, "npc_id"));
					NpcScore score = new NpcScore(Integer.parseInt(attribute(reader, "id")), npcId,
						attribute(reader, "name"), attribute(reader, "name_id"),
						Integer.parseInt(attribute(reader, "score_apply_type")),
						Integer.parseInt(attribute(reader, "equalizing_score")),
						Integer.parseInt(attribute(reader, "value")));
					if (scores.put(npcId, score) != null) {
						throw new IllegalStateException("Duplicate retail NPC score: " + npcId);
					}
				}
			}
			reader.close();
			return scores;
		} catch (Exception e) {
			throw new IllegalStateException("Failed to load retail NPC scores from " + file.getPath(), e);
		}
	}

	private static Map<Integer, DirectPortal> loadDirectPortals(File file) {
		Map<Integer, DirectPortal> portals = new HashMap<>();
		XMLInputFactory factory = xmlFactory();
		try (FileInputStream stream = new FileInputStream(file)) {
			XMLStreamReader reader = factory.createXMLStreamReader(stream);
			int id = 0;
			String name = null;
			int time = 0, count = 0, minLevel = 0, maxLevel = 0, groupId = 0, invadeType = 0;
			int worldId = 0, npcId = 0, weight = 0;
			String needItem = null;
			List<DirectPortalGroup> groups = null;
			List<DirectPortalPoint> points = null;
			DirectPortalEndpoint start = null;
			while (reader.hasNext()) {
				int event = reader.next();
				if (event == XMLStreamConstants.START_ELEMENT) {
					switch (reader.getLocalName()) {
						case "portal" -> {
							id = Integer.parseInt(attribute(reader, "id"));
							name = attribute(reader, "name");
							time = Integer.parseInt(attribute(reader, "time"));
							count = Integer.parseInt(attribute(reader, "count"));
							minLevel = Integer.parseInt(attribute(reader, "min_level"));
							maxLevel = Integer.parseInt(attribute(reader, "max_level"));
							needItem = attribute(reader, "need_item", "");
							groupId = Integer.parseInt(attribute(reader, "group_id", "0"));
							invadeType = Integer.parseInt(attribute(reader, "invade_type", "0"));
						}
						case "start", "destination" -> {
							worldId = Integer.parseInt(attribute(reader, "world_id"));
							npcId = Integer.parseInt(attribute(reader, "npc_id"));
							groups = new ArrayList<>();
						}
						case "group" -> {
							weight = Integer.parseInt(attribute(reader, "weight"));
							points = new ArrayList<>();
						}
						case "point" -> points.add(new DirectPortalPoint(Float.parseFloat(attribute(reader, "x")),
							Float.parseFloat(attribute(reader, "y")), Float.parseFloat(attribute(reader, "z")),
							Float.parseFloat(attribute(reader, "dir"))));
					}
				} else if (event == XMLStreamConstants.END_ELEMENT) {
					switch (reader.getLocalName()) {
						case "group" -> groups.add(new DirectPortalGroup(weight, points));
						case "start" -> start = new DirectPortalEndpoint(worldId, npcId, groups);
						case "destination" -> {
							DirectPortal portal = new DirectPortal(id, name, time, count, minLevel, maxLevel,
								needItem, groupId, invadeType, start,
								new DirectPortalEndpoint(worldId, npcId, groups));
							if (portals.put(id, portal) != null) {
								throw new IllegalStateException("Duplicate retail direct portal: " + id);
							}
						}
					}
				}
			}
			reader.close();
			return portals;
		} catch (Exception e) {
			throw new IllegalStateException("Failed to load retail direct portals from " + file.getPath(), e);
		}
	}

	private static Map<Integer, Map<String, List<LocationAliasPoint>>> loadLocationAliases(File file) {
		Map<Integer, Map<String, List<LocationAliasPoint>>> aliases = new HashMap<>();
		XMLInputFactory factory = xmlFactory();
		try (FileInputStream stream = new FileInputStream(file)) {
			XMLStreamReader reader = factory.createXMLStreamReader(stream);
			int worldId = 0;
			String name = null;
			List<LocationAliasPoint> points = null;
			while (reader.hasNext()) {
				int event = reader.next();
				if (event == XMLStreamConstants.START_ELEMENT && reader.getLocalName().equals("alias")) {
					worldId = Integer.parseInt(attribute(reader, "world_id"));
					name = attribute(reader, "name");
					points = new ArrayList<>();
				} else if (event == XMLStreamConstants.START_ELEMENT && reader.getLocalName().equals("point")) {
					points.add(new LocationAliasPoint(Float.parseFloat(attribute(reader, "x")),
						Float.parseFloat(attribute(reader, "y")), Float.parseFloat(attribute(reader, "z")),
						Float.parseFloat(attribute(reader, "dir"))));
				} else if (event == XMLStreamConstants.END_ELEMENT && reader.getLocalName().equals("alias")) {
					List<LocationAliasPoint> previous = aliases.computeIfAbsent(worldId, ignored -> new HashMap<>())
						.put(name.toLowerCase(), List.copyOf(points));
					if (previous != null) {
						throw new IllegalStateException("Duplicate retail AI location alias: " + worldId + "/" + name);
					}
				}
			}
			reader.close();
			return aliases;
		} catch (Exception e) {
			throw new IllegalStateException("Failed to load retail AI location aliases from " + file.getPath(), e);
		}
	}

	private static Map<Integer, String> loadSkillCategories(File file) {
		Map<Integer, String> categories = new HashMap<>();
		XMLInputFactory factory = xmlFactory();
		try (FileInputStream stream = new FileInputStream(file)) {
			XMLStreamReader reader = factory.createXMLStreamReader(stream);
			while (reader.hasNext()) {
				if (reader.next() == XMLStreamConstants.START_ELEMENT && reader.getLocalName().equals("skill")) {
					int id = Integer.parseInt(attribute(reader, "id"));
					if (categories.put(id, attribute(reader, "category")) != null) {
						throw new IllegalStateException("Duplicate retail skill category: " + id);
					}
				}
			}
			reader.close();
			return categories;
		} catch (Exception e) {
			throw new IllegalStateException("Failed to load retail skill categories from " + file.getPath(), e);
		}
	}

	private static ConditionSpawns loadConditionSpawns(File file) {
		Map<Integer, List<ConditionSpawn>> spawns = new HashMap<>();
		Map<Integer, Set<String>> variables = new HashMap<>();
		Map<String, Integer> worldIdsByName = new HashMap<>();
		XMLInputFactory factory = xmlFactory();
		try (FileInputStream stream = new FileInputStream(file)) {
			XMLStreamReader reader = factory.createXMLStreamReader(stream);
			int worldId = 0;
			int conditionId = 0;
			String expression = null;
			String groupMode = null;
			boolean despawnAtOther = false;
			int groupProbability = 0;
			List<ConditionSpawn> worldSpawns = null;
			List<ConditionSpawnGroup> groups = null;
			List<List<ConditionSpawnChoice>> slots = null;
			List<ConditionSpawnChoice> slot = null;
			List<ConditionSpawnNpc> partyMembers = null;
			String partyId = null;
			int npcId = 0, choiceProbability = 0, npcHeading = 0, initialDelay = 0, initialDelayExtra = 0;
			float npcX = 0, npcY = 0, npcZ = 0, sensoryBottom = 0, sensoryTop = 0;
			String walker = null;
			List<Point2D> sensoryPoints = null;
			while (reader.hasNext()) {
				int event = reader.next();
				if (event == XMLStreamConstants.START_ELEMENT) {
					switch (reader.getLocalName()) {
						case "world" -> {
							worldId = Integer.parseInt(attribute(reader, "id"));
							worldIdsByName.put(attribute(reader, "name").toLowerCase(), worldId);
							worldSpawns = new ArrayList<>();
							variables.put(worldId, new HashSet<>());
						}
						case "variable" -> variables.get(worldId).add(attribute(reader, "name").toLowerCase());
						case "condition" -> {
							conditionId = Integer.parseInt(attribute(reader, "id"));
							expression = attribute(reader, "expression");
							despawnAtOther = Boolean.parseBoolean(attribute(reader, "despawn_at_other"));
							groupMode = attribute(reader, "group_mode");
							groups = new ArrayList<>();
						}
						case "group" -> {
							groupProbability = Integer.parseInt(attribute(reader, "probability"));
							slots = new ArrayList<>();
						}
						case "slot" -> slot = new ArrayList<>();
						case "party" -> {
							choiceProbability = Integer.parseInt(attribute(reader, "probability"));
							partyId = attribute(reader, "token");
							partyMembers = new ArrayList<>();
						}
						case "npc" -> {
							npcId = Integer.parseInt(attribute(reader, "id"));
							if (partyMembers == null) {
								choiceProbability = Integer.parseInt(attribute(reader, "probability"));
							}
							npcX = Float.parseFloat(attribute(reader, "x"));
							npcY = Float.parseFloat(attribute(reader, "y"));
							npcZ = Float.parseFloat(attribute(reader, "z"));
							npcHeading = Integer.parseInt(attribute(reader, "heading"));
							initialDelay = Integer.parseInt(attribute(reader, "initial_delay"));
							initialDelayExtra = Integer.parseInt(attribute(reader, "initial_delay_extra"));
							walker = attribute(reader, "walker");
							sensoryPoints = null;
						}
						case "sensory_area" -> {
							sensoryBottom = Float.parseFloat(attribute(reader, "bottom"));
							sensoryTop = Float.parseFloat(attribute(reader, "top"));
							sensoryPoints = new ArrayList<>();
						}
						case "point" -> {
							if (sensoryPoints != null) {
								sensoryPoints.add(new Point2D(Float.parseFloat(attribute(reader, "x")),
									Float.parseFloat(attribute(reader, "y"))));
							}
						}
					}
				} else if (event == XMLStreamConstants.END_ELEMENT) {
					switch (reader.getLocalName()) {
						case "npc" -> {
							Area sensoryArea = sensoryPoints == null ? null : new PolyArea(
								ZoneName.createOrGet("retail_sensory_" + worldId + "_" + npcId + "_" + npcX + "_" + npcY),
								worldId, sensoryPoints, sensoryBottom, sensoryTop);
							ConditionSpawnNpc npc = new ConditionSpawnNpc(npcId, npcX, npcY, npcZ, npcHeading,
								initialDelay, initialDelayExtra, walker, sensoryArea);
							if (partyMembers == null) {
								slot.add(new ConditionSpawnChoice(choiceProbability, null, List.of(npc)));
							} else {
								partyMembers.add(npc);
							}
						}
						case "party" -> {
							slot.add(new ConditionSpawnChoice(choiceProbability, partyId, partyMembers));
							partyMembers = null;
							partyId = null;
						}
						case "slot" -> slots.add(List.copyOf(slot));
						case "group" -> groups.add(new ConditionSpawnGroup(groupProbability, slots));
						case "condition" -> worldSpawns.add(new ConditionSpawn(conditionId, expression, despawnAtOther,
							groupMode, groups));
						case "world" -> spawns.put(worldId, List.copyOf(worldSpawns));
					}
				}
			}
			reader.close();
			Map<Integer, Set<String>> immutableVariables = new HashMap<>();
			variables.forEach((id, names) -> immutableVariables.put(id, Set.copyOf(names)));
			return new ConditionSpawns(spawns, immutableVariables, worldIdsByName);
		} catch (Exception e) {
			throw new IllegalStateException("Failed to load retail condition spawns from " + file.getPath(), e);
		}
	}

	private static Map<String, Pattern> loadPatterns(File directory) {
		File[] files = directory.listFiles(file -> file.isFile() && file.getName().endsWith(".xml"));
		if (files == null || files.length == 0) {
			throw new IllegalStateException("Retail AI pattern directory not found or empty: " + directory.getPath());
		}
		Arrays.sort(files, Comparator.comparing(File::getName));
		Map<String, Pattern> patterns = new HashMap<>();
		XMLInputFactory factory = xmlFactory();
		for (File file : files) {
			loadPatterns(file, factory, patterns);
		}
		return patterns;
	}

	private static void loadPatterns(File file, XMLInputFactory factory, Map<String, Pattern> patterns) {
		try (FileInputStream stream = new FileInputStream(file)) {
			XMLStreamReader reader = factory.createXMLStreamReader(stream);
			ArrayDeque<Node> stack = new ArrayDeque<>();
			while (reader.hasNext()) {
				int event = reader.next();
				if (event == XMLStreamConstants.START_ELEMENT) {
					String name = reader.getLocalName();
					if (!stack.isEmpty() || name.equals("npc_ai_pattern")) {
						Node node = new Node(name);
						if (!stack.isEmpty()) {
							stack.peek().children.add(node);
						}
						stack.push(node);
					}
				} else if ((event == XMLStreamConstants.CHARACTERS || event == XMLStreamConstants.CDATA) && !stack.isEmpty()) {
					stack.peek().text.append(reader.getText());
				} else if (event == XMLStreamConstants.END_ELEMENT && !stack.isEmpty()) {
					Node node = stack.pop();
					if (stack.isEmpty()) {
						Pattern pattern = compile(node);
						patterns.put(pattern.name().toLowerCase(), pattern);
					}
				}
			}
			reader.close();
		} catch (Exception e) {
			throw new IllegalStateException("Failed to load retail AI patterns from " + file.getPath(), e);
		}
	}

	private static Map<Integer, Npc> loadMappings(File file) {
		Map<Integer, Npc> npcs = new HashMap<>();
		XMLInputFactory factory = xmlFactory();
		try (FileInputStream stream = new FileInputStream(file)) {
			XMLStreamReader reader = factory.createXMLStreamReader(stream);
			while (reader.hasNext()) {
				if (reader.next() == XMLStreamConstants.START_ELEMENT && reader.getLocalName().equals("npc")) {
					int id = Integer.parseInt(attribute(reader, "id"));
					String ai = attribute(reader, "ai");
					if (ai != null && !ai.isBlank()) {
						npcs.put(id, new Npc(id, attribute(reader, "name"), ai,
							parseFloat(attribute(reader, "sensory_range")),
							parseFloat(attribute(reader, "sensory_range_short")),
							parseFloat(attribute(reader, "sensory_angle")),
							parseInt(attribute(reader, "talk_delay")), attribute(reader, "max_chase_time"),
							PathfindFailReaction.valueOf(attribute(reader, "react_to_pathfind_fail", "return_to_sp")
								.toUpperCase(Locale.ROOT)), attribute(reader, "move_type_return", "walk"),
							parseInt(attribute(reader, "move_speed_return", "150")),
							parseInt(attribute(reader, "decrease_sensory_range_return", "50"))));
					}
				}
			}
			reader.close();
			return npcs;
		} catch (Exception e) {
			throw new IllegalStateException("Failed to load retail NPC AI mappings from " + file.getPath(), e);
		}
	}

	private static Map<String, Integer> loadStrings(File file) {
		Map<String, Integer> strings = new HashMap<>();
		XMLInputFactory factory = xmlFactory();
		try (FileInputStream stream = new FileInputStream(file)) {
			XMLStreamReader reader = factory.createXMLStreamReader(stream);
			while (reader.hasNext()) {
				if (reader.next() == XMLStreamConstants.START_ELEMENT && reader.getLocalName().equals("string")) {
					strings.put(attribute(reader, "name").toLowerCase(), Integer.parseInt(attribute(reader, "id")));
				}
			}
			reader.close();
			return strings;
		} catch (Exception e) {
			throw new IllegalStateException("Failed to load retail AI strings from " + file.getPath(), e);
		}
	}

	private static Areas loadAreas(File file) {
		Map<Integer, Map<String, Area>> areas = new HashMap<>();
		Map<Integer, List<ResurrectArea>> resurrectAreas = new HashMap<>();
		Map<Integer, List<QuestArea>> questAreas = new HashMap<>();
		Map<Integer, List<LimitArea>> limitAreas = new HashMap<>();
		Map<Integer, List<GroupControlArea>> groupControlAreas = new HashMap<>();
		Map<Integer, Map<Integer, List<Area>>> skillAreas = new HashMap<>();
		XMLInputFactory factory = xmlFactory();
		try (FileInputStream stream = new FileInputStream(file)) {
			XMLStreamReader reader = factory.createXMLStreamReader(stream);
			int worldId = 0;
			String name = null;
			int id = 0;
			String areaType = null;
			String locationAlias = null;
			String tribe = null;
			int race = 0;
			boolean dynamic = false;
			String noBind = null;
			boolean noRecall = false;
			String noPark = null;
			int noParkReenterInterval = 0;
			boolean noRide = false;
			boolean noShop = false;
			int priority = 0;
			float bottom = 0;
			float top = 0;
			List<Point2D> points = null;
			List<LocationAliasPoint> destinations = null;
			List<Integer> questIds = null;
			while (reader.hasNext()) {
				int event = reader.next();
				if (event == XMLStreamConstants.START_ELEMENT
						&& Set.of("area", "resurrect_area", "quest_area", "limit_area", "groupctrl_area", "skill_area")
							.contains(reader.getLocalName())) {
					areaType = reader.getLocalName();
					worldId = Integer.parseInt(attribute(reader, "world_id"));
					name = areaType.equals("skill_area") ? null : attribute(reader, "name");
					id = areaType.equals("skill_area") ? Integer.parseInt(attribute(reader, "id")) : 0;
					locationAlias = areaType.equals("resurrect_area") ? attribute(reader, "location_alias") : null;
					tribe = areaType.equals("resurrect_area") ? attribute(reader, "tribe") : null;
					race = areaType.equals("resurrect_area") ? Integer.parseInt(attribute(reader, "race")) : 0;
					bottom = Float.parseFloat(attribute(reader, "bottom"));
					top = Float.parseFloat(attribute(reader, "top"));
					points = new ArrayList<>();
					destinations = new ArrayList<>();
					questIds = areaType.equals("quest_area") ? Arrays.stream(attribute(reader, "quests", "").split(","))
						.map(String::trim).filter(value -> !value.isEmpty()).map(Integer::parseInt).toList() : List.of();
					dynamic = areaType.equals("limit_area") && Boolean.parseBoolean(attribute(reader, "dynamic"));
					noBind = areaType.equals("limit_area") ? attribute(reader, "no_bind") : null;
					noRecall = areaType.equals("limit_area") && Boolean.parseBoolean(attribute(reader, "no_recall"));
					noPark = areaType.equals("limit_area") ? attribute(reader, "no_park") : null;
					noParkReenterInterval = areaType.equals("limit_area")
						? Integer.parseInt(attribute(reader, "no_park_reenter_interval")) : 0;
					noRide = areaType.equals("limit_area") && Boolean.parseBoolean(attribute(reader, "no_ride"));
					noShop = areaType.equals("limit_area") && Boolean.parseBoolean(attribute(reader, "no_shop"));
					priority = areaType.equals("limit_area") ? Integer.parseInt(attribute(reader, "priority")) : 0;
				} else if (event == XMLStreamConstants.START_ELEMENT && reader.getLocalName().equals("point")) {
					points.add(new Point2D(Float.parseFloat(attribute(reader, "x")), Float.parseFloat(attribute(reader, "y"))));
				} else if (event == XMLStreamConstants.START_ELEMENT && reader.getLocalName().equals("destination")) {
					destinations.add(new LocationAliasPoint(Float.parseFloat(attribute(reader, "x")),
						Float.parseFloat(attribute(reader, "y")), Float.parseFloat(attribute(reader, "z")),
						Float.parseFloat(attribute(reader, "dir"))));
				} else if (event == XMLStreamConstants.END_ELEMENT
						&& Set.of("area", "resurrect_area", "quest_area", "limit_area", "groupctrl_area", "skill_area")
							.contains(reader.getLocalName())) {
					String zoneName = areaType.equals("skill_area") ? "retail_skill_area_" + worldId + "_" + id : name;
					Area area = new PolyArea(ZoneName.createOrGet(zoneName), worldId, points, bottom, top);
					if (areaType.equals("skill_area")) {
						skillAreas.computeIfAbsent(worldId, ignored -> new HashMap<>())
							.computeIfAbsent(id, ignored -> new ArrayList<>()).add(area);
					} else if (areaType.equals("resurrect_area")) {
						resurrectAreas.computeIfAbsent(worldId, ignored -> new ArrayList<>())
							.add(new ResurrectArea(name, locationAlias, race, tribe, area, destinations));
					} else if (areaType.equals("quest_area")) {
						questAreas.computeIfAbsent(worldId, ignored -> new ArrayList<>())
							.add(new QuestArea(name, questIds, area));
					} else if (areaType.equals("limit_area")) {
						limitAreas.computeIfAbsent(worldId, ignored -> new ArrayList<>())
							.add(new LimitArea(name, dynamic, noBind, noRecall, noPark, noParkReenterInterval,
								noRide, noShop, priority, area));
					} else if (areaType.equals("groupctrl_area")) {
						groupControlAreas.computeIfAbsent(worldId, ignored -> new ArrayList<>())
							.add(new GroupControlArea(name, area));
					} else {
						Area previous = areas.computeIfAbsent(worldId, ignored -> new HashMap<>()).put(name.toLowerCase(), area);
						if (previous != null) {
							throw new IllegalStateException("Duplicate retail AI area: " + worldId + "/" + name);
						}
					}
				}
			}
			reader.close();
			return new Areas(areas, resurrectAreas, questAreas, limitAreas, groupControlAreas, skillAreas);
		} catch (Exception e) {
			throw new IllegalStateException("Failed to load retail AI areas from " + file.getPath(), e);
		}
	}

	private static Pattern compile(Node root) {
		String name = root.childText("name");
		if (name == null || name.isBlank()) {
			throw new IllegalStateException("Retail AI pattern without name");
		}
		Map<String, List<Rule>> events = new LinkedHashMap<>();
		Node handlers = root.child("event_handlers");
		if (handlers != null) {
			for (Node handler : handlers.children) {
				List<Rule> rules = new ArrayList<>();
				for (Node pattern : handler.children("pattern")) {
					rules.add(new Rule(parseInt(pattern.childText("priority")), pattern.childText("action_category"),
						operations(pattern.child("conditions")), operations(pattern.child("actions"))));
				}
				rules.sort(Comparator.comparingInt(Rule::priority).reversed());
				events.put(handler.name, List.copyOf(rules));
			}
		}
		return new Pattern(name, events);
	}

	private static List<Operation> operations(Node container) {
		if (container == null) {
			return List.of();
		}
		List<Operation> operations = new ArrayList<>(container.children.size());
		for (Node node : container.children) {
			Map<String, String> values = new LinkedHashMap<>();
			collectLeaves(node, values);
			operations.add(new Operation(node.name, values));
		}
		return operations;
	}

	private static void collectLeaves(Node node, Map<String, String> values) {
		if (node.children.isEmpty()) {
			values.put(node.name, node.text());
			return;
		}
		for (Node child : node.children) {
			collectLeaves(child, values);
		}
	}

	private static XMLInputFactory xmlFactory() {
		XMLInputFactory factory = XMLInputFactory.newFactory();
		factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
		factory.setProperty("javax.xml.stream.isSupportingExternalEntities", false);
		return factory;
	}

	private static String attribute(XMLStreamReader reader, String name) {
		return reader.getAttributeValue(null, name);
	}

	private static String attribute(XMLStreamReader reader, String name, String defaultValue) {
		String value = attribute(reader, name);
		return value == null || value.isBlank() ? defaultValue : value;
	}

	private static int parseInt(String value) {
		return value == null || value.isBlank() ? 0 : Integer.parseInt(value);
	}

	private static float parseFloat(String value) {
		return value == null || value.isBlank() ? 0 : Float.parseFloat(value);
	}

	private record ConditionSpawns(Map<Integer, List<ConditionSpawn>> spawns,
			Map<Integer, Set<String>> variables, Map<String, Integer> worldIdsByName) {
	}

	private record Areas(Map<Integer, Map<String, Area>> named,
			Map<Integer, List<ResurrectArea>> resurrect,
			Map<Integer, List<QuestArea>> quests,
			Map<Integer, List<LimitArea>> limits,
			Map<Integer, List<GroupControlArea>> groupControls,
			Map<Integer, Map<Integer, List<Area>>> skills) {
	}

	private static final class Node {
		private final String name;
		private final StringBuilder text = new StringBuilder();
		private final List<Node> children = new ArrayList<>();

		private Node(String name) {
			this.name = name;
		}

		private Node child(String name) {
			for (Node child : children) {
				if (child.name.equals(name)) {
					return child;
				}
			}
			return null;
		}

		private List<Node> children(String name) {
			return children.stream().filter(child -> child.name.equals(name)).toList();
		}

		private String childText(String name) {
			Node child = child(name);
			return child == null ? null : child.text();
		}

		private String text() {
			return text.toString().trim();
		}
	}
}
