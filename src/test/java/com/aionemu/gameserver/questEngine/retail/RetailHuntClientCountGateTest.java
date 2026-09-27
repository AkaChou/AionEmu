package com.aionemu.gameserver.questEngine.retail;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * 常设门：单段网格 hunt 的**计数轴**——真端 DD 表读数 vs 客户端进度行计数。
 * <p>
 * 背景：DD 表 {@code value0_progress_} 的尾数是服务端表修订的读数，与客户端任务书/SECTION 门控
 * 在少数行不一致（13758 族：服务端 15/12/8/6/20 vs 客户端 5；15571 族：服务端 30 vs 客户端 10；
 * 13770 族：服务端 5 vs 客户端 12；13841 族：服务端 60/60/55 vs 客户端 40，其中真端名单含
 * 改名/幽灵模板）。绑定侧对"单计数器 + 单条客户端进度行"的单段行以客户端计数为准，
 * 本门①锁死分歧集恰为已裁定的 40 行（34 行同名单 + 6 行改名名单），
 * ②逐行验证对齐结果等于客户端计数，③反向验证未登记/多行登记/零计数时不动真端读数。
 * <p>
 * Permanent gate for the single-stage grid hunt count axis: (1) the DD-vs-client disagreement set is
 * exactly the reviewed 40 rows (34 with matching names plus 6 whose retail list carries renamed or
 * phantom templates), (2) the alignment yields the client count for every one of them, and (3) an
 * unregistered, multi-row or zero-count registration never rewrites the retail count.
 */
class RetailHuntClientCountGateTest {

	private static final String DD_TABLE = "/aion/data/static_data/quest_retail/data_driven_quest.xml";
	private static final String CLIENT_ROWS =
		"/aion/data/static_data/quest_retail/quest_client_hunt_progress_rows.tsv";
	private static final String NPC_DIR = "/aion/data/static_data/npcs/";
	private static final List<String> NPC_TEMPLATES = List.of(
		"npc_template_200000_216188.xml", "npc_template_216189_235748.xml", "npc_template_235749_247606.xml",
		"npc_template_247607_270057.xml", "npc_template_270058_286320.xml", "npc_template_286321_800030.xml",
		"npc_template_800031_834289.xml", "npc_template_834290_885645.xml");
	private static final Pattern HUNT = Pattern.compile(
		"<category_progress_>Hunt</category_progress_>\\s*<value0_progress_>([^<]*)</value0_progress_>");
	private static final Pattern VALUE = Pattern.compile("^(.*?)\\s+(\\d+)\\s*;\\s*$", Pattern.DOTALL);
	/**
	 * 已裁定的计数分歧集（40 行）：24 行守备队族 + 13770/23770（服务端 5 / 客户端 12）
	 * + 15571/15573/15576/15578/25571/25573/25576/25578（服务端 30 / 客户端 10）
	 * + 13841/13845/13849/23841/23845/23849（服务端 60/60/55 / 客户端 40，真端名单含改名模板；
	 * 遗留 XML 的 var1 上限 40、客户端门控 40、客户端进度行 40 三证同侧）。
	 * The reviewed count-disagreement set (40 rows): the 24 guard rows, the 13770/23770 pair
	 * (server 5 / client 12), the 15571 family (server 30 / client 10) and the 13841 family
	 * (server 60/60/55 / client 40, whose retail lists carry renamed templates; the legacy XML
	 * ceiling of 40, the client gate and the client progress row all agree).
	 */
	private static final Set<String> REVIEWED_DISAGREEMENTS = reviewedDisagreements();

	private static List<String> index;
	private static Map<String, List<RetailClientHuntProgressRows.Row>> clientRows;
	private static Map<String, Stage> ddStages;
	private static RetailNpcNameIndex npcIndex;

	private record Stage(int count, List<String> monsters) {
	}

	@BeforeAll
	static void setUp() throws Exception {
		Map<String, List<RetailClientHuntProgressRows.Row>> rows = new LinkedHashMap<>();
		for (String line : lines(open(CLIENT_ROWS))) {
			if (line.startsWith("#") || line.isBlank()) {
				continue;
			}
			String[] parts = line.split("\t");
			rows.computeIfAbsent(parts[0], key -> new ArrayList<>())
				.add(new RetailClientHuntProgressRows.Row(Integer.parseInt(parts[1]),
					Integer.parseInt(parts[2]), Integer.parseInt(parts[3]),
					List.of(parts[4].split(";"))));
		}
		clientRows = rows;

		Map<String, Stage> stages = new TreeMap<>();
		String text = new String(open(DD_TABLE).readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
		for (String block : blocks(text)) {
			Matcher id = Pattern.compile("<id>(\\d+)</id>").matcher(block);
			if (!id.find()) {
				continue;
			}
			Matcher hunt = HUNT.matcher(block);
			List<Stage> found = new ArrayList<>();
			while (hunt.find()) {
				Matcher value = VALUE.matcher(hunt.group(1).strip());
				if (!value.matches()) {
					continue;
				}
				List<String> monsters = new ArrayList<>();
				for (String name : value.group(1).split(",")) {
					if (!name.isBlank()) {
						monsters.add(name.strip().toLowerCase(java.util.Locale.ROOT));
					}
				}
				found.add(new Stage(Integer.parseInt(value.group(2)), List.copyOf(monsters)));
			}
			if (found.size() == 1) {
				stages.put(id.group(1), found.get(0));
			}
		}
		ddStages = stages;

		npcIndex = RetailNpcNameIndex.build(openAll(NPC_DIR, NPC_TEMPLATES));
	}

	/** ①分歧集锁死：DB 表读数与客户端计数不一致的单段行恰为已裁定的 40 行。 / The disagreement set is frozen. */
	@Test
	void ddCountVersusClientCountDisagreementsAreExactlyTheReviewedSet() {
		Set<String> disagreements = new TreeSet<>();
		for (Map.Entry<String, Stage> entry : ddStages.entrySet()) {
			// 只认「单计数器 + 单条客户端进度行」的单段形——绑定侧的计数对齐判据同形，
			// 多行登记（并行段/多段形）不在这里裁定。
			// Only the single-counter + single-client-row form is judged here, matching the binding's
			// alignment predicate; multi-row registrations stay out of this adjudication.
			List<RetailClientHuntProgressRows.Row> rows = clientRows.get(entry.getKey());
			if (rows == null || rows.size() != 1) {
				continue;
			}
			int clientCount = rows.getFirst().count();
			if (clientCount > 0 && clientCount != entry.getValue().count()) {
				disagreements.add(entry.getKey());
			}
		}
		assertEquals(new TreeSet<>(REVIEWED_DISAGREEMENTS), disagreements,
			"DD 表读数与客户端计数的分歧集变动（新增/消失都必须重新裁定）");
	}

	/** ②对齐机制：每个分歧行的绑定结果都等于客户端计数。 / Alignment yields the client count. */
	@Test
	void alignmentYieldsTheClientCountForEveryDisagreeingRow() {
		int checked = 0;
		for (String questId : REVIEWED_DISAGREEMENTS) {
			Stage stage = ddStages.get(questId);
			List<RetailClientHuntProgressRows.Row> rows = clientRows.get(questId);
			RetailSimpleHuntPlan plan = bindPlan(Integer.parseInt(questId), stage);
			RetailSimpleHuntPlan aligned = plan.withClientStageCounts(rows);
			int clientCount = clientCount(rows);
			assertEquals(clientCount, aligned.counters().get(0).required(),
				() -> questId + " 的对齐结果必须是客户端计数");
			// 目标集合与槽位不得被计数对齐改写。
			assertEquals(plan.counters().get(0).npcIds(), aligned.counters().get(0).npcIds(),
				() -> questId + " 计数对齐不得改写击杀目标集");
			checked++;
		}
		assertEquals(REVIEWED_DISAGREEMENTS.size(), checked, "对齐覆盖数必须等于分歧集规模");
	}

	/** ③反向：客户端未登记、多行登记或零计数时，真端读数保持不变。 / No rewrite on those shapes. */
	@Test
	void alignmentIsInertWithoutASingleClientCountRow() {
		String questId = "13758";
		Stage stage = ddStages.get(questId);
		RetailSimpleHuntPlan plan = bindPlan(Integer.parseInt(questId), stage);
		assertEquals(stage.count(), plan.withClientStageCounts(List.of()).counters().get(0).required(),
			"客户端未登记时不得改写真端读数");
		assertEquals(stage.count(), plan.withClientStageCounts(List.of(
			new RetailClientHuntProgressRows.Row(0, 1, 99, List.of("some_other_monster_n")),
			new RetailClientHuntProgressRows.Row(0, 2, 99, List.of("some_other_monster_n"))))
			.counters().get(0).required(),
			"多行登记（并行段/多段形）时不得改写真端读数");
		assertEquals(stage.count(), plan.withClientStageCounts(List.of(
			new RetailClientHuntProgressRows.Row(0, 1, 0, List.of("some_other_monster_n"))))
			.counters().get(0).required(),
			"客户端计数为 0（该段无门控）时不得改写真端读数");
	}

	private static int clientCount(List<RetailClientHuntProgressRows.Row> rows) {
		if (rows == null || rows.size() != 1) {
			throw new IllegalStateException("expected exactly one registered client row for the stage");
		}
		return rows.getFirst().count();
	}

	private static RetailSimpleHuntPlan bindPlan(int questId, Stage stage) {
		return RetailSimpleHuntPlan.bind(new RetailSimpleHuntTable.Entry(questId,
			List.of(new RetailSimpleHuntTable.Counter(1, stage.count(), stage.monsters())),
			"", "", null, RetailGrantKind.AREA), npcIndex);
	}

	private static Set<String> reviewedDisagreements() {
		Set<String> ids = new TreeSet<>();
		for (int quest = 13758; quest <= 13769; quest++) {
			ids.add(String.valueOf(quest));
		}
		for (int quest = 23758; quest <= 23769; quest++) {
			ids.add(String.valueOf(quest));
		}
		ids.add("13770");
		ids.add("23770");
		for (String quest : List.of("15571", "15573", "15576", "15578",
				"25571", "25573", "25576", "25578",
				"13841", "13845", "13849", "23841", "23845", "23849")) {
			ids.add(quest);
		}
		return Set.copyOf(ids);
	}

	private static List<String> blocks(String text) {
		List<String> found = new ArrayList<>();
		Matcher matcher = Pattern.compile("<quest_data_driven>.*?</quest_data_driven>", Pattern.DOTALL)
			.matcher(text);
		while (matcher.find()) {
			found.add(matcher.group());
		}
		return found;
	}

	private static List<String> lines(InputStream input) throws Exception {
		try (input) {
			return new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8).lines().toList();
		}
	}

	private static List<InputStream> openAll(String dir, List<String> files) throws Exception {
		List<InputStream> inputs = new ArrayList<>();
		for (String file : files) {
			inputs.add(open(dir + file));
		}
		return inputs;
	}

	private static InputStream open(String resource) throws Exception {
		InputStream input = RetailHuntClientCountGateTest.class.getResourceAsStream(resource);
		if (input == null) {
			throw new IllegalStateException("missing resource " + resource);
		}
		return input;
	}
}
