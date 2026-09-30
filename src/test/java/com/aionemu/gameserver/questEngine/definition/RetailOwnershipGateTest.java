package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.aionemu.gameserver.questEngine.retail.RetailSimpleHuntTable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 归属门禁（提示词 §4.E.1/E.5）：每个生产任务的定义来源唯一且与保留清单一致。
 * <ul>
 * <li>{@code retail-xml-retention.tsv} 必须覆盖生产任务全集（XML catalog ∪ 真端注入），且没有 XML 的任务必须是 {@code RETAIL_TABLE}；</li>
 * <li>{@code RETAIL_TABLE} 行必须声明家族；保留行必须给出已知原因
 * （SCRIPTED / NO_TABLE / SEMANTIC_GAP:*）；</li>
 * <li>已入仓家族（当前 SimpleHunt）的清单判定必须与真端表逐任务一致。</li>
 * </ul>
 * Ownership gate: exactly one owner per catalog quest, consistent with the retail tables.
 */
class RetailOwnershipGateTest {

	private static final String CATALOG = "/aion/data/static_data/quest/definitions/quest_definition_catalog.xml";
	private static final String RETENTION = "/quest/retail-xml-retention.tsv";
	private static final String SIMPLE_HUNT_TABLE = "/aion/data/static_data/quest/retail/Quest_SimpleHunt.xml";
	private static final String SIMPLE_TALK_TABLE = "/aion/data/static_data/quest/retail/Quest_SimpleTalk.xml";

	/** 清单允许的保留原因前缀。 / Allowed retention reason prefixes. */
	/**
	 * 清单允许的保留原因前缀。{@code FAMILY_PENDING} = 真端表有行但该族驱动尚未实现
	 * （禁止虚报为 RETAIL_TABLE，见 build_retention_list.py 的 IMPLEMENTED_FAMILIES）；
	 * {@code ADJUDICATED:<码>} = 缺口批逐行裁定的保留行（冒号后必须仍是原编译器拒绝码，
	 * 由家族门 fail-closed 复核"裁定行必须仍以同码被拒"）。
	 * Allowed retention reasons; {@code FAMILY_PENDING} marks a retail row whose family driver is not
	 * implemented yet; {@code ADJUDICATED:<code>} marks a per-row adjudicated retention whose embedded
	 * compiler code must still reject the row (fail-closed, checked by the family gates).
	 */
	private static final Set<String> RETENTION_REASONS =
		Set.of("SCRIPTED", "NO_TABLE", "SEMANTIC_GAP", "FAMILY_PENDING", "ADJUDICATED");
	/** 已入仓真端表（后续族切片逐个加入）。 / In-repo retail tables so far. */
	private static final Set<String> IN_REPO_FAMILIES = Set.of("SimpleHunt", "SimpleTalk");

	private static Map<Integer, String> catalogIds;
	private static Map<Integer, Row> rows;
	private static Set<Integer> simpleHuntRows;
	private static Set<Integer> simpleTalkRows;

	record Row(int questId, String owner, String family, String reason) {
	}

	@BeforeAll
	static void loadFixtures() throws Exception {
		catalogIds = loadCatalog();
		rows = loadRetention();
		simpleHuntRows = loadSimpleHuntIds();
		simpleTalkRows = loadRetailIds(SIMPLE_TALK_TABLE);
	}

	@Test
	void retentionListCoversProductionUniverse() {
		// 已退役（真端驱动）的任务不再出现在 XML catalog，但仍是生产任务：
		// 清单 = XML 目录 ∪ 真端注入，且"没有 XML 的任务必须由真端表驱动"。
		// The retention list is the production universe: XML catalog plus retail-injected quests.
		assertTrue(rows.keySet().containsAll(catalogIds.keySet()),
			"retention list must classify every XML-catalog quest exactly once");
		assertEquals(6224, rows.size(), "retention list must keep the frozen production universe");
		List<String> orphanXml = catalogIds.keySet().stream()
			.filter(id -> !rows.containsKey(id))
			.map(String::valueOf)
			.limit(10)
			.toList();
		assertTrue(orphanXml.isEmpty(), "XML-catalog quests missing from the retention list: " + orphanXml);
		List<String> injectedNotRetail = rows.keySet().stream()
			.filter(id -> !catalogIds.containsKey(id))
			.filter(id -> !"RETAIL_TABLE".equals(rows.get(id).owner()))
			.map(String::valueOf)
			.limit(10)
			.toList();
		assertTrue(injectedNotRetail.isEmpty(),
			"quests without XML must be retail-driven: " + injectedNotRetail);
	}

	@Test
	void rowsAreWellFormed() {
		List<String> problems = rows.entrySet().stream()
			.filter(entry -> !wellFormed(entry.getValue()))
			.map(entry -> entry.getKey() + " -> " + entry.getValue())
			.toList();
		assertTrue(problems.isEmpty(), () -> "malformed retention rows: " + problems);
	}

	@Test
	void simpleTalkFamilyTableMatchesList() {
		// SimpleTalk：真端表有行 ⇔ 清单 family=SimpleTalk（RETAIL_TABLE/OK 或 XML_RETENTION/SEMANTIC_GAP:*）。
		// 与 SimpleHunt 同口径：family 记录数据来源，owner 记录能否由真端驱动。
		List<String> problems = new java.util.ArrayList<>();
		for (Map.Entry<Integer, Row> entry : rows.entrySet()) {
			int questId = entry.getKey();
			Row row = entry.getValue();
			boolean listNamesSimpleTalk = "SimpleTalk".equals(row.family());
			boolean tableHas = simpleTalkRows.contains(questId);
			if (listNamesSimpleTalk != tableHas) {
				problems.add(questId + " list=" + row.owner() + "/" + row.family() + " table=" + tableHas);
			}
		}
		assertTrue(problems.isEmpty(), () -> "SimpleTalk ownership mismatch: " + problems);
	}

	@Test
	void inRepoFamilyTablesMatchList() {
		// SimpleHunt：真端表有行 ⇔ 清单行 family=SimpleHunt（RETAIL_TABLE/OK 或 XML_RETENTION/SEMANTIC_GAP）。
		// 其余家族当前一律 FAMILY_PENDING（驱动未实现），因此"family 列 = 真端表有行"这条不变量
		// 对已实现家族成立，也对未实现家族成立（family 记录数据来源，owner 记录是否可驱动）。
		// The retail table row must match any list row that names the SimpleHunt family.
		List<String> problems = new java.util.ArrayList<>();
		for (Map.Entry<Integer, Row> entry : rows.entrySet()) {
			int questId = entry.getKey();
			Row row = entry.getValue();
			boolean listNamesSimpleHunt = "SimpleHunt".equals(row.family());
			boolean tableHas = simpleHuntRows.contains(questId);
			if (listNamesSimpleHunt != tableHas) {
				problems.add(questId + " list=" + row.owner() + "/" + row.family() + " table=" + tableHas);
			}
		}
		assertTrue(problems.isEmpty(), () -> "SimpleHunt ownership mismatch: " + problems);
	}

	private static boolean wellFormed(Row row) {
		if ("RETAIL_TABLE".equals(row.owner())) {
			return row.reason().equals("OK") && List.of("SimpleHunt", "SimpleTalk", "CombineTask",
				"SimpleCollectItem", "SimpleUseItem", "SimpleItemPlay", "SimpleSerialHunt", "DataDriven")
				.contains(row.family());
		}
		if ("XML_RETENTION".equals(row.owner())) {
			String reason = row.reason();
			int colon = reason.indexOf(':');
			String base = colon < 0 ? reason : reason.substring(0, colon);
			return RETENTION_REASONS.contains(base);
		}
		return "SERVER_ONLY".equals(row.owner());
	}

	private static Map<Integer, String> loadCatalog() throws Exception {
		String text = new String(readAll(CATALOG), StandardCharsets.UTF_8);
		Map<Integer, String> ids = new HashMap<>();
		Matcher matcher = Pattern
			.compile("<definition id=\"(\\d+)\"").matcher(text);
		while (matcher.find()) {
			ids.put(Integer.parseInt(matcher.group(1)), "catalog");
		}
		// 迁移开始后 XML 目录会随退役收敛：目录 + 保留清单里的退役 id 必须仍等于冻结的生产全集。
		// Retired quests leave the catalog, so catalog plus the retention manifest must cover the universe.
		Set<Integer> retired = retiredFixtureIds();
		Set<Integer> overlap = new HashSet<>(ids.keySet());
		overlap.retainAll(retired);
		assertTrue(overlap.isEmpty(), "catalog must not keep retired quests: " + overlap);
		assertEquals(6224, ids.size() + retired.size(),
			"XML catalog plus retired fixtures must cover the production quest universe");
		return ids;
	}

	/**
	 * 已退役任务 id：保留清单 owner=RETAIL_TABLE（退役 XML 不进仓，历史内容在 git 里可回溯）。
	 * Retired quest ids come from the retention manifest, not from frozen XML copies.
	 */
	private static Set<Integer> retiredFixtureIds() {
		return RetiredQuestIds.all();
	}

	private static Map<Integer, Row> loadRetention() throws IOException {
		Map<Integer, Row> rows = new HashMap<>();
		try (BufferedReader reader = new BufferedReader(
			new InputStreamReader(open(RETENTION), StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.startsWith("#") || line.isBlank()) {
					continue;
				}
				String[] parts = line.split("\t", -1);
				assertEquals(5, parts.length, "retention row must have 5 columns: " + line);
				Row row = new Row(Integer.parseInt(parts[0]), parts[1], parts[2], parts[3]);
				Row previous = rows.put(row.questId(), row);
				if (previous != null) {
					throw new AssertionError("duplicate owner row for quest " + row.questId());
				}
			}
		}
		return rows;
	}

	/** 真端模板表里的任务 id（XML 形式，与 SimpleHunt 行同构）。 / Quest ids of a retail table. */
	private static Set<Integer> loadRetailIds(String resource) throws IOException {
		String text = new String(readAll(resource), StandardCharsets.UTF_8);
		return Pattern.compile("<id id=\"(\\d+)\"").matcher(text).results()
			.map(match -> Integer.parseInt(match.group(1)))
			.collect(Collectors.toSet());
	}

	private static Set<Integer> loadSimpleHuntIds() throws IOException {
		RetailSimpleHuntTable table;
		try (InputStream input = open(SIMPLE_HUNT_TABLE)) {
			table = RetailSimpleHuntTable.load(input);
		}
		return table.questIds();
	}

	private static byte[] readAll(String resource) throws IOException {
		try (InputStream input = open(resource)) {
			return input.readAllBytes();
		}
	}

	private static InputStream open(String resource) throws IOException {
		InputStream input = RetailOwnershipGateTest.class.getResourceAsStream(resource);
		assertNotNull(input, "missing resource " + resource);
		return input;
	}
}
