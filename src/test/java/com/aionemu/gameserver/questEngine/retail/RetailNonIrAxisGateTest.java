package com.aionemu.gameserver.questEngine.retail;

import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
import com.aionemu.gameserver.questEngine.definition.ProductionQuestDefinitions;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 非 IR 轴统一登记门禁（P0c-11）：采纳真端形状会连带改变"节点/转换之外"的轴
 * （服务器侧封顶、旧存档自愈边、前置条件两表达），这些轴不在家族等价普查里，散落登记曾导致
 * 采纳后静默丢失（P0c-8a/8c 判例）。本门禁把三处登记合并在
 * {@code /quest/retail-non-ir-axis-registry.tsv}（由 {@code p0c11_build_non_ir_registry.py} 生成）
 * 并锁四条不变量：
 * <ol>
 *   <li>统一登记表与两个逐任务源（封顶清单 / 自愈边登记）**逐任务新鲜一致**——采纳切片落地后必须重跑生成器；</li>
 *   <li>登记了 {@code CAP_LEVEL} 的任务**不得已采纳**（owner=RETAIL_TABLE 的行生产==真端 UNLIMITED，
 *       封顶只可能存在于保留 XML 行；P0c-8c/9 的 8060x 先例）；</li>
 *   <li>登记了 {@code LEGACY_SAVE_HEAL} 的任务必须已采纳，且其真端定义**不含任何 EnterWorld 路由**
 *       （自愈边按 P0c-6 先例登记而不编译）；</li>
 *   <li>{@code PREREQ_DUAL_EXPRESSION} 轴级行在案，且 M1 分歧表两表达轴均有登记行。</li>
 * </ol>
 * Non-IR axis registry gate: adoption can change axes outside the node/transition IR (server caps,
 * legacy save-heal edges, dual prerequisite expression). This gate merges the three ledgers into one
 * registry and locks freshness plus per-axis consistency with the production state.
 */
class RetailNonIrAxisGateTest {

	private static final String REGISTRY = "/quest/retail-non-ir-axis-registry.tsv";
	private static final String CAP_LEDGER = "/quest/quest-start-metadata-retail-cap-exceptions.tsv";
	private static final String HEAL_LEDGER = "/quest/retail-legacy-save-normalization.tsv";
	private static final String DIVERGENCES = "/quest/retail-metadata-divergences.tsv";
	private static final String RETENTION = "/quest/retail-xml-retention.tsv";

	private static Map<Integer, String> capLedger;
	private static Map<Integer, Integer> healLedgerEdgeCounts;
	private static Map<Integer, String> registryCap;
	private static Map<Integer, String> registryHeal;
	private static boolean registryHasPrereqAxisRow;
	private static Map<Integer, String> owners;

	@BeforeAll
	static void loadFixtures() throws Exception {
		capLedger = intKeyedRows(CAP_LEDGER);
		healLedgerEdgeCounts = new java.util.TreeMap<>();
		for (String line : lines(open(HEAL_LEDGER))) {
			String[] parts = line.split("\t", -1);
			if (parts.length < 4 || !parts[0].trim().matches("\\d+")) {
				continue;
			}
			healLedgerEdgeCounts.merge(Integer.parseInt(parts[0].trim()), 1, Integer::sum);
		}
		registryCap = new java.util.TreeMap<>();
		registryHeal = new java.util.TreeMap<>();
		registryHasPrereqAxisRow = false;
		for (String line : lines(open(REGISTRY))) {
			String[] parts = line.split("\t", -1);
			if (parts.length < 5) {
				continue;
			}
			if ("PREREQ_DUAL_EXPRESSION".equals(parts[1])) {
				registryHasPrereqAxisRow = "*".equals(parts[0].trim());
				continue;
			}
			if (!parts[0].trim().matches("\\d+")) {
				continue;
			}
			int questId = Integer.parseInt(parts[0].trim());
			if ("CAP_LEVEL".equals(parts[1])) {
				registryCap.put(questId, parts[3]);
			} else if ("LEGACY_SAVE_HEAL".equals(parts[1])) {
				registryHeal.put(questId, parts[3]);
			}
		}
		owners = new HashMap<>();
		for (String line : lines(open(RETENTION))) {
			if (line.startsWith("#") || line.isBlank()) {
				continue;
			}
			String[] parts = line.split("\t", -1);
			if (parts.length > 3) {
				owners.put(Integer.parseInt(parts[0]), parts[1]);
			}
		}
	}

	@Test
	void registryMatchesBothQuestLevelSources() {
		assertEquals(capLedger.keySet(), registryCap.keySet(),
			() -> "CAP_LEVEL rows must mirror " + CAP_LEDGER + " exactly; regenerate via"
				+ " p0c11_build_non_ir_registry.py");
		assertEquals(healLedgerEdgeCounts.keySet(), registryHeal.keySet(),
			() -> "LEGACY_SAVE_HEAL rows must mirror " + HEAL_LEDGER + " exactly; regenerate via"
				+ " p0c11_build_non_ir_registry.py");
	}

	@Test
	void cappedQuestsAreNeverRetailDriven() {
		List<Integer> violations = new ArrayList<>();
		for (int questId : new TreeSet<>(capLedger.keySet())) {
			String owner = owners.get(questId);
			// 采纳（owner=RETAIL_TABLE）后生产==真端 UNLIMITED，封顶随旧 XML 一起失效；
			// 登记行必须随采纳切片同步移除（P0c-8c/9 的 8060x 先例）。
			// Adopted rows run UNLIMITED like the retail row; the ledger row must retire with the XML.
			if (owner == null || !"XML_RETENTION".equals(owner)) {
				violations.add(questId);
			}
		}
		assertTrue(violations.isEmpty(), () -> "capped quests must stay XML-retained: " + violations);
	}

	@Test
	void saveHealQuestsAreRetailDrivenWithoutEnterWorldRoutes() throws Exception {
		List<String> violations = new ArrayList<>();
		for (int questId : new TreeSet<>(healLedgerEdgeCounts.keySet())) {
			String owner = owners.get(questId);
			if (!"RETAIL_TABLE".equals(owner)) {
				violations.add(questId + ": owner=" + owner);
				continue;
			}
			CompiledQuestDefinition definition = ProductionQuestDefinitions.definition(questId);
			long enterWorldRoutes = definition.definition().transitions().stream()
				.filter(transition -> transition.event() instanceof QuestEvent.EnterWorld)
				.count();
			if (enterWorldRoutes > 0) {
				violations.add(questId + ": enterWorldRoutes=" + enterWorldRoutes);
			}
		}
		assertTrue(violations.isEmpty(), () -> "save-heal quests must be retail-driven without enter-world"
			+ " routes: " + violations);
	}

	@Test
	void prerequisiteDualExpressionAxisIsRegistered() throws Exception {
		assertTrue(registryHasPrereqAxisRow, "registry must carry the axis-level PREREQ_DUAL_EXPRESSION row");
		Map<String, Integer> axisRows = new HashMap<>();
		for (String line : lines(open(DIVERGENCES))) {
			if (line.startsWith("#") || line.isBlank()) {
				continue;
			}
			String[] parts = line.split("\t", -1);
			if (parts.length > 1 && (parts[1].equals("startConditions") || parts[1].equals("prerequisites"))) {
				axisRows.merge(parts[1], 1, Integer::sum);
			}
		}
		assertTrue(axisRows.getOrDefault("startConditions", 0) > 0,
			"M1 divergences must register start-conditions rows");
		assertTrue(axisRows.getOrDefault("prerequisites", 0) > 0,
			"M1 divergences must register prerequisites rows");
	}

	private static Map<Integer, String> intKeyedRows(String resource) throws Exception {
		Map<Integer, String> rows = new java.util.TreeMap<>();
		for (String line : lines(open(resource))) {
			String[] parts = line.split("\t", -1);
			if (!parts[0].trim().matches("\\d+")) {
				continue;
			}
			rows.put(Integer.parseInt(parts[0].trim()), parts.length > 1 ? parts[1] : "");
		}
		return rows;
	}

	private static List<String> lines(InputStream input) throws Exception {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
			return reader.lines().toList();
		}
	}

	private static InputStream open(String resource) {
		InputStream input = RetailNonIrAxisGateTest.class.getResourceAsStream(resource);
		assertNotNull(input, "missing resource " + resource);
		return input;
	}
}
