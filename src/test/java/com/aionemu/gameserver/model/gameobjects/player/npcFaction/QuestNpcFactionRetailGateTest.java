package com.aionemu.gameserver.model.gameobjects.player.npcFaction;

import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
import com.aionemu.gameserver.questEngine.definition.QuestCatalog;
import com.aionemu.gameserver.questEngine.definition.QuestDefinitionCatalogManifest;

import org.junit.jupiter.api.Test;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 阵营日常归属门禁：生产 XML 必须声明 {@code npc-faction-id}，且真的能进入该阵营的日常候选池。
 * <p>背景：{@link NpcFactions#sendDailyQuest()} 的候选池按 {@code metadata.npcFactionId()} 过滤；
 * 缺少声明的任务永远不会被推送，而 {@code PlayerQuestStartEligibilityPort} 又会因此跳过阵营校验。
 * 曾出现 218 个任务（含 3505x Alabaster Order 日常）整体缺声明的情况。
 * <p>Gate: faction dailies must declare their faction owner, and must actually land in that faction's
 * daily candidate pool.
 */
class QuestNpcFactionRetailGateTest {
	private static final String CONTRACT_RESOURCE = "/quest/quest-npc-faction-retail-contract.tsv";
	private static final String ROTATION_RESOURCE =
		"/aion/data/static_data/npc_factions/npc_factions_quest.xml";
	/** 合同快照规模：防止基线被误删或生成脚本漏项。 / Guard against silent baseline shrink. */
	private static final int EXPECTED_CONTRACT_ROWS = 253;

	private static QuestCatalog catalog() {
		return QuestDefinitionCatalogManifest.compile(
			Path.of("src/main/resources/aion/data/static_data/quest_definition"));
	}

	@Test
	void factionDailiesDeclareExactlyTheirReviewedFaction() {
		QuestCatalog catalog = catalog();
		Map<Integer, Integer> contract = readings();
		assertEquals(EXPECTED_CONTRACT_ROWS, contract.size(),
			"faction contract snapshot must keep its reviewed coverage");
		for (Map.Entry<Integer, Integer> entry : contract.entrySet()) {
			CompiledQuestDefinition compiled = catalog.findExecutable(entry.getKey())
				.orElseThrow(() -> new AssertionError("faction quest " + entry.getKey()
					+ " is not an executable owner"));
			assertEquals(entry.getValue(), compiled.definition().metadata().npcFactionId(),
				"quest " + entry.getKey() + " must declare its reviewed faction owner");
		}
		// 反向：生产目录里任何声明了阵营的任务都必须在评审基线上，禁止静默新增未评审归属。
		Set<Integer> undeclared = new TreeSet<>();
		for (CompiledQuestDefinition compiled : catalog.executables()) {
			int factionId = compiled.definition().metadata().npcFactionId();
			if (factionId != 0 && !contract.containsKey(compiled.id())) {
				undeclared.add(compiled.id());
			}
		}
		assertEquals(Set.of(), undeclared,
			"quests declaring a faction owner must be part of the reviewed contract snapshot");
	}

	@Test
	void everyContractQuestLandsInItsFactionDailyPool() {
		QuestCatalog catalog = catalog();
		Map<Integer, Integer> contract = readings();
		Map<Integer, Set<Integer>> expectedByFaction = new TreeMap<>();
		contract.forEach((questId, factionId) ->
			expectedByFaction.computeIfAbsent(factionId, ignored -> new LinkedHashSet<>()).add(questId));
		for (Map.Entry<Integer, Set<Integer>> entry : expectedByFaction.entrySet()) {
			Set<Integer> pool = new TreeSet<>(NpcFactions.canonicalDailyQuestCandidates(
				catalog, entry.getKey(), id -> true, id -> true, id -> true));
			assertEquals(new TreeSet<>(entry.getValue()), pool,
				"faction " + entry.getKey() + " daily pool must contain exactly its reviewed owners");
		}
	}

	/**
	 * 轮换表要么没有该任务的记录（{@code isActiveOn} 视为每天可发），要么必须至少有一个星期位；
	 * 全 0 掩码等于永远轮不到，且必须与合同阵营一致。
	 * <p>The rotation table either omits the quest (treated as always active) or must enable at least one
	 * weekday; an all-zero mask can never be rotated in, and any row must match the reviewed faction.
	 */
	@Test
	void everyContractQuestCanBeRotatedIn() {
		Map<Integer, Integer> contract = readings();
		Map<Integer, int[]> rotation = rotationRows();
		for (Map.Entry<Integer, Integer> entry : contract.entrySet()) {
			int[] row = rotation.get(entry.getKey());
			if (row == null) {
				continue;
			}
			assertEquals(entry.getValue().intValue(), row[0],
				"quest " + entry.getKey() + " rotation row must match its declared faction");
			assertTrue(row[1] > 0,
				"quest " + entry.getKey() + " has an all-zero weekday mask and can never be rotated in");
		}
	}

	/** 解析轮换表：quest_id -> [factionId, 星期位总和]。 / Rotation rows as [factionId, weekday bit sum]. */
	private static Map<Integer, int[]> rotationRows() {
		Map<Integer, int[]> rows = new LinkedHashMap<>();
		try (InputStream input = Objects.requireNonNull(
			QuestNpcFactionRetailGateTest.class.getResourceAsStream(ROTATION_RESOURCE), ROTATION_RESOURCE)) {
			var document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(input);
			var nodes = document.getElementsByTagName("npc_faction_quest");
			for (int index = 0; index < nodes.getLength(); index++) {
				var element = (org.w3c.dom.Element) nodes.item(index);
				int days = 0;
				for (String day : new String[] {"mon", "tue", "wed", "thu", "fri", "sat", "sun"}) {
					days += "1".equals(element.getAttribute(day)) ? 1 : 0;
				}
				rows.put(Integer.parseInt(element.getAttribute("quest_id")),
					new int[] {Integer.parseInt(element.getAttribute("faction_id")), days});
			}
		} catch (Exception e) {
			throw new AssertionError("unable to read " + ROTATION_RESOURCE, e);
		}
		return rows;
	}

	private static Map<Integer, Integer> readings() {
		Map<Integer, Integer> rows = new LinkedHashMap<>();
		try (InputStream input = Objects.requireNonNull(
			QuestNpcFactionRetailGateTest.class.getResourceAsStream(CONTRACT_RESOURCE), CONTRACT_RESOURCE);
			var reader = new java.io.BufferedReader(new java.io.InputStreamReader(input,
				java.nio.charset.StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.isBlank() || line.startsWith("#") || line.startsWith("quest_id")) {
					continue;
				}
				String[] cells = line.split("\t");
				rows.put(Integer.parseInt(cells[0]), Integer.parseInt(cells[1]));
			}
		} catch (Exception e) {
			throw new AssertionError("unable to read " + CONTRACT_RESOURCE, e);
		}
		assertTrue(rows.size() > 0, "faction contract snapshot must not be empty");
		return rows;
	}
}
