package com.aionemu.gameserver.questEngine.tablelane;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;

import com.aionemu.gameserver.questEngine.retail.RetailLedgerRows;

/**
 * 链式接取发放边资源门（2026-10-08 `acquire=none` 缺口修复批）。
 * <p>
 * 冻结点：① 资源 = DD `none` 行 ∩ 仓库内有链路证据者（原版 {@code finished_quest_cond} 或退役
 * XML/handler 证据），恰 34 行、两证据桶 26 + 8；② 运行期注册面 = 资源 − 6 个 XML_RETENTION 行
 * （属 typed 车道）− 1 个冻结行（20032 落点别名原版内在缺失）⇒ 恰 27 行；③ 全部前序 ∈ retention
 * 宇宙；④ 5 个带 {@code unfinished_quest_cond} 的链式行条件词法 = {@code Q<id>} 且目标行在原版表。
 * <p>
 * Gate for the chain-acquire edge resource: the file must equal the evidence-derived none-row set
 * (34 rows, 26 + 8 by provenance), the runtime must register exactly 27 successors (drops = six
 * XML_RETENTION rows plus the frozen 20032), every predecessor must exist in the retention universe,
 * and the unfinished-condition tokens of the five carriers must be plain in-repo quest ids.
 */
class QuestChainAcquireResourceGateTest {

	private static final String DD_TABLE = "/aion/data/static_data/quest/retail/data_driven_quest.xml";
	private static final String RETENTION = "/quest/retail-xml-retention.xml";
	private static final String QUEST_XML = "/aion/data/static_data/quest/retail/quest.xml";

	/** 运行期注册面冻结值（27）。 / The frozen registered-successor set. */
	private static final Set<Integer> REGISTERED = Set.of(
		10011, 10033, 10034, 10035, 10111, 10113, 10501, 10502, 10503, 10504, 10505, 10506, 10507, 10526,
		20011, 20033, 20034, 20111, 20113, 20501, 20502, 20503, 20504, 20505, 20506, 20507, 20526);

	/** 仅退役证据（原版 quest.xml 无 finished 条件）的 8 行冻结表。 / The eight retired-evidence rows. */
	private static final Map<Integer, List<Integer>> RETIRED_EVIDENCE = Map.of(
		10032, List.of(10031),
		10033, List.of(10032),
		10034, List.of(10033),
		10035, List.of(10031, 10032, 10033, 10034),
		20032, List.of(20031),
		20033, List.of(20032),
		20034, List.of(20033),
		20035, List.of(20031, 20032, 20033, 20034));

	/** 带 {@code unfinished_quest_cond} 的链式行（原版列）。 / The chain rows carrying unfinished conditions. */
	private static final Map<Integer, List<Integer>> UNFINISHED_CONDITIONS = Map.of(
		10033, List.of(10025, 14062),
		10034, List.of(10025, 14062),
		10035, List.of(10025, 14062),
		20033, List.of(20025, 24062),
		20034, List.of(20025, 24062));

	private static ChainAcquireEdges edges;
	private static Set<Integer> ddNoneIds;
	private static Map<Integer, String> retentionOwners;
	private static String questXml;

	@BeforeAll
	static void loadFixtures() throws Exception {
		edges = ChainAcquireEdges.load(resource(ChainAcquireEdges.TABLE_RESOURCE));
		DataDrivenQuestTable table = DataDrivenQuestTable.load(resource(DD_TABLE));
		ddNoneIds = new TreeSet<>();
		for (int questId : table.questIds()) {
			if ("none".equals(table.find(questId).orElseThrow().acquireKind())) {
				ddNoneIds.add(questId);
			}
		}
		retentionOwners = new LinkedHashMap<>();
		for (Element row : RetailLedgerRows.rows(RETENTION, "quest")) {
			String questId = RetailLedgerRows.cell(row, "quest_id");
			String owner = RetailLedgerRows.cell(row, "owner");
			if (questId != null && questId.chars().allMatch(Character::isDigit) && owner != null) {
				retentionOwners.put(Integer.parseInt(questId), owner);
			}
		}
		questXml = new String(resource(QUEST_XML).readAllBytes(), StandardCharsets.UTF_8);
	}

	/** ① 资源良构 + 两证据桶冻结（26 原版条件 + 8 退役证据），前序不得自环/重复。 */
	@Test
	void resourceRowsAreUniqueWellFormedAndCarryProvenance() {
		assertEquals(34, edges.all().size(), "资源行数冻结");
		Map<ChainAcquireEdges.Source, Integer> buckets = new java.util.EnumMap<>(ChainAcquireEdges.Source.class);
		for (ChainAcquireEdges.Edge edge : edges.all().values()) {
			assertFalse(edge.evidence().isBlank(), "证据非空：" + edge.successor());
			assertFalse(edge.predecessors().isEmpty(), "前序非空：" + edge.successor());
			assertFalse(edge.predecessors().contains(edge.successor()), "不得自环：" + edge.successor());
			assertEquals(edge.predecessors().stream().distinct().count(), edge.predecessors().size(),
				"前序不得重复：" + edge.successor());
			buckets.merge(edge.source(), 1, Integer::sum);
		}
		assertEquals(Map.of(ChainAcquireEdges.Source.RETAIL_FINISHED_COND, 26,
			ChainAcquireEdges.Source.RETIRED_XML, 2,
			ChainAcquireEdges.Source.RETIRED_HANDLER, 6), buckets, "证据桶冻结（26 + 2 + 6）");
	}

	/**
	 * ② 后继集合 = 证据推导集：DD `none` 行里「原版 quest.xml 有 finished 条件」∪「退役证据冻结表」
	 * ——资源不得多行也不得少行（无证据的 none 行必须留在面外）。
	 */
	@Test
	void successorSetIsExactlyTheEvidenceDerivedCandidates() {
		Set<Integer> expected = new TreeSet<>();
		for (int questId : ddNoneIds) {
			if (!finishedConditions(questId).isEmpty() || RETIRED_EVIDENCE.containsKey(questId)) {
				expected.add(questId);
			}
		}
		assertEquals(expected, edges.successors(), "资源后继 = 证据推导集");
		for (int successor : edges.successors()) {
			assertTrue(ddNoneIds.contains(successor), "资源后继必须是 DD none 行：" + successor);
		}
		// 退役证据行逐条对拍（原版 quest.xml 无 finished 条件，证据在 git 历史）。
		for (Map.Entry<Integer, List<Integer>> entry : RETIRED_EVIDENCE.entrySet()) {
			assertTrue(finishedConditions(entry.getKey()).isEmpty(),
				"退役证据行的原版行不得含 finished 条件：" + entry.getKey());
			assertEquals(entry.getValue(), edges.edge(entry.getKey()).orElseThrow().predecessors(),
				"退役证据前序冻结：" + entry.getKey());
		}
		// 原版条件行逐条对拍。
		for (int successor : edges.successors()) {
			List<Integer> finished = finishedConditions(successor);
			if (!finished.isEmpty()) {
				assertEquals(finished, edges.edge(successor).orElseThrow().predecessors(),
					"原版 finished 条件逐行对拍：" + successor);
				assertEquals(ChainAcquireEdges.Source.RETAIL_FINISHED_COND, edges.edge(successor).orElseThrow().source(),
					"证据轴 = retail-finished-cond：" + successor);
			}
		}
	}

	/** ③ 运行期过滤：注册面 = 资源 − 6 个 XML_RETENTION 行 − 冻结行 20032。 */
	@Test
	void runtimeFilterDropsExactlyTheXmlRetainedAndFrozenRows() {
		DataDrivenNativeRuntime production = DataDrivenNativeRuntime.instance();
		assertEquals(REGISTERED, production.chainAcquireSuccessors(), "运行期注册面冻结 27 行");
		Set<Integer> dropped = new TreeSet<>(edges.successors());
		dropped.removeAll(production.chainAcquireSuccessors());
		Set<Integer> expectedDrops = new TreeSet<>();
		for (int successor : edges.successors()) {
			if ("XML_RETENTION".equals(retentionOwners.get(successor))
					|| production.frozenQuestIds().containsKey(successor)) {
				expectedDrops.add(successor);
			}
		}
		assertEquals(expectedDrops, dropped,
			"被过滤集 = XML_RETENTION 行 ∪ 冻结行（" + expectedDrops + "）");
		assertTrue(production.frozenQuestIds().containsKey(20032), "20032 冻结（亚斯莫链死边前提）");
	}

	/** ④ 全部前序 ∈ retention 宇宙（防错字/防悬空边）。 */
	@Test
	void predecessorsExistInTheRetentionUniverse() {
		for (ChainAcquireEdges.Edge edge : edges.all().values()) {
			for (int predecessor : edge.predecessors()) {
				assertNotNull(retentionOwners.get(predecessor),
					"前序必须在 retention 宇宙：successor " + edge.successor() + " predecessor " + predecessor);
			}
		}
	}

	/** ⑤ unfinished 条件词法 = {@code Q<id>} 且目标行在原版表（运行期解析永不落 0）。 */
	@Test
	void unfinishedConditionTokensResolveToPlainRetailIds() {
		for (Map.Entry<Integer, List<Integer>> entry : UNFINISHED_CONDITIONS.entrySet()) {
			Matcher matcher = Pattern.compile("<unfinished_quest_cond\\d+>(\\w+)</unfinished_quest_cond\\d+>")
				.matcher(rowBlock(entry.getKey()));
			List<Integer> parsed = new ArrayList<>();
			while (matcher.find()) {
				assertTrue(matcher.group(1).matches("Q\\d+"), "unfinished 词法 = Q<id>：" + matcher.group(1));
				parsed.add(Integer.parseInt(matcher.group(1).substring(1)));
			}
			assertEquals(entry.getValue(), parsed, "unfinished 条件冻结：" + entry.getKey());
			for (int condition : parsed) {
				// 目标行必须在原版 quest.xml（哨兵行如 10025/20025 不在 retention 保留清单内，属正常）。
				// The referenced row must exist in retail quest.xml (sentinel rows such as 10025/20025 sit
				// outside the retention manifest, which is expected).
				assertFalse(rowBlock(condition).isEmpty(), "unfinished 目标行在原版表：" + condition);
			}
		}
	}

	/** 原版 quest.xml 某行的 finished_quest_condN 解析（Q<digits> 词法）。 / Parses the retail finished conditions. */
	private static List<Integer> finishedConditions(int questId) {
		List<Integer> conditions = new ArrayList<>();
		Matcher matcher = Pattern.compile("<finished_quest_cond\\d+>(\\w+)</finished_quest_cond\\d+>")
			.matcher(rowBlock(questId));
		while (matcher.find()) {
			String token = matcher.group(1);
			assertTrue(token.matches("Q\\d+"), "finished 词法 = Q<id>：" + token);
			conditions.add(Integer.parseInt(token.substring(1)));
		}
		return conditions;
	}

	/** quest.xml 某 id 的整段行文本（无 = 空串）。 / The raw row block ("" when absent). */
	private static String rowBlock(int questId) {
		Matcher matcher = Pattern.compile("<quest>(.*?)</quest>", Pattern.DOTALL).matcher(questXml);
		while (matcher.find()) {
			String block = matcher.group(1);
			if (block.contains("<id>" + questId + "</id>")) {
				return block;
			}
		}
		return "";
	}

	private static InputStream resource(String path) {
		InputStream input = QuestChainAcquireResourceGateTest.class.getResourceAsStream(path);
		assertNotNull(input, "missing resource " + path);
		return input;
	}
}
