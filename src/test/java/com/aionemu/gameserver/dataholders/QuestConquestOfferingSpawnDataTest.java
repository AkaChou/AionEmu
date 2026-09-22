package com.aionemu.gameserver.dataholders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aionemu.gameserver.model.templates.spawns.SpawnGroup2;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * 锁定“征服之祭物”击杀段在玩家可达世界按活动时段刷出的刷怪契约。
 * Locks the spawn contract that keeps the Conquest/Offering hunt stage of quest 15321/25321 solvable inside the
 * player-reachable Inggison/Gelkmaros worlds while the scheduled event runs.
 * <p>背景：真端把 4.8 Rotation（征服之祭物）刷怪点登记在大师服镜像地图 LF4_M/DF4_M（世界 210130000/220140000），
 * 单机 emulator 没有大师服入口，因此 15321/25321 第 3 段原先没有可达的击杀目标。修复方式是把同一活动
 * (conquest id 1/2) 的击杀目标补登记到可玩世界 210050000/220070000，不改变活动本身的排程与状态机。</p>
 * <p>Background: retail registers the 4.8 Rotation (Conquest/Offering) monsters on the master-server maps
 * LF4_M/DF4_M (worlds 210130000/220140000); a single-server emulator has no master-server entry, so stage 3 of
 * 15321/25321 had no reachable kill target. The repair registers the same event (conquest id 1/2) kill targets for
 * the playable worlds 210050000/220070000 without touching the event schedule or state machine.</p>
 */
class QuestConquestOfferingSpawnDataTest {

	/** 15321/25321 第 3 段要求击杀 10 只征服之祭物。/ Stage 3 of 15321/25321 requires 10 offerings killed. */
	private static final int REQUIRED_KILLS = 10;

	private static final Path QUESTS = Path.of("src/main/resources/aion/data/static_data/quest_definition/quests");
	private static final Path CONQUEST_SPAWNS = Path.of("src/main/resources/aion/data/static_data/spawns/Conquest");
	private static final Path SCHEDULE = Path.of("src/main/resources/aion/config/schedule/conquest_schedule.xml");

	@Test
	void inggisonEventSpawnsQuest15321TargetsInPlayableWorld() throws Exception {
		EventSpawn live = conquestEventSpawn("210050000_Inggison.xml", 1);
		Set<Integer> questTargets = questKillTargets(15321);
		Set<Integer> masterTargets = conquestEventSpawn("210130000_Inggison [Master Server].xml", 1).npcIds();

		assertEquals(Set.of(210050000), live.mapIds(),
			"英吉斯温活动刷怪必须声明在可玩世界 210050000 / event spawns must target the playable Inggison 210050000");
		assertTrue(questTargets.containsAll(live.npcIds()),
			"活动刷怪必须是 15321 声明的击杀目标 / event npcs must be quest 15321 kill targets, unexpected="
				+ minus(live.npcIds(), questTargets));
		assertTrue(masterTargets.containsAll(live.npcIds()),
			"可玩世界刷怪集合必须仍是大师服事件集合的子集 / playable-world set must stay a subset of the mirror block");
		assertTrue(live.npcIds().size() >= REQUIRED_KILLS, "至少刷出 " + REQUIRED_KILLS + " 种击杀目标 / at least "
			+ REQUIRED_KILLS + " distinct kill targets required, actual=" + live.npcIds().size());
		assertTrue(live.spots() >= REQUIRED_KILLS, "刷怪点必须足以完成 " + REQUIRED_KILLS + " 杀 / at least "
			+ REQUIRED_KILLS + " spawn spots required, actual=" + live.spots());
		assertTrue(scheduledConquestIds().contains(1),
			"英吉斯温征服活动必须仍在排程表中 / Inggison conquest id 1 must stay scheduled");
	}

	@Test
	void gelkmarosEventSpawnsQuest25321TargetsInPlayableWorld() throws Exception {
		EventSpawn live = conquestEventSpawn("220070000_Gelkmaros.xml", 2);
		Set<Integer> questTargets = questKillTargets(25321);
		Set<Integer> masterTargets = conquestEventSpawn("220140000_Gelkmaros [Master Server].xml", 2).npcIds();

		assertEquals(Set.of(220070000), live.mapIds(),
			"格尔克马洛斯活动刷怪必须声明在可玩世界 220070000 / event spawns must target the playable Gelkmaros 220070000");
		assertTrue(questTargets.containsAll(live.npcIds()),
			"活动刷怪必须是 25321 声明的击杀目标 / event npcs must be quest 25321 kill targets, unexpected="
				+ minus(live.npcIds(), questTargets));
		assertTrue(masterTargets.containsAll(live.npcIds()),
			"可玩世界刷怪集合必须仍是大师服事件集合的子集 / playable-world set must stay a subset of the mirror block");
		assertTrue(live.npcIds().size() >= REQUIRED_KILLS, "至少刷出 " + REQUIRED_KILLS + " 种击杀目标 / at least "
			+ REQUIRED_KILLS + " distinct kill targets required, actual=" + live.npcIds().size());
		assertTrue(live.spots() >= REQUIRED_KILLS, "刷怪点必须足以完成 " + REQUIRED_KILLS + " 杀 / at least "
			+ REQUIRED_KILLS + " spawn spots required, actual=" + live.spots());
		assertTrue(scheduledConquestIds().contains(2),
			"格尔克马洛斯征服活动必须仍在排程表中 / Gelkmaros conquest id 2 must stay scheduled");
	}

	@Test
	void productionSpawnLoaderIndexesPlayableWorldConquestGroups() throws Exception {
		SpawnsData2 data = SpawnsData2.load(CONQUEST_SPAWNS.toFile(), null);

		assertTrue(conquestGroupWorldIds(data, 1).containsAll(Set.of(210050000, 210130000)),
			"conquest id 1 必须同时索引可玩世界与镜像世界 / loc 1 must index the playable world and its mirror");
		assertTrue(conquestGroupWorldIds(data, 2).containsAll(Set.of(220070000, 220140000)),
			"conquest id 2 必须同时索引可玩世界与镜像世界 / loc 2 must index the playable world and its mirror");
		assertTrue(indexedConquestNpcIds(data, 1, 210050000).contains(236530),
			"可玩世界 conquest id 1 必须索引到 236530 / the playable Inggison block must index npc 236530");
		assertTrue(indexedConquestNpcIds(data, 2, 220070000).contains(236586),
			"可玩世界 conquest id 2 必须索引到 236586 / the playable Gelkmaros block must index npc 236586");
	}

	/** 生产加载器为某个征服地点索引的世界 ID 集合。 / World ids indexed for one conquest location. */
	private static Set<Integer> conquestGroupWorldIds(SpawnsData2 data, int conquestId) {
		List<SpawnGroup2> groups = data.getConquestSpawnsByLocId(conquestId);
		assertNotNull(groups, "conquest id " + conquestId + " must be indexed");
		return groups.stream().map(SpawnGroup2::getWorldId).collect(Collectors.toSet());
	}

	/** 生产加载器在指定世界为某个征服地点索引到的 NPC 集合。 / Npcs indexed for one conquest location and world. */
	private static Set<Integer> indexedConquestNpcIds(SpawnsData2 data, int conquestId, int worldId) {
		List<SpawnGroup2> groups = data.getConquestSpawnsByLocId(conquestId);
		assertNotNull(groups, "conquest id " + conquestId + " must be indexed");
		return groups.stream().filter(group -> group.getWorldId() == worldId)
			.flatMap(group -> group.getSpawnTemplates().stream())
			.map(SpawnTemplate::getNpcId)
			.collect(Collectors.toSet());
	}

	/** Collects the CONQUEST-state spawns of one conquest id: declared worlds, npc ids and spot count. */
	private static EventSpawn conquestEventSpawn(String fileName, int conquestId) throws Exception {
		Set<Integer> worlds = new LinkedHashSet<>();
		Set<Integer> npcIds = new LinkedHashSet<>();
		int spots = 0;
		for (Element map : elements(parse(CONQUEST_SPAWNS.resolve(fileName)).getElementsByTagName("spawn_map"))) {
			worlds.add(Integer.parseInt(map.getAttribute("map_id")));
			for (Element conquest : elements(map.getElementsByTagName("conquest_spawn"))) {
				if (Integer.parseInt(conquest.getAttribute("id")) != conquestId) {
					continue;
				}
				for (Element state : elements(conquest.getElementsByTagName("conquest_type"))) {
					if (!"CONQUEST".equals(state.getAttribute("ostate"))) {
						continue;
					}
					for (Element spawn : elements(state.getElementsByTagName("spawn"))) {
						npcIds.add(Integer.parseInt(spawn.getAttribute("npc_id")));
						spots += spawn.getElementsByTagName("spot").getLength();
					}
				}
			}
		}
		assertFalse(npcIds.isEmpty(), fileName + " must declare CONQUEST spawns for conquest id " + conquestId);
		return new EventSpawn(Set.copyOf(worlds), Set.copyOf(npcIds), spots);
	}

    /** Reads every npc id declared by kill-npc events of the quest XML. */
	private static Set<Integer> questKillTargets(int questId) throws Exception {
		Set<Integer> targets = new LinkedHashSet<>();
		for (Element event : elements(parse(QUESTS.resolve(questId + ".xml")).getElementsByTagName("kill-npc"))) {
			for (String token : event.getAttribute("npc-ids").split("\\s+")) {
				if (!token.isEmpty()) {
					targets.add(Integer.parseInt(token));
				}
			}
		}
		assertFalse(targets.isEmpty(), "quest " + questId + " must declare kill targets");
		return Set.copyOf(targets);
	}

	/** 读取排程表中已登记的征服活动 id。 / Reads conquest ids registered in the cron schedule. */
	private static Set<Integer> scheduledConquestIds() throws Exception {
		Set<Integer> ids = new LinkedHashSet<>();
		for (Element conquest : elements(parse(SCHEDULE).getElementsByTagName("conquest"))) {
			if (conquest.getElementsByTagName("offeringTime").getLength() > 0) {
				ids.add(Integer.parseInt(conquest.getAttribute("id")));
			}
		}
		return Set.copyOf(ids);
	}

	/** 解析生产 XML。 / Parses a production XML file. */
	private static org.w3c.dom.Document parse(Path path) throws Exception {
		return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(path.toFile());
	}

	/** 将 NodeList 转成元素列表。 / Converts a NodeList into a list of elements. */
	private static List<Element> elements(NodeList nodes) {
		List<Element> elements = new ArrayList<>();
		for (int index = 0; index < nodes.getLength(); index++) {
			elements.add((Element) nodes.item(index));
		}
		return elements;
	}

	/** 返回 left 中不属于 right 的 id。 / Returns the ids of left that are absent from right. */
	private static Set<Integer> minus(Set<Integer> left, Set<Integer> right) {
		Set<Integer> result = new LinkedHashSet<>(left);
		result.removeAll(right);
		return result;
	}

	/** 单个征服活动刷怪块的契约数据。 / Contract data of one conquest event spawn block. */
	private record EventSpawn(Set<Integer> mapIds, Set<Integer> npcIds, int spots) {
	}
}
