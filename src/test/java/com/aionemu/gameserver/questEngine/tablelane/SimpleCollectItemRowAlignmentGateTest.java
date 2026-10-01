package com.aionemu.gameserver.questEngine.tablelane;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
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

/**
 * SimpleCollectItem 族级**逐行对齐门**（P4 收口，计划 §8.9）：以真端表 {@code Quest_SimpleCollectItem.xml}
 * 与 {@code quest.xml} 的**独立重解析**为唯一事实，逐行对拍 native 映射；不复用
 * {@link NativeQuestTableLoader} 的 DOM 装载路径，也不保留任何旧 IR 形状（typed 节点 / 条件 / 动作 /
 * SELECT 页链 / 掉落 {@code collectingStep}）断言。
 * <p>
 * 取数入口：{@code aion/data/static_data/quest/retail/Quest_SimpleCollectItem.xml}（正则逐行）、
 * {@code .../quest.xml}（交付列 {@code collect_itemN} 与掉落列 {@code drop_*}）与
 * {@link NativeNpcNameResolver}（NPC/对象名→id，数据源）。规则（两通道符号、唯一解析、槽 = 交付列序）
 * 在本类内独立实现，避免「用被测实现证明被测实现」。
 * <p>
 * Family-wide per-row alignment gate for SimpleCollectItem: the retail table and quest.xml are re-parsed
 * independently (regex, not the DOM loader) and compared row by row against the native mapping; no
 * legacy IR-shape assertions survive in this lane's goldens.
 */
class SimpleCollectItemRowAlignmentGateTest {

	private static final String COLLECT_RESOURCE =
		"aion/data/static_data/quest/retail/Quest_SimpleCollectItem.xml";
	private static final String QUEST_XML_RESOURCE = "aion/data/static_data/quest/retail/quest.xml";

	/** 真端表事实（全量复算）。 / Retail-table facts (whole-table recomputation). */
	private static final int ROWS = 262;
	private static final int OBJECT1_ROWS = 257;
	private static final int OBJECT2_ROWS = 22;
	private static final int OBJECT3_ROWS = 13;
	private static final int OBJECT4_ROWS = 8;
	private static final int OBJECT_COLUMNS = 300;
	private static final int TALK_NPC1_ROWS = 5;
	private static final int TALK_NPC2_ROWS = 2;
	private static final int TALK_NPC3_ROWS = 1;
	private static final int PARTY_DROP_ROWS = 80;
	private static final int CON_QUEST_ROWS = 37;
	private static final int CUTSCENE_ROWS = 2;
	private static final int GIVE_ITEM_ROWS = 6;
	private static final int GIVE_ITEM1_ROWS = 1;
	private static final int REMOVE_ITEM2_ROWS = 1;
	private static final int REWARD_CHECK_ROWS = 5;
	/** {@code collect_itemN} 列覆盖（253 / 27 / 13 行）。 / Hand-in column coverage. */
	private static final int COLLECT_ITEM1_ROWS = 253;
	private static final int COLLECT_ITEM2_ROWS = 27;
	private static final int COLLECT_ITEM3_ROWS = 13;
	/** 可派生相机行（253 有计数 − 3 行真端休眠 minlevel=999）。 / Derivable camera rows. */
	private static final int CAMERA_ROWS = 250;
	/** 真端 minlevel=999 的休眠行（元数据 min>max，不派生相机行）。 / Dormant retail rows. */
	private static final Set<Integer> DORMANT_LEVEL_ROWS = Set.of(36017, 46017, 47112);
	/** 表格声明了对象但真端不给槽位的行（41216 的来源是另一个 FOBJ）。 / Object without a retail slot. */
	private static final Set<Integer> OBJECT_WITHOUT_SLOT_ROWS = Set.of(41216);
	/** 无采集计数的行（TEST 5 + 事件行 4）：不可路由。 / Rows without a collect count. */
	private static final Set<Integer> NO_COLLECT_COUNT_ROWS =
		Set.of(9649, 9650, 9654, 9655, 9656, 50017, 50018, 51017, 51018);
	/** XML-only 行（注册集里让位给 XML 车道）。 / XML-owned rows that yield to the XML lane. */
	private static final Set<Integer> XML_ONLY_ROWS = Set.of(2237);
	/**
	 * 复合势力交付名且客户端登记缺失的行（39611/49611：{@code LDF5b_Silverlin_LD}，真端 min/max/client
	 * level 全为 999 的停用行）。真端名册无此名、客户端交付登记（{@code quest_client_reward_npcs.tsv}）
	 * 亦未登记 ⇒ 交付面不存在，按 fail-closed 不路由（退役编译器同判 {@code RETAIL_REWARD_NPC_FACTION_COMPOSITE}）。
	 * Composite faction reward names with no client registration (the level-999 disabled rows 39611/49611):
	 * the retail name index has no such npc and the client reward-npc registry declares none, so the
	 * hand-in face does not exist and the row fails closed (the retired compiler rejected it the same way).
	 */
	private static final Set<Integer> COMPOSITE_REWARD_WITHOUT_CLIENT_SET_ROWS = Set.of(39611, 49611);

	private static final Pattern ROW = Pattern.compile("<id id=\"(\\d+)\">(.*?)</id>", Pattern.DOTALL);
	private static final Pattern FIELD = Pattern.compile("<(\\w+)>(.*?)</\\1>", Pattern.DOTALL);
	private static final Pattern QUEST = Pattern.compile("<quest>(.*?)</quest>", Pattern.DOTALL);
	private static final Pattern ENTITY = Pattern.compile("<!ENTITY\\s+(\\w+)\\s+\"([^\"]*)\"\\s*>");

	private static Map<Integer, Map<String, String>> tableRaw;
	private static Map<Integer, Map<String, List<String>>> questXmlRaw;
	private static SimpleCollectItemHandler handler;

	@BeforeAll
	static void setUp() throws Exception {
		tableRaw = parseTable(readResource(COLLECT_RESOURCE));
		questXmlRaw = parseQuestXml(readResource(QUEST_XML_RESOURCE));
		handler = SimpleCollectItemHandler.instance();
	}

	/** ① 行集与逐行字段：native 映射必须与真端表原文逐行一致（含长尾列）。 */
	@Test
	void everyRetailRowIsMappedVerbatim() {
		assertEquals(ROWS, tableRaw.size(), "真端表行数漂移 / retail row count drifted");
		NativeQuestTableLoader loader = NativeQuestTableLoader.instance();
		assertEquals(tableRaw.keySet(), handler.ownedQuestIds(),
			"注册集必须逐行等于真端表 id 集 / registration set must equal the retail id set");
		assertEquals(tableRaw.size(), loader.collectSize(), "装载器行数必须等于真端表行数");

		for (Map.Entry<Integer, Map<String, String>> entry : tableRaw.entrySet()) {
			int questId = entry.getKey();
			Map<String, String> raw = entry.getValue();
			NativeQuestTableLoader.SimpleCollectItemRow row = loader.requireCollect(questId);

			assertEquals(raw.getOrDefault("dev_name", ""), row.devName(), "dev_name: " + questId);
			assertEquals(value(raw, "acquired_npc_name"), row.acquiredNpcName(), "acquired_npc_name: " + questId);
			assertEquals(value(raw, "reward_npc_name"), row.rewardNpcName(), "reward_npc_name: " + questId);
			assertEquals(value(raw, "give_item"), row.acceptGiveItem(), "give_item: " + questId);
			assertEquals(value(raw, "give_item1"), row.stepGiveItem(), "give_item1: " + questId);
			assertEquals(value(raw, "remove_item2"), row.removeItem2(), "remove_item2: " + questId);
			assertEquals(value(raw, "con_quest") == null ? null : Integer.valueOf(value(raw, "con_quest")),
				row.conQuest(), "con_quest: " + questId);
			assertEquals(value(raw, "cutsceneid1") == null ? null : Integer.valueOf(value(raw, "cutsceneid1")),
				row.cutsceneId(), "cutsceneid1: " + questId);
			assertEquals(value(raw, "cs1_haction") == null ? null : Integer.valueOf(value(raw, "cs1_haction")),
				row.cutsceneAction(), "cs1_haction: " + questId);
			assertEquals(value(raw, "party_drop") != null, row.partyDrop(), "party_drop: " + questId);

			List<String> objects = new ArrayList<>();
			for (int column = 1; column <= 4; column++) {
				String object = value(raw, "object" + column);
				if (object != null) {
					objects.add(object);
				}
			}
			assertEquals(objects, row.objects(), "object1..4: " + questId);
			List<String> talks = new ArrayList<>();
			for (int step = 1; step <= 3; step++) {
				String talk = value(raw, "talk_npc" + step);
				if (talk != null) {
					talks.add(talk);
				}
			}
			assertEquals(talks, row.talkNpcNames(), "talk_npc1..3: " + questId);
		}
	}

	/** ② 单一 owner：路由集 = 注册集 − XML-only 行 − 不可路由行；三者互补。 */
	@Test
	void routingSetIsTheRegistrationSetMinusXmlAndUnslottableRows() {
		Set<Integer> xmlOnly = NativeQuestOwnerResolver.instance().xmlOnlyIds();
		assertEquals(XML_ONLY_ROWS, new TreeSet<>(xmlOnly).stream()
				.filter(tableRaw::containsKey).collect(java.util.stream.Collectors.toCollection(TreeSet::new)),
			"本族 XML-only 行（真端表 ∩ XML 保留）必须冻结");
		for (int questId : handler.ownedQuestIds()) {
			boolean expectRouted = !xmlOnly.contains(questId)
				&& !DORMANT_LEVEL_ROWS.contains(questId)
				&& !NO_COLLECT_COUNT_ROWS.contains(questId)
				&& !OBJECT_WITHOUT_SLOT_ROWS.contains(questId)
				&& !COMPOSITE_REWARD_WITHOUT_CLIENT_SET_ROWS.contains(questId);
			assertEquals(expectRouted, handler.routes(questId),
				"路由/注册不一致 / routing differs from the derived verdict: " + questId);
		}
		assertEquals(ROWS - XML_ONLY_ROWS.size() - DORMANT_LEVEL_ROWS.size()
			- NO_COLLECT_COUNT_ROWS.size() - OBJECT_WITHOUT_SLOT_ROWS.size()
			- COMPOSITE_REWARD_WITHOUT_CLIENT_SET_ROWS.size(),
			handler.routedQuestIds().size(), "路由集大小必须冻结");
		for (int questId : COMPOSITE_REWARD_WITHOUT_CLIENT_SET_ROWS) {
			assertTrue(handler.rewardNpcs(questId).isEmpty(),
				() -> "无客户端登记的复合交付名不得凭空造出交付面: " + questId);
			assertTrue(handler.ownedQuestIds().contains(questId),
				() -> "复合行仍须装载（注册集逐行等于真端表）: " + questId);
		}
		assertTrue(java.util.Collections.disjoint(handler.routedQuestIds(), xmlOnly),
			"路由集与 XML-only 集不得交叠 / the routing set must not intersect the XML-owned set");
	}

	/** ③ 相机行与真端交付计数逐行一致（槽 = 交付列序）。 */
	@Test
	void cameraRowsFollowTheRetailCollectColumns() {
		CameraRegistry registry = CameraRegistry.instance();
		int derived = 0;
		for (int questId : tableRaw.keySet()) {
			List<String> collected = questNumbered(questId, "collect_item");
			if (collected.isEmpty()) {
				assertTrue(registry.find(questId).isEmpty(), "无交付列不得派生相机行: " + questId);
				continue;
			}
			if (DORMANT_LEVEL_ROWS.contains(questId)) {
				assertTrue(registry.find(questId).isEmpty(), "真端休眠行不得派生相机行: " + questId);
				assertTrue(registry.collectRowsWithoutCamera().contains(questId),
					"休眠行必须登记在未派生相机清单里: " + questId);
				continue;
			}
			derived++;
			for (int slot = 1; slot <= collected.size(); slot++) {
				assertEquals(symbolCount(collected.get(slot - 1)), registry.require(questId).required(slot),
					"槽 " + slot + " required 必须等于真端 collect_item" + slot + ": " + questId);
			}
		}
		assertEquals(CAMERA_ROWS, derived, "可派生相机行数必须冻结");
		assertEquals(CAMERA_ROWS, tableRaw.keySet().stream()
			.filter(questId -> registry.find(questId).isPresent()).count(),
			"本族相机注册行数必须冻结（全库相机表含其他已切换家族）");
		assertEquals(DORMANT_LEVEL_ROWS, registry.collectRowsWithoutCamera(),
			"未派生相机行的采集行 = 3 个 minlevel=999 休眠行");
	}

	/**
	 * ④ 槽对齐（P4 真端复核）：真端 {@code drop_monster_K} 列出的来源产出 {@code drop_item_K}，
	 * 该物品在交付列里的位置就是相机槽；对象列序**不是**槽序（4046/2487/2346/1154/41510 反例）。
	 * The retail drop column decides the slot: the object column order is not the slot order.
	 */
	@Test
	void collectSlotsFollowTheRetailDropColumnsNotTheObjectOrder() {
		int alignedColumns = 0;
		int caseOnlyColumns = 0;
		int multiSourceColumns = 0;
		int monsterColumns = 0;
		for (Map.Entry<Integer, Map<String, String>> entry : tableRaw.entrySet()) {
			int questId = entry.getKey();
			if (!handler.routes(questId)) {
				continue;
			}
			Map<Integer, String> dropSources = new LinkedHashMap<>();
			List<String> collected = questNumbered(questId, "collect_item");
			Map<Integer, String> collectSymbols = new LinkedHashMap<>();
			for (int slot = 1; slot <= collected.size(); slot++) {
				collectSymbols.put(slot, symbol(collected.get(slot - 1)));
			}
			for (int column = 1; column <= 4; column++) {
				String sources = questText(questId, "drop_monster_" + column);
				String item = questText(questId, "drop_item_" + column);
				if (sources == null || item == null) {
					continue;
				}
				// 真端一致性：掉落物必须是交付列里的一项，槽 = 该项的位置。
				String symbol = symbol(item);
				int dropColumn = column;
				assertTrue(collectSymbols.containsValue(symbol),
					() -> "掉落物必须来自交付列: " + questId + " drop_item_" + dropColumn + "=" + item);
				int slot = slotOf(collectSymbols, symbol);
				String object = value(entry.getValue(), "object" + column);
				if (sources.equals(object)) {
					alignedColumns++;
				} else if (object != null && sources.equalsIgnoreCase(object)) {
					caseOnlyColumns++;
				} else if (sources.split("\\s+").length > 1) {
					multiSourceColumns++;
				} else {
					monsterColumns++;
				}
				int dropSlot = slot;
				for (String source : sources.split("\\s+")) {
					List<Integer> ids = NativeNpcNameResolver.instance().resolveMonsterIds(source);
					assertEquals(1, ids.size(), () -> "真端来源名必须唯一解析: " + source);
					dropSources.put(dropSlot, source);
					assertEquals(dropSlot, handler.collectSources(questId).get(ids.getFirst()),
						() -> "来源 " + source + " 的相机槽必须等于掉落列物品在交付列里的位置: " + questId);
				}
			}
			// 表对象列：有槽的必须有引用，无槽的必须列为不可路由。
			Set<Integer> objectIds = new TreeSet<>();
			for (int column = 1; column <= 4; column++) {
				String object = value(entry.getValue(), "object" + column);
				if (object == null) {
					continue;
				}
				List<Integer> ids = NativeNpcNameResolver.instance().resolveMonsterIds(object);
				if (ids.size() != 1) {
					continue;
				}
				objectIds.add(ids.getFirst());
				if (dropSources.containsValue(object)) {
					assertTrue(handler.collectObjects(questId).contains(ids.getFirst()),
						() -> "有槽的对象必须装载为采集对象: " + questId + " " + object);
				}
			}
			assertFalse(objectIds.isEmpty(), () -> "有对象列的行必须解析出对象 id: " + questId);
		}
		// 真端冻结（限本族可路由行）：K 列与 objectK 同名的列 273 个、仅大小写差异 8 个、
		// 一个掉落列列多个来源 3 个、掉落源是真怪（对象只是线索）3 个 —— 正因如此，槽不能按对象列序取。
		// 39611/49611 两行（各有一个同名对齐列）因交付面缺失不路由，故不在这 273 个之内。
		// Frozen over the routed rows: the object column order is not the slot order (273 aligned, 8
		// case-only, 3 multi-source, 3 real-monster drop columns). The two unrouted composite rows
		// 39611/49611 would each add one aligned column but are excluded with the whole routing scope.
		assertEquals(273, alignedColumns, "objectK 与 drop_monster_K 同名的列数必须冻结（路由集口径）");
		assertEquals(8, caseOnlyColumns, "仅大小写差异的列数必须冻结");
		assertEquals(3, multiSourceColumns, "一个掉落列列出多个来源的列数必须冻结");
		assertEquals(3, monsterColumns, "掉落源是真怪（对象只是交付线索）的列数必须冻结");
	}

	/** ⑤ 全表计数冻结（族级事实，任何一行变化即红灯）。 */
	@Test
	void familyCountsAreFrozenOverTheWholeTable() {
		int[] objectColumns = new int[5];
		int talk1 = 0;
		int talk2 = 0;
		int talk3 = 0;
		int partyDrop = 0;
		int conQuest = 0;
		int cutscene = 0;
		int giveItem = 0;
		int giveItem1 = 0;
		int removeItem2 = 0;
		int rewardCheck = 0;
		int collect1 = 0;
		int collect2 = 0;
		int collect3 = 0;
		for (Map<String, String> raw : tableRaw.values()) {
			for (int column = 1; column <= 4; column++) {
				objectColumns[column] += value(raw, "object" + column) != null ? 1 : 0;
			}
			talk1 += value(raw, "talk_npc1") != null ? 1 : 0;
			talk2 += value(raw, "talk_npc2") != null ? 1 : 0;
			talk3 += value(raw, "talk_npc3") != null ? 1 : 0;
			partyDrop += value(raw, "party_drop") != null ? 1 : 0;
			conQuest += value(raw, "con_quest") != null ? 1 : 0;
			cutscene += value(raw, "cutsceneid1") != null ? 1 : 0;
			giveItem += value(raw, "give_item") != null ? 1 : 0;
			giveItem1 += value(raw, "give_item1") != null ? 1 : 0;
			removeItem2 += value(raw, "remove_item2") != null ? 1 : 0;
			rewardCheck += value(raw, "reward_check") != null ? 1 : 0;
		}
		for (int questId : tableRaw.keySet()) {
			Map<String, List<String>> quest = questXmlRaw.getOrDefault(questId, Map.of());
			collect1 += quest.containsKey("collect_item1") ? 1 : 0;
			collect2 += quest.containsKey("collect_item2") ? 1 : 0;
			collect3 += quest.containsKey("collect_item3") ? 1 : 0;
		}
		assertEquals(OBJECT1_ROWS, objectColumns[1]);
		assertEquals(OBJECT2_ROWS, objectColumns[2]);
		assertEquals(OBJECT3_ROWS, objectColumns[3]);
		assertEquals(OBJECT4_ROWS, objectColumns[4]);
		assertEquals(OBJECT_COLUMNS, objectColumns[1] + objectColumns[2] + objectColumns[3] + objectColumns[4]);
		assertEquals(TALK_NPC1_ROWS, talk1);
		assertEquals(TALK_NPC2_ROWS, talk2);
		assertEquals(TALK_NPC3_ROWS, talk3);
		assertEquals(PARTY_DROP_ROWS, partyDrop);
		assertEquals(CON_QUEST_ROWS, conQuest);
		assertEquals(CUTSCENE_ROWS, cutscene);
		assertEquals(GIVE_ITEM_ROWS, giveItem);
		assertEquals(GIVE_ITEM1_ROWS, giveItem1);
		assertEquals(REMOVE_ITEM2_ROWS, removeItem2);
		assertEquals(REWARD_CHECK_ROWS, rewardCheck);
		assertEquals(COLLECT_ITEM1_ROWS, collect1, "quest.xml collect_item1 行数");
		assertEquals(COLLECT_ITEM2_ROWS, collect2, "quest.xml collect_item2 行数");
		assertEquals(COLLECT_ITEM3_ROWS, collect3, "quest.xml collect_item3 行数");
	}

	/** quest.xml 单值列（缺列/空值返回 null）。 / A single-valued quest.xml column. */
	private static String questText(int questId, String tag) {
		List<String> values = questXmlRaw.getOrDefault(questId, Map.of()).get(tag);
		if (values == null || values.isEmpty() || values.getFirst().isBlank()) {
			return null;
		}
		return values.getFirst();
	}

	/** quest.xml 的 {@code prefixN} 列（按列序）。 / The numbered quest.xml columns in retail order. */
	private static List<String> questNumbered(int questId, String prefix) {
		Map<Integer, String> ordered = new TreeMap<>();
		for (Map.Entry<String, List<String>> entry
				: questXmlRaw.getOrDefault(questId, Map.of()).entrySet()) {
			Matcher matcher = Pattern.compile(prefix + "(\\d*)").matcher(entry.getKey());
			if (matcher.matches() && !entry.getValue().isEmpty() && !entry.getValue().getFirst().isBlank()) {
				ordered.put(matcher.group(1).isEmpty() ? 0 : Integer.parseInt(matcher.group(1)),
					entry.getValue().getFirst());
			}
		}
		return List.copyOf(ordered.values());
	}

	/** 交付列 → 槽号（1..N）。 / The slot of a hand-in symbol (1..N). */
	private static int slotOf(Map<Integer, String> collectSymbols, String symbol) {
		for (Map.Entry<Integer, String> entry : collectSymbols.entrySet()) {
			if (entry.getValue().equals(symbol)) {
				return entry.getKey();
			}
		}
		return 0;
	}

	/** 单元格的物品符号（尾随数字是数量）。 / The item symbol of a cell (a trailing number is the count). */
	private static String symbol(String cell) {
		String trimmed = cell.trim();
		int lastSpace = trimmed.lastIndexOf(' ');
		if (lastSpace < 0) {
			return trimmed;
		}
		String tail = trimmed.substring(lastSpace + 1);
		return tail.chars().allMatch(Character::isDigit) ? trimmed.substring(0, lastSpace) : trimmed;
	}

	/** 单元格的数量（缺省 1）。 / The cell count (1 when absent). */
	private static int symbolCount(String cell) {
		String trimmed = cell.trim();
		int lastSpace = trimmed.lastIndexOf(' ');
		if (lastSpace < 0) {
			return 1;
		}
		String tail = trimmed.substring(lastSpace + 1);
		return tail.chars().allMatch(Character::isDigit) ? Integer.parseInt(tail) : 1;
	}

	/** 按真端列序取 {@code prefixN}（缺号视为 0）。 / Numbered columns in retail order. */
	private static List<String> numbered(Map<String, String> raw, String prefix) {
		Map<Integer, String> ordered = new TreeMap<>();
		for (Map.Entry<String, String> entry : raw.entrySet()) {
			Matcher matcher = Pattern.compile(prefix + "(\\d*)").matcher(entry.getKey());
			if (matcher.matches() && !entry.getValue().isBlank()) {
				ordered.put(matcher.group(1).isEmpty() ? 0 : Integer.parseInt(matcher.group(1)),
					entry.getValue());
			}
		}
		return List.copyOf(ordered.values());
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

	private static String readResource(String path) throws Exception {
		try (InputStream input = SimpleCollectItemRowAlignmentGateTest.class.getClassLoader().getResourceAsStream(path)) {
			if (input == null) {
				throw new IllegalStateException("missing resource " + path);
			}
			return new String(input.readAllBytes(), StandardCharsets.UTF_8);
		}
	}
}
