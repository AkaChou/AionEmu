package com.aionemu.gameserver.questEngine.tablelane;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.questEngine.retail.RetailGrantKind;
import com.aionemu.gameserver.questEngine.retail.RetailItemNameIndex;

/**
 * SimpleTalk 族级**逐行对齐门**（P3 重锚，计划 §8.9）：以真端表/quest.xml 的**独立重解析**为唯一事实，
 * 逐行对拍 native 映射；不复用 {@link NativeQuestTableLoader} 的 DOM 装载路径，也不保留任何旧 IR 形状
 * （节点名 / 条件 / 动作 / 页链残留）断言。
 * <p>
 * 取数入口：{@code aion/data/static_data/quest/retail/Quest_SimpleTalk.xml}（正则逐行）、
 * {@code .../quest.xml}（交付门通道）、{@link RetailItemNameIndex}（物品 name_desc→id，数据源）与
 * {@link NativeNpcNameResolver}（NPC 名→id，数据源）。规则（两通道符号、唯一解析、门候选序）在本类内
 * 独立实现，避免"用被测实现证明被测实现"。
 * <p>
 * Family-wide per-row alignment gate for SimpleTalk: the retail table and quest.xml are re-parsed
 * independently (regex, not the DOM loader) and compared row by row against the native mapping; no
 * legacy IR-shape assertions survive in this lane's goldens.
 */
class SimpleTalkRowAlignmentGateTest {

	private static final String TALK_RESOURCE = "aion/data/static_data/quest/retail/Quest_SimpleTalk.xml";
	private static final String QUEST_XML_RESOURCE = "aion/data/static_data/quest/retail/quest.xml";

	/** 真端表事实（全量复算）。 / Retail-table facts (whole-table recomputation). */
	private static final int ROWS = 3152;
	private static final int RELAY_ROWS = 468;
	private static final int TALK_NPC1_ROWS = 468;
	private static final int TALK_NPC2_ROWS = 193;
	private static final int TALK_NPC3_ROWS = 68;
	private static final int ITEM_CELLS = 663;
	private static final int ACCEPT_GIVE_ROWS = 407;
	private static final int ITEM_CHECK_ROWS = 1988;
	private static final int GATE_ROWS = 1981;
	private static final int CON_QUEST_ROWS = 492;
	private static final int CUTSCENE_ROWS = 67;
	/** 其他 retail 族表（跨族 {@code con_quest} 目标的接取 NPC 来源）。 / Sibling retail family tables. */
	private static final List<String> SIBLING_RESOURCES = List.of(
		"aion/data/static_data/quest/retail/Quest_SimpleHunt.xml",
		"aion/data/static_data/quest/retail/Quest_SimpleCollectItem.xml",
		"aion/data/static_data/quest/retail/Quest_CombineTask.xml",
		"aion/data/static_data/quest/retail/Quest_SimpleUseItem.xml",
		"aion/data/static_data/quest/retail/Quest_SimpleItemPlay.xml",
		"aion/data/static_data/quest/retail/Quest_SimpleSerialHunt.xml");
	/** 链式接取窗目标的分布（真端全表冻结）。 / Distribution of the chain-window targets (frozen). */
	private static final int CHAIN_TARGET_IN_TABLE = 308;
	private static final int CHAIN_TARGET_SIBLING = 89;
	private static final int CHAIN_TARGET_NO_ROW = 95;
	private static final int CHAIN_TARGET_NO_ROW_XML_OWNED = 70;
	/** 跨族例外：目标由「用物品」接取，真端该行没有接取 NPC 列。 / Cross-family exception: use-item targets. */
	private static final Set<Integer> CHAIN_TARGET_USE_ITEM = Set.of(80197, 80201);
	/** 门通道全缺且真端不可接取的行（`client_level`/`minlevel_permitted`=999）。 / Gate-less, unreachable rows. */
	private static final Set<Integer> UNREACHABLE_GATE_ROWS =
			Set.of(2732, 30509, 41571, 50011, 50012, 51011, 51012);

	private static final Pattern ROW = Pattern.compile("<id id=\"(\\d+)\">(.*?)</id>", Pattern.DOTALL);
	private static final Pattern FIELD = Pattern.compile("<(\\w+)>(.*?)</\\1>", Pattern.DOTALL);
	private static final Pattern QUEST = Pattern.compile("<quest>(.*?)</quest>", Pattern.DOTALL);

	/** DOCTYPE 实体声明（真端文件自带 DTD：`&hellip;` 展开为字面量 "hellip"，不是 U+2026）。 */
	private static final Pattern ENTITY = Pattern.compile("<!ENTITY\\s+(\\w+)\\s+\"([^\"]*)\"\\s*>");

	private static Map<Integer, Map<String, String>> tableRaw;
	private static Map<Integer, Map<String, List<String>>> questXmlRaw;
	private static SimpleTalkHandler handler;
	private static RetailItemNameIndex items;

	@BeforeAll
	static void setUp() throws Exception {
		tableRaw = parseTable(readResource(TALK_RESOURCE));
		questXmlRaw = parseQuestXml(readResource(QUEST_XML_RESOURCE));
		handler = SimpleTalkHandler.instance();
		items = RetailItemNameIndex.loadItemTemplates();
	}

	/** ① 行集与逐行字段：native 映射必须与真端表原文逐行一致（含长尾列）。 */
	@Test
	void everyRetailRowIsMappedVerbatim() {
		assertEquals(ROWS, tableRaw.size(), "真端表行数漂移 / retail row count drifted");
		assertEquals(tableRaw.keySet(), handler.ownedQuestIds(),
				"注册集必须逐行等于真端表 id 集 / registration set must equal the retail id set");

		for (Map.Entry<Integer, Map<String, String>> entry : tableRaw.entrySet()) {
			int questId = entry.getKey();
			Map<String, String> raw = entry.getValue();
			NativeQuestTableLoader.SimpleTalkRow row = handler.requireRow(questId);

			assertEquals(raw.getOrDefault("dev_name", ""), row.devName(), "dev_name: " + questId);
			assertEquals(value(raw, "acquired_npc_name"), row.acquiredNpcName(), "acquired_npc_name: " + questId);
			assertEquals(value(raw, "reward_npc_name"), row.rewardNpcName(), "reward_npc_name: " + questId);
			assertEquals(value(raw, "give_item"), row.acceptGiveItem(), "give_item: " + questId);
			assertEquals(value(raw, "item_check"), row.itemCheck() ? "1" : null, "item_check: " + questId);
			assertEquals(integer(raw, "con_quest"), row.conQuest(), "con_quest: " + questId);
			assertEquals(integer(raw, "cutsceneid1"), row.cutsceneId(), "cutsceneid1: " + questId);
			assertEquals(integer(raw, "cs1_haction"), row.cutsceneAction(), "cs1_haction: " + questId);

			List<String> relaying = new ArrayList<>();
			for (int step = 1; step <= 3; step++) {
				String talk = value(raw, "talk_npc" + step);
				if (talk != null) {
					relaying.add(talk);
				}
			}
			assertEquals(relaying, row.talkNpcNames(), "talk_npc1..3: " + questId);
			for (int step = 1; step <= 3; step++) {
				assertEquals(value(raw, "give_item" + step), row.stepGiveItems().get(step - 1),
						"give_item" + step + ": " + questId);
				assertEquals(value(raw, "remove_item" + step), row.stepRemoveItems().get(step - 1),
						"remove_item" + step + ": " + questId);
			}
		}
	}

	/** ② 单一 owner：路由集 = 注册集 − XML-only 行；两集合互补且不交叠。 */
	@Test
	void routingSetIsTheRegistrationSetMinusXmlRetainedRows() {
		Set<Integer> xmlOnly = NativeQuestOwnerResolver.instance().xmlOnlyIds();
		int routed = 0;
		for (int questId : handler.ownedQuestIds()) {
			boolean expectRouted = !xmlOnly.contains(questId);
			assertEquals(expectRouted, handler.routes(questId),
					"路由/注册不一致（XML-only 行必须让位）/ routing must yield to XML-only rows: " + questId);
			routed += expectRouted ? 1 : 0;
		}
		assertEquals(ROWS - 18, routed, "XML_RETENTION 18 行必须让位 / the 18 XML-retained rows must yield");
		assertTrue(java.util.Collections.disjoint(handler.routedQuestIds(), xmlOnly),
				"路由集与 XML-only 集不得交叠 / routing set must not intersect the XML-owned set");
	}

	/** ③ 物品通道：接取发放 / 步内发扣 / 交付门，逐行与真端原文独立复算一致。 */
	@Test
	void itemChannelsMatchTheRetailTableRecomputation() {
		int gateRows = 0;
		for (int questId : handler.ownedQuestIds()) {
			Map<String, String> raw = tableRaw.get(questId);
			assertEquals(stack(value(raw, "give_item")), handler.acceptGiveItem(questId),
					"接取发放漂移 / accept grant drifted: " + questId);
			for (int step = 1; step <= 3; step++) {
				assertEquals(stack(value(raw, "give_item" + step)), handler.stepGiveItem(questId, step),
						"第 " + step + " 步发放漂移 / step grant drifted: " + questId);
				assertEquals(stack(value(raw, "remove_item" + step)), handler.stepRemoveItem(questId, step),
						"第 " + step + " 步扣除漂移 / step removal drifted: " + questId);
			}
			if (!handler.requireRow(questId).itemCheck()) {
				assertTrue(handler.workItems(questId).isEmpty(),
						"非 item_check 行不得有交付门 / non item_check rows carry no gate: " + questId);
				assertFalse(handler.unresolvedGate(questId), "非 item_check 行不得 fail-closed: " + questId);
				continue;
			}
			List<String> declared = declaredGateSymbols(questId);
			if (declared.isEmpty()) {
				// 门通道全缺：真端无收集通道 ⇒ 报告门不可达（行方 client_level/minlevel=999）。
				assertTrue(handler.workItems(questId).isEmpty(), "门缺通道行不得有门物品: " + questId);
				assertEquals(UNREACHABLE_GATE_ROWS.contains(questId), handler.unresolvedGate(questId),
						"门缺通道行集漂移（只允许冻结的 7 行）/ gate-less row set drifted: " + questId);
				continue;
			}
			List<SimpleTalkHandler.ItemStack> declaredStacks = resolved(declared);
			boolean fullyResolved = declaredStacks.size() == declared.size();
			if (fullyResolved) {
				gateRows++;
				assertFalse(handler.unresolvedGate(questId), "可解门不得 fail-closed: " + questId);
				assertEquals(declaredStacks, handler.workItems(questId),
						"交付门物品漂移 / gate items drifted: " + questId);
			} else {
				assertTrue(handler.unresolvedGate(questId),
						"门含未解符号必须 fail-closed / a gate with unresolved symbols must fail closed: " + questId);
				assertTrue(handler.workItems(questId).isEmpty(),
						"fail-closed 行不得留可解子集 / a failing gate keeps no resolvable subset: " + questId);
			}
		}
		assertEquals(ITEM_CHECK_ROWS, tableRaw.values().stream()
				.filter(raw -> "1".equals(value(raw, "item_check"))).count(), "item_check 行数漂移");
		assertEquals(GATE_ROWS, gateRows, "门成立行数漂移 / resolvable gate rows drifted");
		for (int questId : UNREACHABLE_GATE_ROWS) {
			assertTrue(handler.unresolvedGate(questId) && handler.workItems(questId).isEmpty(),
					"门通道全缺行必须冻结为 fail-closed: " + questId);
		}
	}

	/** ④ 全表计数冻结（族级事实，任何一行变化即红灯）。 */
	@Test
	void familyCountsAreFrozenOverTheWholeTable() {
		int relayRows = 0;
		int talk1 = 0;
		int talk2 = 0;
		int talk3 = 0;
		int itemCells = 0;
		int acceptGive = 0;
		int conQuest = 0;
		int cutscene = 0;
		for (Map<String, String> raw : tableRaw.values()) {
			if (value(raw, "talk_npc1") != null) {
				relayRows++;
				talk1++;
			}
			talk2 += value(raw, "talk_npc2") != null ? 1 : 0;
			talk3 += value(raw, "talk_npc3") != null ? 1 : 0;
			for (String cell : List.of("give_item", "give_item1", "give_item2", "give_item3",
					"remove_item1", "remove_item2", "remove_item3")) {
				itemCells += value(raw, cell) != null ? 1 : 0;
			}
			acceptGive += value(raw, "give_item") != null ? 1 : 0;
			conQuest += value(raw, "con_quest") != null ? 1 : 0;
			cutscene += value(raw, "cutsceneid1") != null ? 1 : 0;
		}
		assertEquals(RELAY_ROWS, relayRows);
		assertEquals(TALK_NPC1_ROWS, talk1);
		assertEquals(TALK_NPC2_ROWS, talk2);
		assertEquals(TALK_NPC3_ROWS, talk3);
		assertEquals(ITEM_CELLS, itemCells);
		assertEquals(ACCEPT_GIVE_ROWS, acceptGive);
		assertEquals(CON_QUEST_ROWS, conQuest);
		assertEquals(CUTSCENE_ROWS, cutscene);

		// 中继页阶梯与接取/报告页（真端分派器字面量，族级常量）。
		assertEquals(1352, SimpleTalkHandler.pageForStep(1));
		assertEquals(1693, SimpleTalkHandler.pageForStep(2));
		assertEquals(2034, SimpleTalkHandler.pageForStep(3));

		// 系统发放入口只对「本服确有发放入口」的哨兵行成立（`_challengetask_` 只有完成回调 ⇒ 不放行），
		// 且哨兵行必定未解析为 NPC。
		// The system-grant entry exists only for sentinels this server can actually grant
		// (`_challengetask_` has a completion callback only), and sentinel rows never resolve to an NPC.
		for (int questId : handler.ownedQuestIds()) {
			RetailGrantKind kind = handler.grantKind(questId);
			boolean grantable = kind != RetailGrantKind.NPC && kind.grantable();
			assertEquals(grantable && handler.routes(questId), handler.isSystemGranted(questId),
					"系统发放判定漂移（XML 保留行与无入口哨兵必须让位）: " + questId);
			if (kind.knownGrant()) {
				assertNull(handler.acquireNpc(questId), "系统发放行不得有接取 NPC: " + questId);
			}
		}
	}

	/**
	 * 链式接取窗（真端 0x1e 槽）：每行 {@code con_quest} 指出的下一环必须能在本行的交付 NPC 处接取。
	 * <p>
	 * 本车道按 NPC 建接取路由 ⇒「下一环的接取 NPC == 本行交付 NPC」即窗口成立，不需要第二套路由；
	 * 目标没有真端表行的行由目标自身的 owner（XML 车道，或不在本服宇宙）负责，登记为冻结证据。
	 * The chain accept window (retail slot 0x1e): the next quest named by {@code con_quest} must be
	 * acquirable at this row's reward NPC. Targets without a retail row are owned elsewhere and frozen.
	 */
	@Test
	void chainAcquireWindowsCloseAtTheRewardNpc() throws Exception {
		Map<Integer, Map<String, String>> siblings = new HashMap<>();
		for (String resource : SIBLING_RESOURCES) {
			parseTable(readResource(resource)).forEach(siblings::putIfAbsent);
		}
		Set<Integer> xmlOwned = NativeQuestOwnerResolver.instance().xmlOnlyIds();
		int inTable = 0;
		int inSibling = 0;
		int noRow = 0;
		int noRowXmlOwned = 0;
		Set<Integer> useItemTargets = new TreeSet<>();
		List<String> mismatches = new ArrayList<>();
		for (Map.Entry<Integer, Map<String, String>> entry : tableRaw.entrySet()) {
			int questId = entry.getKey();
			Integer next = integer(entry.getValue(), "con_quest");
			if (next == null) {
				continue;
			}
			assertEquals(next, handler.conQuest(questId), "con_quest 装载漂移 / mapping: " + questId);
			String rewardName = value(entry.getValue(), "reward_npc_name");
			Map<String, String> target = tableRaw.get(next);
			if (target != null) {
				inTable++;
			} else if (siblings.containsKey(next)) {
				inSibling++;
				target = siblings.get(next);
			} else {
				noRow++;
				if (xmlOwned.contains(next)) {
					noRowXmlOwned++;
				}
				continue;
			}
			String targetAcquire = value(target, "acquired_npc_name");
			if (Objects.equals(rewardName, targetAcquire)) {
				continue;
			}
			if (targetAcquire == null) {
				// 目标靠「用物品」接取（真端该行没有接取 NPC 列）⇒ 窗口不属于本车道。
				// The target is acquired by using an item, so the window is not this lane's business.
				useItemTargets.add(next);
			} else {
				mismatches.add(questId + "->" + next + " reward=" + rewardName
					+ " targetAcquire=" + targetAcquire);
			}
		}
		assertTrue(mismatches.isEmpty(), () -> "链式接取窗未在本行交付 NPC 上闭环: " + mismatches);
		assertEquals(CHAIN_TARGET_USE_ITEM, useItemTargets, "跨族用物品目标集漂移");
		assertEquals(CHAIN_TARGET_IN_TABLE, inTable, "本表内目标数漂移");
		assertEquals(CHAIN_TARGET_SIBLING, inSibling, "跨族目标数漂移");
		assertEquals(CHAIN_TARGET_NO_ROW, noRow, "无表行目标数漂移");
		assertEquals(CHAIN_TARGET_NO_ROW_XML_OWNED, noRowXmlOwned, "无表行目标里的 XML-owner 数漂移");
		assertTrue(handler.unresolvedChainQuestIds().isEmpty(),
			() -> "本族链式接取窗未闭环: " + handler.unresolvedChainQuestIds());
	}

	// ── 独立重解析与复算 ────────────────────────────────────────────────────────────────────────────

	/** 交付门声明符号：quest.xml `collect_item1..N`；为空时取 `quest_work_item1..N` 首项。 */
	private static List<String> declaredGateSymbols(int questId) {
		Map<String, List<String>> quest = questXmlRaw.get(questId);
		if (quest == null) {
			return List.of();
		}
		List<String> collect = numbered(quest, "collect_item");
		if (!collect.isEmpty()) {
			return collect;
		}
		List<String> work = numbered(quest, "quest_work_item");
		return work.isEmpty() ? List.of() : List.of(work.getFirst());
	}

	private static List<String> numbered(Map<String, List<String>> quest, String prefix) {
		Map<Integer, String> ordered = new TreeMap<>();
		for (Map.Entry<String, List<String>> entry : quest.entrySet()) {
			Matcher matcher = Pattern.compile(prefix + "(\\d*)").matcher(entry.getKey());
			if (matcher.matches()) {
				ordered.put(matcher.group(1).isEmpty() ? 0 : Integer.parseInt(matcher.group(1)),
						entry.getValue().getFirst());
			}
		}
		return ordered.values().stream().filter(text -> !text.isBlank()).toList();
	}

	private static List<SimpleTalkHandler.ItemStack> resolved(List<String> symbols) {
		List<SimpleTalkHandler.ItemStack> stacks = new ArrayList<>();
		for (String symbol : symbols) {
			SimpleTalkHandler.ItemStack stack = stack(symbol);
			if (stack != null) {
				stacks.add(stack);
			}
		}
		return stacks;
	}

	/**
	 * 真端符号 → 物品栈（两通道约定）：表 give/remove 列是 {@code ITEM_X} 形，quest.xml 交付列是原名形
	 * （含 {@code item_*} 真名）⇒ 先按原名查，未命中再去 {@code ITEM_} 前缀重查。
	 */
	private static SimpleTalkHandler.ItemStack stack(String symbol) {
		if (symbol == null) {
			return null;
		}
		String[] parts = symbol.trim().split("\\s+");
		if (parts.length == 0 || parts[0].isBlank()) {
			return null;
		}
		String stem = parts[0].toLowerCase(java.util.Locale.ROOT);
		Integer itemId = items.resolve(stem);
		if (itemId == null && stem.startsWith("item_")) {
			itemId = items.resolve(stem.substring("item_".length()));
		}
		if (itemId == null) {
			return null;
		}
		int count = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;
		return new SimpleTalkHandler.ItemStack(itemId, count);
	}

	private static Map<Integer, Map<String, String>> parseTable(String text) {
		Map<String, String> entities = entities(text);
		Map<Integer, Map<String, String>> rows = new LinkedHashMap<>();
		Matcher row = ROW.matcher(text);
		while (row.find()) {
			Map<String, String> fields = new LinkedHashMap<>();
			Matcher field = FIELD.matcher(row.group(2));
			while (field.find()) {
				fields.put(field.group(1), decode(field.group(2), entities).trim());
			}
			rows.put(Integer.parseInt(row.group(1)), fields);
		}
		return rows;
	}

	private static Map<Integer, Map<String, List<String>>> parseQuestXml(String text) {
		Map<String, String> entities = entities(text);
		Map<Integer, Map<String, List<String>>> quests = new LinkedHashMap<>();
		Matcher quest = QUEST.matcher(text);
		while (quest.find()) {
			Map<String, List<String>> fields = new LinkedHashMap<>();
			Matcher field = FIELD.matcher(quest.group(1));
			while (field.find()) {
				fields.computeIfAbsent(field.group(1), key -> new ArrayList<>())
						.add(decode(field.group(2), entities).trim());
			}
			List<String> ids = fields.get("id");
			if (ids != null && !ids.isEmpty()) {
				quests.put(Integer.parseInt(ids.getFirst()), fields);
			}
		}
		return quests;
	}

	/** 从文件自带 DOCTYPE 提取实体表（与 DOM 装载器的实体展开口径一致）。 */
	private static Map<String, String> entities(String text) {
		Map<String, String> entities = new LinkedHashMap<>();
		Matcher matcher = ENTITY.matcher(text);
		while (matcher.find()) {
			entities.put(matcher.group(1), matcher.group(2));
		}
		// XML 预定义五实体不受 DTD 重定义影响（DOM 口径）：真端 DTD 里 gt/lt/amp 等声明为字面量，
		// 但解析器按规范始终展开为标准字符。
		entities.put("amp", "&");
		entities.put("lt", "<");
		entities.put("gt", ">");
		entities.put("apos", "'");
		entities.put("quot", "\"");
		return entities;
	}

	private static String decode(String text, Map<String, String> entities) {
		String out = text;
		for (Map.Entry<String, String> entity : entities.entrySet()) {
			out = out.replace("&" + entity.getKey() + ";", entity.getValue());
		}
		return out;
	}

	private static String value(Map<String, String> raw, String key) {
		String text = raw.get(key);
		return text == null || text.isBlank() ? null : text;
	}

	private static Integer integer(Map<String, String> raw, String key) {
		String text = value(raw, key);
		return text == null ? null : Integer.valueOf(text);
	}

	private static String readResource(String path) throws Exception {
		try (InputStream input = SimpleTalkRowAlignmentGateTest.class.getClassLoader().getResourceAsStream(path)) {
			if (input == null) {
				throw new IllegalStateException("missing resource " + path);
			}
			return new String(input.readAllBytes(), StandardCharsets.UTF_8);
		}
	}
}
