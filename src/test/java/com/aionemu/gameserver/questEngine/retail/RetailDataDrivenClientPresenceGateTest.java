package com.aionemu.gameserver.questEngine.retail;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;

import com.aionemu.gameserver.questEngine.definition.RetiredQuestIds;
import com.aionemu.gameserver.questEngine.tablelane.DataDrivenQuestTable;
import com.aionemu.gameserver.questEngine.tablelane.DataDrivenQuestTable.Kind;
import com.aionemu.gameserver.questEngine.tablelane.DataDrivenQuestTable.Row;
import com.aionemu.gameserver.questEngine.tablelane.NativeQuestOwnerResolver;

/**
 * P7 前置裁定门（计划 §10.3-#5）：DD 行按「客户端三表是否存在」分桶，并把两处裁定冻结成不可静默漂移的证据面。
 * <p>
 * ① **客户端三表皆无的 DD 行 = 表内孤行**（`p7/dd-client-presence.tsv` 逐行证据；客户端 `quest.xml` +
 * 客户端 `data_driven_quest.xml` + 客户端 `challenge_task.xml` 三表都没这个 id）：全部落在 99xxx 段、
 * 接取类别全是 `Talk`、**原版 `quest.xml` 也没有元数据行**、台账 owner `ABSENT`（无 XML、无保留条目）。
 * 原版同版客户端亦无这些行 ⇒ 玩家不可渲染 ⇒ P7（DD 切换批）不得把它们拉进路由/注册集；本门按
 * **逐元素冻结** 355 行，并断言 P7 的切换集（owner `RETAIL_TABLE`）与之零交集。
 * <p>
 * ② **80817**（活动任务）客户端三表**都有**行、原版 `quest.xml` 也有元数据 ⇒ 必须实现，不得「显式禁用」；
 * 同时其原版 Hunt 规格 `world_event_camel 100` 必须原样保留——原版 DD Hunt 是「6 位步号 + 4×6 位组槽」
 * 打包（单组上限 63），100 杀在原版会槽回绕且永不达标（`p0a/semantic-matrix-dd-wide-count.md` §2b/§2c），
 * 因此**不得**改成 10 位相机「修好」它（那是家族相机的形，不是 DD 的形）。本门冻结该算术前提。
 * <p>
 * 客户端侧的「三表皆无」由探针记录（客户端表尺寸 + sha256 见裁定文档），门内复算仓库内可复算的那一半：
 * 原版表行存在、99xxx 段、`Talk` 接取、原版 `quest.xml` 无行、owner `ABSENT`、无 XML、非退役。
 * <p>
 * Pre-P7 adjudication gate for §10.3-#5: DD rows absent from all three client tables are frozen element-by-element
 * (table-only rows, never routed), the P7 switch set must stay disjoint from them, and the retail 6-bit-per-group
 * hunt arithmetic for the event quest 80817 is frozen as-is (never "fixed" into a 10-bit camera).
 */
class RetailDataDrivenClientPresenceGateTest {

	private static final String DD_TABLE = "/aion/data/static_data/quest/retail/data_driven_quest.xml";
	private static final String QUEST_XML = "/aion/data/static_data/quest/retail/quest.xml";
	private static final String RETENTION = "/quest/retail-xml-retention.xml";
	private static final String ABSENT_FIXTURE = "/quest/retail-data-driven-client-absent.tsv";

	/** 客户端三表皆无的 DD 行数（2026-10-01 探针冻结）。 / Frozen table-only rows. */
	private static final int FROZEN_ABSENT_ROWS = 337;
	/** 原版表里被 XML 注释禁用的 DD 行（生产装载器不含）。 / Rows commented out in the retail table. */
	private static final int COMMENTED_OUT_ROWS = 18;
	/** P7 切换集规模：DD 表里 owner `RETAIL_TABLE` 的行数。 / The P7 switch set size. */
	private static final int DD_RETIRED_ROWS = 1467;
	/** 80817 的原版击杀规格（6 位组槽 × target = 100 ⇒ 原版不可完成）。 / The retail spec of 80817. */
	private static final String EVENT_HUNT_MONSTER = "world_event_camel";
	private static final int EVENT_HUNT_COUNT = 100;

	private static DataDrivenQuestTable dd;
	private static Set<Integer> frozenAbsent;
	private static Set<Integer> commentedOut;
	private static Set<Integer> questXmlIds;
	private static Map<Integer, String> owners;

	@BeforeAll
	static void loadFixtures() throws Exception {
		dd = DataDrivenQuestTable.load(resource(DD_TABLE));
		questXmlIds = questXmlIds();
		owners = retentionOwners();
		Map<String, Set<Integer>> fixture = fixtureRows();
		frozenAbsent = fixture.get("CLIENT_ABSENT_LIVE");
		commentedOut = fixture.get("COMMENTED_OUT");
	}

	/** ① 孤行冻结：逐元素、逐判据复算（表行存在 / 99xxx 段 / `Talk` / 原版 `quest.xml` 无行 / owner ABSENT / 非退役 / 无 XML）。 */
	@Test
	void clientAbsentRowsFreezeToTheTableOnlyBand() {
		assertEquals(FROZEN_ABSENT_ROWS, frozenAbsent.size(), "孤行集规模冻结（客户端三表皆无）");
		assertEquals(COMMENTED_OUT_ROWS, commentedOut.size(), "原版表注释禁用行数冻结");
		Set<Integer> xmlOnly = NativeQuestOwnerResolver.instance().xmlOnlyIds();
		for (int questId : frozenAbsent) {
			Row row = dd.find(questId).orElse(null);
			assertNotNull(row, "孤行必须仍在原版 DD 表里: " + questId);
			assertTrue(questId >= 99000 && questId <= 99999, "孤行必须落在 99xxx 段: " + questId);
			assertEquals("talk", row.acquireKind(), "孤行的接取类别冻结: " + questId);
			assertFalse(questXmlIds.contains(questId), "孤行在原版 quest.xml 里不得有元数据行: " + questId);
			assertEquals("ABSENT", owners.getOrDefault(questId, "ABSENT"),
				"孤行不得出现在保留台账（owner 必须 ABSENT）: " + questId);
			assertFalse(RetiredQuestIds.contains(questId), "孤行不得退役（否则会被 native 车道接管）: " + questId);
			assertFalse(xmlOnly.contains(questId), "孤行不得有 XML 定义（无双路径）: " + questId);
		}
	}

	/** ①-b 原版表注释禁用的 18 行：装载器不得含它们（原版自身也不装载），owner/退役/XML 三面同样冻结。 */
	@Test
	void commentedOutRowsAreNotLoadedByTheRetailLoader() {
		Set<Integer> xmlOnly = NativeQuestOwnerResolver.instance().xmlOnlyIds();
		for (int questId : commentedOut) {
			assertTrue(questId >= 99000 && questId <= 99999, "注释行必须落在 99xxx 段: " + questId);
			assertTrue(dd.find(questId).isEmpty(),
				"原版表注释禁用的行不得进装载集（与 RetailDataDrivenTable 口径一致）: " + questId);
			assertFalse(RetiredQuestIds.contains(questId), "注释行不得退役: " + questId);
			assertFalse(xmlOnly.contains(questId), "注释行不得有 XML 定义: " + questId);
			assertEquals("ABSENT", owners.getOrDefault(questId, "ABSENT"), "注释行 owner 必须 ABSENT: " + questId);
		}
		assertTrue(java.util.Collections.disjoint(frozenAbsent, commentedOut), "两桶互斥");
	}

	/** ② P7 切换集与孤行零交集：owner `RETAIL_TABLE` 的 DD 行 = 1467，且全部客户端可见（不在孤行集里）。 */
	@Test
	void theP7SwitchSetStaysDisjointFromTheFrozenRows() {
		Set<Integer> retired = new TreeSet<>();
		for (int questId : dd.questIds()) {
			if ("RETAIL_TABLE".equals(owners.get(questId))) {
				retired.add(questId);
			}
		}
		assertEquals(DD_RETIRED_ROWS, retired.size(), "DD 切换集规模冻结（owner RETAIL_TABLE）");
		Set<Integer> overlap = new TreeSet<>(retired);
		overlap.retainAll(frozenAbsent);
		assertTrue(overlap.isEmpty(), "P7 切换集不得包含客户端不可渲染的孤行: " + overlap);
		for (int questId : frozenAbsent) {
			assertFalse("RETAIL_TABLE".equals(owners.get(questId)), "孤行不得转 RETAIL_TABLE: " + questId);
			assertFalse("XML_RETENTION".equals(owners.get(questId)), "孤行不得进 XML 保留: " + questId);
		}
		Set<Integer> deadOverlap = new TreeSet<>(retired);
		deadOverlap.retainAll(commentedOut);
		assertTrue(deadOverlap.isEmpty(), "P7 切换集不得包含注释禁用行: " + deadOverlap);
	}

	/** ③ 80817：客户端三表都有行 ⇒ 必须实现；原版规格 `world_event_camel 100` 必须原样保留（6 位组槽 ⇒ 原版不可完成）。 */
	@Test
	void eventQuest80817KeepsTheRetailHuntArithmetic() {
		assertFalse(frozenAbsent.contains(80817), "80817 客户端两表都有行，不属于孤行集");
		Row row = dd.find(80817).orElseThrow();
		assertEquals("talk", row.acquireKind(), "80817 接取类别 = Talk（原版表）");
		String huntPayload = row.steps().stream()
			.filter(step -> step.kind() == Kind.HUNT)
			.filter(step -> step.payload().contains(EVENT_HUNT_MONSTER))
			.findFirst()
			.orElseThrow(() -> new AssertionError("80817 必须声明 " + EVENT_HUNT_MONSTER + " 击杀"))
			.payload();
		// 原版载荷尾整数可带 `;` 段尾（`world_event_camel 100;`）——只取前导整数。
		// The retail payload's trailing count may end with a `;` segment separator — take the
		// leading integer only.
		String tail = huntPayload.substring(huntPayload.lastIndexOf(' ') + 1).trim();
		int count = Integer.parseInt(tail.substring(0, (int) tail.chars().takeWhile(Character::isDigit).count()));
		assertEquals(EVENT_HUNT_COUNT, count,
			"80817 原版计数 = 100（6 位组槽上限 63 ⇒ 原版自身永不达标，禁止改成 10 位相机）");
		assertTrue(questXmlIds.contains(80817), "80817 必须有原版 quest.xml 元数据行");
		assertEquals("RETAIL_TABLE", owners.get(80817), "80817 属于 P7 切换集");
	}

	// ---------------------------------------------------------------- 夹具

	private static Map<String, Set<Integer>> fixtureRows() throws Exception {
		Map<String, Set<Integer>> buckets = new LinkedHashMap<>();
		buckets.put("CLIENT_ABSENT_LIVE", new TreeSet<>());
		buckets.put("COMMENTED_OUT", new TreeSet<>());
		try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(resource(ABSENT_FIXTURE), StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.isBlank() || line.startsWith("#")) {
					continue;
				}
				String[] cells = line.split("\t", -1);
				if (cells[0].equals("quest_id")) {
					assertEquals(7, cells.length, "孤行夹具必须 7 列");
					continue;
				}
				assertEquals(7, cells.length, "孤行夹具行必须 7 列: " + line);
				Set<Integer> bucket = buckets.get(cells[6]);
				assertNotNull(bucket, "孤行夹具裁定列必须是两桶之一: " + line);
				bucket.add(Integer.parseInt(cells[0]));
			}
		}
		return buckets;
	}

	/** 原版 `quest.xml` 的全部任务 id（独立重解析）。 / All retail quest.xml ids. */
	private static Set<Integer> questXmlIds() throws Exception {
		String text = new String(resource(QUEST_XML).readAllBytes(), StandardCharsets.UTF_8);
		Set<Integer> ids = new TreeSet<>();
		Matcher matcher = Pattern.compile("<id>\\s*(\\d+)\\s*</id>").matcher(text);
		while (matcher.find()) {
			ids.add(Integer.parseInt(matcher.group(1)));
		}
		assertTrue(ids.size() > 9000, "原版 quest.xml 行数异常: " + ids.size());
		return ids;
	}

	/** 保留台账 owner（测试副本与生产副本逐字节一致）。 */
	private static Map<Integer, String> retentionOwners() throws Exception {
		Map<Integer, String> owners = new LinkedHashMap<>();
		for (Element row : RetailLedgerRows.rows(RETENTION, "quest")) {
			String questId = RetailLedgerRows.cell(row, "quest_id");
			String owner = RetailLedgerRows.cell(row, "owner");
			String family = RetailLedgerRows.cell(row, "family");
			String reason = RetailLedgerRows.cell(row, "reason");
			String evidence = RetailLedgerRows.cell(row, "evidence");
			// 旧 TSV「行必须 5 列」的钉子：五列任一缺席即红。
			// The old five-column pin: any missing column is red.
			assertNotNull(questId, "台账行必须 5 列");
			assertNotNull(owner, "台账行必须 5 列");
			assertNotNull(family, "台账行必须 5 列");
			assertNotNull(reason, "台账行必须 5 列");
			assertNotNull(evidence, "台账行必须 5 列");
			owners.putIfAbsent(Integer.parseInt(questId), owner);
		}
		return owners;
	}

	private static InputStream resource(String path) {
		InputStream input = RetailDataDrivenClientPresenceGateTest.class.getResourceAsStream(path);
		assertNotNull(input, "missing resource " + path);
		return input;
	}
}
