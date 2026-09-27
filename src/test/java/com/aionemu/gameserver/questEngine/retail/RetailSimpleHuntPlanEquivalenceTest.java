package com.aionemu.gameserver.questEngine.retail;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 表驱动 ↔ 现有 XML 定义 的等价门禁（SimpleHunt 第一切片）。
 * <p>
 * 数据链路：真端 {@code Quest_SimpleHunt.xml} → {@link RetailSimpleHuntPlan}（经 NPC {@code name_desc} 解析）
 * 必须与 {@code quest-definition} 的击杀计数合同一致：
 * 槽位 {@code N} 的 {@code required} 等于 {@code countN}，npc_id 集合覆盖真端怪列表，
 * 且不超出「真端怪列表 ∪ 客户端 quest_monster.csv」。合同快照由 {@code QuestSimpleHuntRetailContractTest} 共同使用。
 * <p>
 * Migration gate proving the retail table reproduces the current XML counter contract.
 */
class RetailSimpleHuntPlanEquivalenceTest {

	private static final String CONTRACT = "/quest/quest-simple-hunt-retail-contract.tsv";
	private static final String TABLE = "/aion/data/static_data/quest_retail/Quest_SimpleHunt.xml";
	private static final String NPC_DIR = "/aion/data/static_data/npcs/";
	private static final List<String> NPC_TEMPLATES = List.of(
		"npc_template_200000_216188.xml", "npc_template_216189_235748.xml", "npc_template_235749_247606.xml",
		"npc_template_247607_270057.xml", "npc_template_270058_286320.xml", "npc_template_286321_800030.xml",
		"npc_template_800031_834289.xml", "npc_template_834290_885645.xml");

	@Test
	void retailTableReproducesEveryCounterGridContractRow() throws Exception {
		RetailNpcNameIndex index = buildNpcIndex();
		assertTrue(index.size() > 80_000, "NPC name index should cover the full template set, was " + index.size());

		RetailSimpleHuntTable table = RetailSimpleHuntTableTest.load();
		List<String> violations = new ArrayList<>();
		int checked = 0;
		for (String line : contractLines()) {
			String[] cols = line.split("\t", -1);
			if (cols.length != 6 || !"COUNTER_GRID".equals(cols[2])) {
				continue;
			}
			int questId = Integer.parseInt(cols[0]);
			int slot = Integer.parseInt(cols[1]);
			int required = Integer.parseInt(cols[3]);
			Set<Integer> expected = parseInts(cols[4]);
			Set<Integer> declared = new LinkedHashSet<>(expected);
			declared.addAll(parseInts(cols[5]));
			// 计划侧按同名族闭包展开击杀目标，允许集必须用同一口径。
			// The plan expands kill targets to the display-name family, so must the allowed set.
			Set<Integer> allowed = new LinkedHashSet<>(index.withDisplayNameVariants(declared));

			RetailSimpleHuntTable.Entry entry = table.find(questId).orElse(null);
			if (entry == null) {
				violations.add("quest " + questId + " slot " + slot + ": missing retail table row");
				continue;
			}
			RetailSimpleHuntTable.Counter counter = entry.counter(slot).orElse(null);
			if (counter == null) {
				violations.add("quest " + questId + " slot " + slot + ": missing retail counter slot");
				continue;
			}
			if (counter.required() != required) {
				violations.add("quest " + questId + " slot " + slot + ": retail countN=" + counter.required()
					+ " but contract requires " + required);
				continue;
			}
			RetailSimpleHuntPlan plan = RetailSimpleHuntPlan.bind(entry, index);
			Set<Integer> resolved = plan.counter(slot).orElseThrow().npcIds();
			List<Integer> missing = expected.stream().filter(id -> !resolved.contains(id)).toList();
			List<Integer> unexpected = resolved.stream().filter(id -> !allowed.contains(id)).toList();
			if (!missing.isEmpty()) {
				violations.add("quest " + questId + " slot " + slot + ": table does not resolve " + missing);
			}
			if (!unexpected.isEmpty()) {
				violations.add("quest " + questId + " slot " + slot + ": table resolves unexpected " + unexpected);
			}
			checked++;
		}
		int checkedFinal = checked;
		assertTrue(checkedFinal > 800,
			() -> "equivalence gate should cover the counter-grid contracts, checked=" + checkedFinal);
		assertTrue(violations.isEmpty(),
			() -> "retail-table/XML counter contract differences (" + violations.size() + "):\n"
				+ String.join("\n", violations.subList(0, Math.min(violations.size(), 20))));
	}

	private static RetailNpcNameIndex buildNpcIndex() throws Exception {
		List<InputStream> streams = new ArrayList<>();
		try {
			for (String name : NPC_TEMPLATES) {
				InputStream input = RetailSimpleHuntPlanEquivalenceTest.class.getResourceAsStream(NPC_DIR + name);
				assertNotNull(input, "missing npc template " + name);
				streams.add(input);
			}
			return RetailNpcNameIndex.build(streams, RetailQuestAiNameGroupsFixture.streams());
		} finally {
			for (InputStream stream : streams) {
				stream.close();
			}
		}
	}

	private static List<String> contractLines() throws Exception {
		List<String> lines = new ArrayList<>();
		try (InputStream input = RetailSimpleHuntPlanEquivalenceTest.class.getResourceAsStream(CONTRACT)) {
			assertNotNull(input, "missing retail SimpleHunt contract snapshot");
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
				String line;
				while ((line = reader.readLine()) != null) {
					if (!line.isBlank() && !line.startsWith("#") && !line.startsWith("quest_id\t")) {
						lines.add(line);
					}
				}
			}
		}
		return lines;
	}

	private static Set<Integer> parseInts(String raw) {
		Set<Integer> ids = new HashSet<>();
		if (raw == null || raw.isBlank() || "-".equals(raw.trim())) {
			return ids;
		}
		for (String token : raw.trim().split("\\s+")) {
			if (!token.isBlank()) {
				ids.add(Integer.parseInt(token));
			}
		}
		return ids;
	}
}
