package com.aionemu.gameserver.questEngine.tablelane;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.questEngine.retail.RetailDataDrivenTable;
import com.aionemu.gameserver.questEngine.retail.RetailDataDrivenTable.Entry;

/**
 * P7 步 1 门（计划 §10.2「P7 DataDriven」）：DD 行的**原生 handler 契约**逐行冻结。
 * <p>
 * P7 切换集 = 真端 DD 活行 ∧ owner {@code RETAIL_TABLE} = **1467 行**（客户端可渲染 ∧ 玩家可见；
 * 孤行 337 / 注释 18 见 {@code RetailDataDrivenClientPresenceGateTest}）。本门把该集合的
 * **原生实现契约**冻成六条不可静默漂移的不变量，作为 step 2（运行时 + 原子切换 + 删旧）的验收基线：
 * <ol>
 *   <li>接取轴 = 真端 {@code category_acquire_}（去大小写）只出现 6 类：{@code talk/enterarea/none/
 *       leveluplogin/itemplay/enterworld}，逐类计数冻结（真端 5 类 kind 3/4/7/8/10 + DD 的
 *       {@code EnterArea} 与链式 {@code none}）；</li>
 *   <li>进度轴 = 真端 8 类 progress handler（{@code Hunt/CollectItem/Pvp/Talk/EnterArea/ItemPlay/
 *       EnterWorld/TalkFOBJ}；证据 {@code p7-prereqs/dd-dispatcher-and-handlers.md} §2
 *       FUN_180c46020/46980/46e90/466a0/47bf0/474b0/467b0/478e0）——未知类别 fail-closed；</li>
 *   <li>步列词汇表 = 每类实际出现的 {@code valueN_progress_} 列号与出现次数（即 step 2 必须实现的
 *       动作/效果面清单：0=载荷、1..4=发/扣物品与多 FOBJ、4=过场/影像、5=生成对象、6/7/9/10=其它
 *       效果列）——新增列号或计数漂移即失败；</li>
 *   <li>6 位布局不变量 = 每步子计数 ≤ 4 组、计数 ≤ 63（真端 bit0-5 步号 + 4×6 位组槽），唯一例外
 *       80817（计数 100；裁定「复刻真端算术含第 64 杀回绕」，见 §10.3-#5）；</li>
 *   <li>每行必须声明 {@code reward_npc_name}（领奖面由真端表行驱动）；</li>
 *   <li>逐行矩阵（id × 接取 × 步序列 × 列词汇 × 领奖名）的规范形摘要冻结，且生产装载器
 *       {@link RetailDataDrivenTable} 的解析结果与之逐行一致（装载器视图 = 契约视图）。</li>
 * </ol>
 * 证据面：`p7/tools/dd-native-contract-probe.py`（复算工具）、`p7/dd-native-shape-matrix.tsv`
 * （逐行矩阵）、`p7/P7-STEP1-REPORT.zh-CN.md`（报告）。
 * <p>
 * P7 step 1 gate: freezes the per-row native handler contract of the DataDriven switch set (1467 rows):
 * acquire kinds, the eight retail progress categories, the step-column (action/effect) vocabulary, the
 * 6-bit layout invariants, the reward-npc axis, and a canonical row-matrix digest that the production
 * table loader must reproduce row by row.
 */
class DataDrivenNativeContractGateTest {

	private static final String DD_TABLE = "/aion/data/static_data/quest/retail/data_driven_quest.xml";
	private static final String RETENTION = "/quest/retail-xml-retention.tsv";
	private static final String ABSENT_FIXTURE = "/quest/retail-data-driven-client-absent.tsv";

	/** 切换集规模（真端 DD 活行 ∧ owner RETAIL_TABLE）。 / The switch set size. */
	private static final int SWITCH_ROWS = 1467;
	private static final int CLIENT_ABSENT_LIVE_ROWS = 337;
	private static final int COMMENTED_OUT_ROWS = 18;
	/** 进度形去重数（有序步序列；漂移即新形，必须显式裁定）。 / Distinct ordered step shapes. */
	private static final int DISTINCT_SHAPES = 94;
	/** 逐行矩阵规范形摘要（工具复算；装载器/表改动即失败）。 / Canonical row-matrix digest. */
	private static final String CANONICAL_SHA256 =
		"3d7b762e16b4977b98844fdca414ea287fa3c3c36429373cb8b6b08fa5b7f77d";
	/** 真端 8 类 progress handler。 / The eight retail progress handlers. */
	private static final Set<String> PROGRESS_CATEGORIES = Set.of(
		"hunt", "collectitem", "pvp", "talk", "enterarea", "itemplay", "enterworld", "talkfobj");
	/** 真端接取 kind（kind 3/4/7/8/10）+ DD 的 EnterArea / none。 / Retail acquire kinds. */
	private static final Set<String> ACQUIRE_KINDS = Set.of(
		"talk", "itemplay", "levelup", "enterworld", "leveluplogin", "enterarea", "none");
	/** 裁定例外：80817 计数 100 > 63（真端自身不可完成，原样复刻）。 / Adjudicated overflow row. */
	private static final Set<Integer> ADJUDICATED_OVER_SIX_BIT = Set.of(80817);
	private static final int SIX_BIT_MASK = 0x3F;

	private static final Map<String, Integer> ACQUIRE_HISTOGRAM = Map.of(
		"talk", 1142, "enterarea", 165, "none", 120, "leveluplogin", 15, "itemplay", 13, "enterworld", 12);
	private static final Map<Integer, Integer> STEPS_PER_ROW = Map.ofEntries(
		Map.entry(0, 99), Map.entry(1, 1188), Map.entry(2, 36), Map.entry(3, 48), Map.entry(4, 17),
		Map.entry(5, 26), Map.entry(6, 10), Map.entry(7, 19), Map.entry(8, 7), Map.entry(9, 4),
		Map.entry(10, 7), Map.entry(12, 4), Map.entry(14, 2));
	private static final Map<String, Integer> CATEGORY_STEPS = Map.of(
		"hunt", 827, "collectitem", 348, "pvp", 207, "talk", 403,
		"enterarea", 153, "itemplay", 42, "enterworld", 34, "talkfobj", 19);
	private static final Map<String, Map<Integer, Integer>> COLUMN_HISTOGRAM = Map.of(
		"hunt", Map.of(0, 827, 4, 5, 5, 4),
		"collectitem", Map.of(0, 348, 1, 72, 2, 13, 3, 4, 4, 2, 5, 83),
		"pvp", Map.of(0, 207, 1, 22, 2, 22, 3, 96),
		"talk", Map.of(0, 403, 1, 78, 2, 20, 3, 3, 4, 8, 5, 11, 6, 10, 7, 4, 9, 3, 10, 6),
		"enterarea", Map.of(0, 153, 4, 12, 5, 6, 7, 3, 10, 2),
		"itemplay", Map.of(0, 42, 1, 4, 2, 3, 4, 2, 5, 5, 7, 1, 10, 2),
		"enterworld", Map.of(0, 34, 1, 1, 2, 1, 4, 1),
		"talkfobj", Map.of(0, 19, 1, 7, 2, 3, 3, 1, 4, 2, 5, 5, 6, 2, 10, 2));

	/**
	 * 类别载荷列（真端 `FUN_180c4b980`：按 kind 解析 index=0 的类别数据；CollectItem 另占 1..4 的追加 FOBJ 与 5 的整数，
	 * PvP 另占 1..3 的军衔阈值/上限/等级差）。 / Category payload columns parsed by `FUN_180c4b980`.
	 */
	private static final Map<String, Set<Integer>> CATEGORY_PAYLOAD_COLUMNS = Map.of(
		"hunt", Set.of(0),
		"collectitem", Set.of(0, 1, 2, 3, 4, 5),
		"pvp", Set.of(0, 1, 2, 3),
		"talk", Set.of(0),
		"enterarea", Set.of(0),
		"itemplay", Set.of(0),
		"enterworld", Set.of(0),
		"talkfobj", Set.of(0));
	/**
	 * 通用附加动作列（真端 `FUN_180c49610` = `DataDrivenQuestLoader::LoadExtraAction`）：1/2=发扣物品、3=传送、
	 * 4=过场影像、5=生成 NPC、6=延迟、7/8=消息、9=进副本、10=定时器；真端 guard 只放行 kind 2/3/4/6/7/8/9/10，
	 * 且 Hunt 只放行 4/5 ⇒ CollectItem(1) 与 PvP(5) **没有**附加动作面。 / Generic extra-action columns.
	 */
	private static final Map<String, Set<Integer>> EXTRA_ACTION_COLUMNS = Map.of(
		"hunt", Set.of(4, 5),
		"itemplay", Set.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10),
		"talk", Set.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10),
		"enterarea", Set.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10),
		"enterworld", Set.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10),
		"talkfobj", Set.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));

	private static final Pattern BLOCK = Pattern.compile("<quest_data_driven>(.*?)</quest_data_driven>", Pattern.DOTALL);
	private static final Pattern DATA = Pattern.compile("<data>(.*?)</data>", Pattern.DOTALL);
	private static final Pattern COMMENT = Pattern.compile("<!--.*?-->", Pattern.DOTALL);
	private static final Pattern DTD = Pattern.compile("<!DOCTYPE.*?\\]>", Pattern.DOTALL);
	private static final Pattern CATEGORY = Pattern.compile("<category_progress_>\\s*([^<]*?)\\s*</category_progress_>");
	private static final Pattern COLUMN = Pattern.compile("<(value\\d+_progress_)>\\s*([^<]*?)\\s*</\\1>");

	private static RetailDataDrivenTable loader;
	private static Map<Integer, Row> rows;
	private static Set<Integer> switchSet;
	private static Set<Integer> clientAbsent;
	private static Set<Integer> commentedOut;

	/** 逐行原生契约：接取类别 + 步序列（类别、列词汇）。 / One row's native contract. */
	private record Row(int questId, String acquire, String rewardNpc, List<Step> steps) {
	}

	private record Step(String category, Map<Integer, String> columns) {
	}

	@BeforeAll
	static void loadFixtures() throws Exception {
		loader = RetailDataDrivenTable.load(resource(DD_TABLE));
		rows = parseRawTable();
		clientAbsent = fixtureBucket("CLIENT_ABSENT_LIVE");
		commentedOut = fixtureBucket("COMMENTED_OUT");
		Map<Integer, String> owners = retentionOwners();
		switchSet = new TreeSet<>();
		for (int questId : rows.keySet()) {
			if ("RETAIL_TABLE".equals(owners.get(questId))) {
				switchSet.add(questId);
			}
		}
	}

	/** ① 切换集与两个排除桶互斥、规模冻结，且每行都声明领奖 NPC。 */
	@Test
	void switchSetIsTheClientVisibleRetailBand() {
		assertEquals(SWITCH_ROWS, switchSet.size(), "P7 切换集规模冻结（真端活行 ∧ owner RETAIL_TABLE）");
		assertEquals(CLIENT_ABSENT_LIVE_ROWS, clientAbsent.size(), "客户端三表皆无的孤行规模冻结");
		assertEquals(COMMENTED_OUT_ROWS, commentedOut.size(), "真端表注释禁用行规模冻结");
		assertTrue(java.util.Collections.disjoint(switchSet, clientAbsent), "切换集不得含孤行");
		assertTrue(java.util.Collections.disjoint(switchSet, commentedOut), "切换集不得含注释行");
		for (int questId : switchSet) {
			String reward = rows.get(questId).rewardNpc();
			assertTrue(reward != null && !reward.isBlank(), "切换集每行必须声明 reward_npc_name: " + questId);
		}
	}

	/** ② 接取轴：真端 kind 6 类（含 EnterArea/none），未知类别 fail-closed。 */
	@Test
	void acquireAxisCoversOnlyRetailKinds() {
		Map<String, Integer> histogram = new TreeMap<>();
		for (int questId : switchSet) {
			String acquire = rows.get(questId).acquire();
			assertTrue(ACQUIRE_KINDS.contains(acquire),
				"接取类别必须落在真端 kind 集合内（未知类别 fail-closed）: " + questId + " -> " + acquire);
			histogram.merge(acquire, 1, Integer::sum);
		}
		assertEquals(ACQUIRE_HISTOGRAM, histogram, "接取轴直方图冻结");
		assertEquals(SWITCH_ROWS, histogram.values().stream().mapToInt(Integer::intValue).sum(), "直方图必须覆盖全切换集");
	}

	/** ③ 进度轴：8 类 handler 词汇闭合，步数分布与去重形数冻结。 */
	@Test
	void progressVocabularyIsTheRetailEightHandlers() {
		Map<Integer, Integer> stepsPerRow = new TreeMap<>();
		Map<String, Integer> categorySteps = new TreeMap<>();
		Set<List<String>> shapes = new TreeSet<>((left, right) -> {
			int size = Integer.compare(left.size(), right.size());
			if (size != 0) {
				return size;
			}
			for (int index = 0; index < left.size(); index++) {
				int compare = left.get(index).compareTo(right.get(index));
				if (compare != 0) {
					return compare;
				}
			}
			return 0;
		});
		for (int questId : switchSet) {
			Row row = rows.get(questId);
			List<String> categories = new ArrayList<>();
			for (Step step : row.steps()) {
				assertTrue(PROGRESS_CATEGORIES.contains(step.category()),
					"进度类别必须落在真端 8 类 handler 内（未知类别 fail-closed）: " + questId + " -> " + step.category());
				assertTrue(step.columns().containsKey(0), "步必须声明 value0_progress_ 载荷: " + questId);
				categorySteps.merge(step.category(), 1, Integer::sum);
				categories.add(step.category());
			}
			stepsPerRow.merge(categories.size(), 1, Integer::sum);
			shapes.add(categories);
		}
		assertEquals(CATEGORY_STEPS, categorySteps, "每类 handler 的步数直方图冻结");
		assertEquals(STEPS_PER_ROW, stepsPerRow, "每行步数分布冻结");
		assertEquals(DISTINCT_SHAPES, shapes.size(), "有序步序列去重数冻结（新形必须显式裁定）");
	}

	/** ④ 步列词汇表：每类实际使用的 valueN 列号与次数冻结 = step 2 的动作/效果实现清单。 */
	@Test
	void stepColumnsStayInsideTheFrozenActionSurface() {
		Map<String, Map<Integer, Integer>> histogram = new TreeMap<>();
		for (int questId : switchSet) {
			for (Step step : rows.get(questId).steps()) {
				Map<Integer, Integer> perCategory = histogram.computeIfAbsent(step.category(), key -> new TreeMap<>());
				step.columns().keySet().forEach(column -> perCategory.merge(column, 1, Integer::sum));
			}
		}
		Map<String, Map<Integer, Integer>> frozen = new TreeMap<>();
		COLUMN_HISTOGRAM.forEach((category, counts) -> frozen.put(category, new TreeMap<>(counts)));
		assertEquals(frozen, histogram, "步列词汇表冻结：新增 valueN 列必须显式裁定（禁止静默吞列）");
	}

	/**
	 * ⑦ 步列语义闭合（真端 `FUN_180c4b980` + `FUN_180c49610`，见 `p7/P7-STEP2-PREREQ-COLUMN-SEMANTICS.zh-CN.md`）：
	 * 每个观测到的 (类别, 列号) 必须落在「类别载荷」或「该类别真端允许的附加动作」之一；真端非法组合必须零命中，
	 * 未知组合必须零命中（新增列号/新组合一律 fail-closed，禁止静默吞列）。
	 *
	 * Every observed (category, column) pair must be either a category payload column or an extra-action column the
	 * retail loader admits for that category; illegal or unknown combinations must stay at zero hits.
	 */
	@Test
	void everyStepColumnCarriesAdjudicatedRetailSemantics() {
		Map<String, Set<Integer>> observed = new TreeMap<>();
		for (int questId : switchSet) {
			for (Step step : rows.get(questId).steps()) {
				for (int column : step.columns().keySet()) {
					Set<Integer> payloads = CATEGORY_PAYLOAD_COLUMNS.get(step.category());
					Set<Integer> extras = EXTRA_ACTION_COLUMNS.getOrDefault(step.category(), Set.of());
					assertTrue(payloads.contains(column) || extras.contains(column),
						"步列语义未裁定（fail-closed）: quest " + questId + " category " + step.category()
							+ " column " + column);
					observed.computeIfAbsent(step.category(), key -> new TreeSet<>()).add(column);
				}
			}
		}
		for (Map.Entry<String, Set<Integer>> entry : observed.entrySet()) {
			Set<Integer> allowed = new TreeSet<>(CATEGORY_PAYLOAD_COLUMNS.get(entry.getKey()));
			allowed.addAll(EXTRA_ACTION_COLUMNS.getOrDefault(entry.getKey(), Set.of()));
			assertTrue(allowed.containsAll(entry.getValue()),
				"类别 " + entry.getKey() + " 的观测列必须落在裁定面内: " + entry.getValue());
			assertTrue(entry.getValue().contains(0), "每步必须有载荷列 0: " + entry.getKey());
		}
		// 真端 guard：CollectItem(1) 与 PvP(5) 无附加动作面；Hunt 只允许 4/5。
		assertTrue(!EXTRA_ACTION_COLUMNS.containsKey("collectitem"), "CollectItem 无附加动作面（真端 return 1）");
		assertTrue(!EXTRA_ACTION_COLUMNS.containsKey("pvp"), "PvP 无附加动作面（真端 return 1）");
		assertEquals(Set.of(4, 5), EXTRA_ACTION_COLUMNS.get("hunt"), "Hunt 只允许 4/5 落到附加动作");
		assertEquals(PROGRESS_CATEGORIES, observed.keySet(), "观测类别必须恰为真端 8 类");
	}

	/** ⑤ 6 位布局：每步子计数 ≤ 4 组、计数 ≤ 63（80817 = 裁定例外，原样复刻真端回绕）。 */
	@Test
	void everyStepStaysInsideTheSixBitLayout() {
		Set<Integer> overSixBit = new TreeSet<>();
		for (int questId : switchSet) {
			Row row = rows.get(questId);
			for (Step step : row.steps()) {
				int groups = counterGroups(step);
				assertTrue(groups <= 4, "每步子计数 ≤ 4 组（真端 bit0-5 步号 + 4×6 位组槽）: " + questId
					+ " -> " + groups);
				for (Integer target : targets(step)) {
					if (target == null) {
						continue;
					}
					if (target > SIX_BIT_MASK) {
						overSixBit.add(questId);
					}
				}
			}
		}
		assertEquals(ADJUDICATED_OVER_SIX_BIT, overSixBit,
			"唯一超 6 位计数的行必须仍只有 80817（禁止改成 10 位相机、禁止静默修复）");
		assertEquals(100, targets(rows.get(80817).steps().get(0)).get(0), "80817 真端计数 = 100（原样保留）");
	}

	/** ⑥ 逐行矩阵摘要 + 生产装载器视图一致性（装载器 = 契约视图）。 */
	@Test
	void theRowMatrixMatchesTheFrozenDigest() throws Exception {
		StringBuilder canonical = new StringBuilder();
		for (int questId : switchSet) {
			Row row = rows.get(questId);
			List<String> categories = new ArrayList<>();
			Set<Integer> columns = new TreeSet<>();
			for (Step step : row.steps()) {
				categories.add(step.category());
				columns.addAll(step.columns().keySet());
			}
			canonical.append(questId).append('\t')
				.append(row.acquire()).append('\t')
				.append(categories.isEmpty() ? "-" : String.join(";", categories)).append('\t')
				.append(columns.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(",")))
				.append('\t')
				.append(row.rewardNpc() == null || row.rewardNpc().isBlank() ? "-" : row.rewardNpc())
				.append('\n');
		}
		assertEquals(CANONICAL_SHA256, sha256(canonical.toString()),
			"逐行矩阵规范形摘要冻结（真端表 / owner 台账 / 孤行快照任一漂移即失败）");
		for (int questId : switchSet) {
			Entry entry = loader.find(questId).orElseThrow(() -> new AssertionError("装载器缺行: " + questId));
			assertEquals(rows.get(questId).acquire().toLowerCase(java.util.Locale.ROOT),
				entry.acquireCategory().toLowerCase(java.util.Locale.ROOT), "装载器接取类别必须与表一致: " + questId);
			assertEquals(rows.get(questId).steps().stream().map(Step::category).toList(),
				entry.stepCategories(), "装载器步序列必须与表一致: " + questId);
			assertEquals(rows.get(questId).rewardNpc(), entry.rewardNpc(), "装载器领奖名必须与表一致: " + questId);
		}
	}

	// ---------------------------------------------------------------- 复算辅助

	/** 每步子计数组数（hunt/collectitem/pvp = 分号段数，其余 = 1）。 / Counter groups declared by a step. */
	private static int counterGroups(Step step) {
		if (!Set.of("hunt", "collectitem", "pvp").contains(step.category())) {
			return 1;
		}
		return Math.max(1, segments(step).size());
	}

	/** 每步目标计数（hunt/collectitem 取段尾数字，pvp 取段值）。 / Target counts of a step. */
	private static List<Integer> targets(Step step) {
		if (!Set.of("hunt", "collectitem", "pvp").contains(step.category())) {
			return List.of(1);
		}
		List<Integer> targets = new ArrayList<>();
		for (String segment : segments(step)) {
			if ("pvp".equals(step.category())) {
				targets.add(allDigits(segment) ? Integer.parseInt(segment) : null);
				continue;
			}
			int split = segment.lastIndexOf(' ');
			String tail = split > 0 ? segment.substring(split + 1).trim() : "";
			targets.add(allDigits(tail) ? Integer.parseInt(tail) : null);
		}
		return targets;
	}

	private static List<String> segments(Step step) {
		List<String> segments = new ArrayList<>();
		for (String segment : step.columns().getOrDefault(0, "").split(";")) {
			if (!segment.isBlank()) {
				segments.add(segment.trim());
			}
		}
		return segments;
	}

	private static boolean allDigits(String value) {
		return !value.isEmpty() && value.chars().allMatch(Character::isDigit);
	}

	private static String sha256(String text) throws Exception {
		byte[] digest = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
		StringBuilder hex = new StringBuilder();
		for (byte value : digest) {
			hex.append(String.format("%02x", value));
		}
		return hex.toString();
	}

	/** 真端表原始文本复算（与工具同口径：去 DTD/注释后逐块解析）。 / Raw-table recomputation. */
	private static Map<Integer, Row> parseRawTable() throws Exception {
		String text = new String(resource(DD_TABLE).readAllBytes(), StandardCharsets.UTF_8);
		Matcher dtd = DTD.matcher(text);
		if (dtd.find()) {
			text = text.substring(0, dtd.start()) + text.substring(dtd.end());
		}
		text = COMMENT.matcher(text).replaceAll("");
		Map<Integer, Row> parsed = new LinkedHashMap<>();
		Matcher blocks = BLOCK.matcher(text);
		while (blocks.find()) {
			String body = blocks.group(1);
			int questId = Integer.parseInt(field(body, "id"));
			List<Step> steps = new ArrayList<>();
			Matcher datas = DATA.matcher(body);
			while (datas.find()) {
				String data = datas.group(1);
				Matcher category = CATEGORY.matcher(data);
				assertTrue(category.find(), "步缺 category_progress_: " + questId);
				Map<Integer, String> columns = new TreeMap<>();
				Matcher column = COLUMN.matcher(data);
				while (column.find()) {
					String value = column.group(2).trim();
					if (!value.isEmpty()) {
						columns.put(Integer.parseInt(column.group(1).replaceAll("\\D+", "")), value);
					}
				}
				steps.add(new Step(category.group(1).trim().toLowerCase(java.util.Locale.ROOT), columns));
			}
			parsed.put(questId, new Row(questId, field(body, "category_acquire_").toLowerCase(java.util.Locale.ROOT),
				field(body, "reward_npc_name"), List.copyOf(steps)));
		}
		assertTrue(parsed.size() > 2400, "真端 DD 表活行数异常: " + parsed.size());
		return parsed;
	}

	private static String field(String body, String name) {
		Matcher matcher = Pattern.compile("<" + name + ">\\s*([^<]*?)\\s*</" + name + ">", Pattern.DOTALL).matcher(body);
		return matcher.find() ? matcher.group(1).trim() : null;
	}

	private static Set<Integer> fixtureBucket(String bucketName) throws Exception {
		Set<Integer> bucket = new TreeSet<>();
		try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(resource(ABSENT_FIXTURE), StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.isBlank() || line.startsWith("#")) {
					continue;
				}
				String[] cells = line.split("\t", -1);
				if (cells[0].equals("quest_id")) {
					continue;
				}
				if (cells[6].equals(bucketName)) {
					bucket.add(Integer.parseInt(cells[0]));
				}
			}
		}
		return bucket;
	}

	private static Map<Integer, String> retentionOwners() throws Exception {
		Map<Integer, String> owners = new LinkedHashMap<>();
		try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(resource(RETENTION), StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.isBlank() || line.startsWith("#")) {
					continue;
				}
				String[] cells = line.split("\t", -1);
				owners.putIfAbsent(Integer.parseInt(cells[0]), cells[1]);
			}
		}
		return owners;
	}

	private static InputStream resource(String path) {
		InputStream input = DataDrivenNativeContractGateTest.class.getResourceAsStream(path);
		assertNotNull(input, "missing resource " + path);
		return input;
	}
}
