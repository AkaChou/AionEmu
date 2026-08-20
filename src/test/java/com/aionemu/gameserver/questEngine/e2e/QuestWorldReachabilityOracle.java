package com.aionemu.gameserver.questEngine.e2e;

import com.aionemu.gameserver.questEngine.definition.AfterCommitAction;
import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.definition.QuestTransition;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * 依据生产地图和静态出生数据判断任务对话 NPC 是否可达；动态活动和脚本生成未被证明时保持关闭式判定。
 * Determines whether quest-dialog NPCs are reachable from production maps and static spawn data; dynamic event or
 * scripted spawns remain fail-closed until explicitly proven by the quest definition.
 */
final class QuestWorldReachabilityOracle {
	private static final Path WORLD_MAPS = Path.of("src/main/resources/aion/data/static_data/world_maps.xml");
	private static final Path SPAWNS = Path.of("src/main/resources/aion/data/static_data/spawns");

	private final Map<Integer, Set<Integer>> staticSpawnWorldsByNpc;

	private QuestWorldReachabilityOracle(Map<Integer, Set<Integer>> staticSpawnWorldsByNpc) {
		Map<Integer, Set<Integer>> frozen = new HashMap<>();
		staticSpawnWorldsByNpc.forEach((npcId, worldIds) -> frozen.put(npcId, Set.copyOf(worldIds)));
		this.staticSpawnWorldsByNpc = Map.copyOf(frozen);
	}

	/**
	 * 从仓库生产静态数据路径加载 oracle，不初始化全局 DataManager。
	 * Loads the oracle from repository production static-data paths without initializing the global DataManager.
	 */
	static QuestWorldReachabilityOracle loadProductionData() throws IOException, XMLStreamException {
		return load(WORLD_MAPS, SPAWNS);
	}

	/**
	 * 从指定地图文件和 spawn 目录构建只读索引。
	 * Builds a read-only index from the supplied world-map file and spawn directory.
	 *
	 * @param worldMapsFile 生产 world_maps.xml 路径 / production world_maps.xml path
	 * @param spawnsDirectory 生产 spawn XML 根目录 / production spawn XML root directory
	 */
	static QuestWorldReachabilityOracle load(Path worldMapsFile, Path spawnsDirectory)
			throws IOException, XMLStreamException {
		XMLInputFactory inputFactory = XMLInputFactory.newFactory();
		inputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
		inputFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
		Set<Integer> loadedWorlds = readLoadedWorlds(worldMapsFile, inputFactory);
		Map<Integer, Set<Integer>> spawnWorlds = new HashMap<>();
		try (Stream<Path> paths = Files.walk(spawnsDirectory)) {
			for (Path path : paths.filter(Files::isRegularFile)
					.filter(candidate -> candidate.getFileName().toString().endsWith(".xml"))
					.filter(candidate -> !candidate.getFileName().toString().startsWith("new"))
					.sorted().toList()) {
				readSpawnFile(path, inputFactory, loadedWorlds, spawnWorlds);
			}
		}
		return new QuestWorldReachabilityOracle(spawnWorlds);
	}

	/**
	 * 返回必须升级到真实运行时验证的原因；空字符串表示静态世界证据足以继续内存审计。
	 * Returns the reason real-runtime validation is required; an empty string means static-world evidence is sufficient
	 * to continue the in-memory audit.
	 */
	String runtimeRequiredReason(CompiledQuestDefinition definition, QuestTransition transition) {
		if (!(transition.event() instanceof QuestEvent.TalkToNpc talk)) {
			return "";
		}
		int npcId = talk.npcId();
		if (staticSpawnWorldsByNpc.containsKey(npcId) || questSpawnsNpc(definition, npcId)) {
			return "";
		}
		return "npc " + npcId + " has no usable static spawn in loaded world_maps.xml and quest "
			+ definition.id() + " does not spawn it";
	}

	private static Set<Integer> readLoadedWorlds(Path worldMapsFile, XMLInputFactory inputFactory)
			throws IOException, XMLStreamException {
		Set<Integer> worldIds = new HashSet<>();
		try (InputStream input = Files.newInputStream(worldMapsFile)) {
			XMLStreamReader reader = inputFactory.createXMLStreamReader(input);
			try {
				while (reader.hasNext()) {
					if (reader.next() == XMLStreamConstants.START_ELEMENT && "map".equals(reader.getLocalName())) {
						worldIds.add(Integer.parseInt(attribute(reader, "id")));
					}
				}
			} finally {
				reader.close();
			}
		}
		return Set.copyOf(worldIds);
	}

	private static void readSpawnFile(Path path, XMLInputFactory inputFactory, Set<Integer> loadedWorlds,
			Map<Integer, Set<Integer>> spawnWorlds) throws IOException, XMLStreamException {
		try (InputStream input = Files.newInputStream(path)) {
			XMLStreamReader reader = inputFactory.createXMLStreamReader(input);
			int worldId = 0;
			int npcId = 0;
			int depth = 0;
			int spawnMapDepth = -1;
			int spawnDepth = -1;
			try {
				while (reader.hasNext()) {
					int event = reader.next();
					if (event == XMLStreamConstants.START_ELEMENT) {
						depth++;
						if ("spawn_map".equals(reader.getLocalName())) {
							worldId = Integer.parseInt(attribute(reader, "map_id"));
							spawnMapDepth = depth;
						}
						else if ("spawn".equals(reader.getLocalName()) && depth == spawnMapDepth + 1) {
							npcId = parseOptionalInt(reader.getAttributeValue(null, "npc_id"));
							spawnDepth = depth;
						}
						else if ("spot".equals(reader.getLocalName()) && spawnDepth > 0
								&& npcId > 0 && loadedWorlds.contains(worldId)) {
							spawnWorlds.computeIfAbsent(npcId, ignored -> new HashSet<>()).add(worldId);
						}
					}
					else if (event == XMLStreamConstants.END_ELEMENT) {
						if ("spawn".equals(reader.getLocalName()) && depth == spawnDepth) {
							npcId = 0;
							spawnDepth = -1;
						}
						else if ("spawn_map".equals(reader.getLocalName()) && depth == spawnMapDepth) {
							worldId = 0;
							spawnMapDepth = -1;
						}
						depth--;
					}
				}
			} finally {
				reader.close();
			}
		}
	}

	private static boolean questSpawnsNpc(CompiledQuestDefinition definition, int npcId) {
		return definition.definition().transitions().stream()
			.flatMap(transition -> transition.afterCommit().stream())
			.anyMatch(action -> action instanceof AfterCommitAction.SpawnNpc spawn && spawn.templateId() == npcId
				|| action instanceof AfterCommitAction.SpawnNpcRandom random
					&& random.variants().stream().anyMatch(variant -> variant.templateId() == npcId));
	}

	private static String attribute(XMLStreamReader reader, String name) {
		String value = reader.getAttributeValue(null, name);
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException("missing XML attribute " + name + " at " + reader.getLocation());
		}
		return value;
	}

	private static int parseOptionalInt(String value) {
		return value == null || value.isBlank() ? 0 : Integer.parseInt(value);
	}
}
