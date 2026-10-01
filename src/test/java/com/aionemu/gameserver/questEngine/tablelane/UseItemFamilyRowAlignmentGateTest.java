package com.aionemu.gameserver.questEngine.tablelane;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.questEngine.definition.QuestDialogContract;
import com.aionemu.gameserver.questEngine.definition.QuestDialogPage;
import com.aionemu.gameserver.questEngine.definition.RetiredQuestIds;
import com.aionemu.gameserver.questEngine.retail.RetailClientHandinNpcSets;
import com.aionemu.gameserver.questEngine.retail.RetailItemNameIndex;
import com.aionemu.gameserver.questEngine.retail.RetailQuestDriver;
import com.aionemu.gameserver.questEngine.retail.RetailQuestMetadataCompiler;

/**
 * P5 两族（SimpleUseItem / SimpleItemPlay）的**逐行对齐门**（计划 §8.9）。
 * <p>
 * 以真端表与真端 {@code quest.xml} 的**独立重解析**为唯一事实（正则逐行，不复用
 * {@link NativeQuestTableLoader} 的 DOM 路径），逐行对拍 native 装载与路由映射；再按同一判据独立复算
 * 路由集，必须与处理器路由集逐元素相等。任何一行漂移即红灯。
 * <p>
 * Per-row alignment gate for the two P5 families: the retail tables and {@code quest.xml} are re-parsed
 * independently (regex, not the DOM loader) and compared row by row against the native mapping; the
 * routing verdict is recomputed here and must equal the handler's routing set element by element.
 */
class UseItemFamilyRowAlignmentGateTest {

	private static final String USE_ITEM_RESOURCE =
		"aion/data/static_data/quest/retail/Quest_SimpleUseItem.xml";
	private static final String ITEM_PLAY_RESOURCE =
		"aion/data/static_data/quest/retail/Quest_SimpleItemPlay.xml";
	private static final String QUEST_XML_RESOURCE = "aion/data/static_data/quest/retail/quest.xml";

	/** 真端表事实（全量复算）。 / Retail-table facts (whole-table recomputation). */
	private static final int USE_ITEM_ROWS = 160;
	private static final int USE_ITEM_TALK1 = 54;
	private static final int USE_ITEM_TALK2 = 26;
	private static final int USE_ITEM_TALK3 = 10;
	private static final int USE_ITEM_CON_QUEST = 32;
	private static final int USE_ITEM_CHECK = 5;
	private static final int USE_ITEM_GIVE1 = 8;
	private static final int USE_ITEM_GIVE2 = 4;
	private static final int USE_ITEM_GIVE3 = 3;
	private static final int USE_ITEM_REMOVE1 = 10;
	private static final int USE_ITEM_REMOVE2 = 6;
	private static final int USE_ITEM_REMOVE3 = 3;
	private static final int ITEM_PLAY_ROWS = 43;
	private static final int ITEM_PLAY_TALK1 = 11;
	private static final int ITEM_PLAY_TALK2 = 6;
	private static final int ITEM_PLAY_CON_QUEST = 9;
	private static final int ITEM_PLAY_GIVE = 34;
	private static final int ITEM_PLAY_USE_ITEM = 41;
	private static final int ITEM_PLAY_CUTSCENE = 2;
	/** 路由集冻结（与处理器一致；判据在本类内独立复算）。 / Frozen routing sets. */
	private static final int USE_ITEM_ROUTED = 102;
	private static final int ITEM_PLAY_ROUTED = 6;
	/** quest.xml {@code check_item} 声明行（退役面 11 行，其中 2 行为 fail-closed 残余）。 / check_item rows. */
	private static final Set<Integer> CHECK_ITEM_ROWS = Set.of(13812, 23812, 30720, 30723, 80482, 80486, 80554,
		80558, 80612, 80615, 80616);

	private static final Pattern ROW = Pattern.compile("<id id=\"(\\d+)\">(.*?)</id>", Pattern.DOTALL);
	private static final Pattern FIELD = Pattern.compile("<(\\w+)>(.*?)</\\1>", Pattern.DOTALL);
	private static final Pattern QUEST = Pattern.compile("<quest>(.*?)</quest>", Pattern.DOTALL);
	private static final Pattern ENTITY = Pattern.compile("<!ENTITY\\s+(\\w+)\\s+\"([^\"]*)\"\\s*>");

	private static Map<Integer, Map<String, String>> useItemRaw;
	private static Map<Integer, Map<String, String>> itemPlayRaw;
	private static Map<Integer, Map<String, List<String>>> questXmlRaw;

	@BeforeAll
	static void setUp() throws Exception {
		useItemRaw = parseTable(readResource(USE_ITEM_RESOURCE));
		itemPlayRaw = parseTable(readResource(ITEM_PLAY_RESOURCE));
		questXmlRaw = parseQuestXml(readResource(QUEST_XML_RESOURCE));
	}

	/** ① 行集与逐行字段：native 装载必须与真端表原文逐行一致（含长尾列）。 */
	@Test
	void everyUseItemRowIsMappedVerbatim() {
		assertEquals(USE_ITEM_ROWS, useItemRaw.size(), "真端表行数漂移");
		NativeQuestTableLoader loader = NativeQuestTableLoader.instance();
		assertEquals(USE_ITEM_ROWS, loader.useItemSize(), "装载器行数必须等于真端表行数");
		for (Map.Entry<Integer, Map<String, String>> entry : useItemRaw.entrySet()) {
			int questId = entry.getKey();
			Map<String, String> raw = entry.getValue();
			NativeQuestTableLoader.SimpleUseItemRow row = loader.requireUseItem(questId);
			assertEquals(value(raw, "dev_name"), row.devName(), "dev_name: " + questId);
			assertEquals(value(raw, "use_item_name"), row.useItemName(), "use_item_name: " + questId);
			assertEquals(value(raw, "reward_npc_name"), row.rewardNpcName(), "reward_npc_name: " + questId);
			assertEquals(declaredNames(raw, "talk_npc"), row.talkNpcNames(), "talk_npc1..3: " + questId);
			assertEquals(numberedSlots(raw, "give_item", 3), row.stepGiveItems(), "give_item1..3: " + questId);
			assertEquals(numberedSlots(raw, "remove_item", 3), row.stepRemoveItems(),
				"remove_item1..3: " + questId);
			assertEquals(integer(raw, "con_quest"), row.conQuest(), "con_quest: " + questId);
			assertEquals("1".equals(value(raw, "item_check")), row.itemCheck(), "item_check: " + questId);
			assertEquals(integer(raw, "cutsceneid1"), row.cutsceneId(), "cutsceneid1: " + questId);
			// 中继链必须无空位：装载器按表序紧凑装载，空位会让第 K 步物品错位。
			// The relay chain must be gap-free: the loader packs it in table order, so a hole would
			// misalign the step-scoped item columns.
			assertEquals(List.of(1, 2, 3).subList(0, row.talkNpcNames().size()),
				declaredSteps(raw, "talk_npc"), "talk 链不得有空位: " + questId);
		}
	}

	@Test
	void everyItemPlayRowIsMappedVerbatim() {
		assertEquals(ITEM_PLAY_ROWS, itemPlayRaw.size(), "真端表行数漂移");
		NativeQuestTableLoader loader = NativeQuestTableLoader.instance();
		assertEquals(ITEM_PLAY_ROWS, loader.itemPlaySize(), "装载器行数必须等于真端表行数");
		for (Map.Entry<Integer, Map<String, String>> entry : itemPlayRaw.entrySet()) {
			int questId = entry.getKey();
			Map<String, String> raw = entry.getValue();
			NativeQuestTableLoader.SimpleItemPlayRow row = loader.requireItemPlay(questId);
			assertEquals(value(raw, "dev_name"), row.devName(), "dev_name: " + questId);
			assertEquals(value(raw, "acquired_npc_name"), row.acquiredNpcName(), "acquired_npc_name: " + questId);
			assertEquals(value(raw, "reward_npc_name"), row.rewardNpcName(), "reward_npc_name: " + questId);
			assertEquals(value(raw, "use_item_name"), row.useItemName(), "use_item_name: " + questId);
			assertEquals(value(raw, "give_item"), row.acceptGiveItem(), "give_item: " + questId);
			assertEquals(declaredNames(raw, "talk_npc"), row.talkNpcNames(), "talk_npc1..2: " + questId);
			assertEquals(numberedSlots(raw, "give_item", 2), row.stepGiveItems(), "give_item1..2: " + questId);
			assertEquals(numberedSlots(raw, "remove_item", 2), row.stepRemoveItems(),
				"remove_item1..2: " + questId);
			assertEquals(integer(raw, "con_quest"), row.conQuest(), "con_quest: " + questId);
			assertEquals("1".equals(value(raw, "item_check")), row.itemCheck(), "item_check: " + questId);
			assertEquals(integer(raw, "cutsceneid1"), row.cutsceneId(), "cutsceneid1: " + questId);
			assertEquals(List.of(1, 2).subList(0, row.talkNpcNames().size()),
				declaredSteps(raw, "talk_npc"), "talk 链不得有空位: " + questId);
		}
	}

	/** ② 家族填充率冻结（任何一行增删列即红灯）。 / Frozen column coverage over both tables. */
	@Test
	void familyCountsAreFrozen() {
		assertEquals(USE_ITEM_TALK1, countDeclared(useItemRaw, "talk_npc", 1));
		assertEquals(USE_ITEM_TALK2, countDeclared(useItemRaw, "talk_npc", 2));
		assertEquals(USE_ITEM_TALK3, countDeclared(useItemRaw, "talk_npc", 3));
		assertEquals(USE_ITEM_CON_QUEST, countDeclared(useItemRaw, "con_quest", 0));
		assertEquals(USE_ITEM_CHECK, countDeclared(useItemRaw, "item_check", 0));
		assertEquals(USE_ITEM_GIVE1, countDeclared(useItemRaw, "give_item", 1));
		assertEquals(USE_ITEM_GIVE2, countDeclared(useItemRaw, "give_item", 2));
		assertEquals(USE_ITEM_GIVE3, countDeclared(useItemRaw, "give_item", 3));
		assertEquals(USE_ITEM_REMOVE1, countDeclared(useItemRaw, "remove_item", 1));
		assertEquals(USE_ITEM_REMOVE2, countDeclared(useItemRaw, "remove_item", 2));
		assertEquals(USE_ITEM_REMOVE3, countDeclared(useItemRaw, "remove_item", 3));
		assertEquals(ITEM_PLAY_TALK1, countDeclared(itemPlayRaw, "talk_npc", 1));
		assertEquals(ITEM_PLAY_TALK2, countDeclared(itemPlayRaw, "talk_npc", 2));
		assertEquals(ITEM_PLAY_CON_QUEST, countDeclared(itemPlayRaw, "con_quest", 0));
		assertEquals(ITEM_PLAY_GIVE, countDeclared(itemPlayRaw, "give_item", 0));
		assertEquals(ITEM_PLAY_USE_ITEM, countDeclared(itemPlayRaw, "use_item_name", 0));
		assertEquals(ITEM_PLAY_CUTSCENE, countDeclared(itemPlayRaw, "cutsceneid1", 0));
	}

	/** ③ 第 K 步发/扣列必须与第 K 个中继 NPC 同步声明（真端 codegen 槽语义前提）。 */
	@Test
	void stepScopedItemColumnsPairWithTheSameIndexRelayNpc() {
		for (Map<String, String> raw : useItemRaw.values()) {
			for (int step = 1; step <= 3; step++) {
				boolean declares = value(raw, "give_item" + step) != null || value(raw, "remove_item" + step) != null;
				if (declares) {
					int declaredStep = step;
					assertTrue(declaredSteps(raw, "talk_npc").contains(step),
						() -> "第 " + declaredStep + " 步发/扣必须有同号中继 NPC: " + raw.get("dev_name"));
				}
			}
		}
	}

	/**
	 * ④ 路由集独立复算：退役 ∧ 非 XML-only ∧ 接取/交付/物品/中继/门面可解 ∧ 元数据可编译，
	 * 必须与处理器路由集逐元素相等。
	 * The routing verdict recomputed from the retail data must equal the handler's routing set.
	 */
	@Test
	void routingVerdictMatchesTheHandlerElementByElement() throws Exception {
		RetailItemNameIndex items = RetailItemNameIndex.loadItemTemplates();
		NativeNpcNameResolver npcs = NativeNpcNameResolver.instance();
		RetailClientHandinNpcSets handin = RetailClientHandinNpcSets.defaultSets();
		Set<Integer> xmlOnly = NativeQuestOwnerResolver.instance().xmlOnlyIds();
		RetailQuestDriver driver = RetailQuestDriver.ensureLoaded();

		Set<Integer> expectedUseItem = new TreeSet<>();
		for (Map.Entry<Integer, Map<String, String>> entry : useItemRaw.entrySet()) {
			int questId = entry.getKey();
			Map<String, String> raw = entry.getValue();
			boolean resolvable = itemResolves(raw.get("use_item_name"), items)
				&& faceResolves(raw.get("reward_npc_name"), npcs, handin, questId)
				&& declaredSteps(raw, "talk_npc").stream()
					.allMatch(step -> unique(value(raw, "talk_npc" + step), npcs))
				&& declaredSteps(raw, "give_item").stream()
					.allMatch(step -> itemResolves(value(raw, "give_item" + step), items))
				&& declaredSteps(raw, "remove_item").stream()
					.allMatch(step -> itemResolves(value(raw, "remove_item" + step), items));
			if ("1".equals(value(raw, "item_check"))) {
				resolvable = resolvable && !checkItems(questId).isEmpty();
			}
			if (resolvable && RetiredQuestIds.contains(questId) && !xmlOnly.contains(questId)
					&& metadataClean(driver, questId)) {
				expectedUseItem.add(questId);
			}
		}
		assertEquals(USE_ITEM_ROUTED, expectedUseItem.size(), "SimpleUseItem 路由集规模冻结");
		assertEquals(expectedUseItem, SimpleUseItemHandler.instance().routedQuestIds(),
			"SimpleUseItem 路由集必须逐元素等于独立复算");

		Set<Integer> expectedItemPlay = new TreeSet<>();
		for (Map.Entry<Integer, Map<String, String>> entry : itemPlayRaw.entrySet()) {
			int questId = entry.getKey();
			Map<String, String> raw = entry.getValue();
			boolean longTail = !declaredSteps(raw, "talk_npc").isEmpty()
				|| !declaredSteps(raw, "give_item").isEmpty() || !declaredSteps(raw, "remove_item").isEmpty()
				|| value(raw, "cutsceneid1") != null || "1".equals(value(raw, "item_check"));
			boolean resolvable = !longTail
				&& unique(value(raw, "acquired_npc_name"), npcs)
				&& faceResolves(value(raw, "reward_npc_name"), npcs, handin, questId)
				&& itemResolves(value(raw, "use_item_name"), items)
				&& (value(raw, "give_item") == null || itemResolves(value(raw, "give_item"), items));
			if (resolvable && RetiredQuestIds.contains(questId) && !xmlOnly.contains(questId)
					&& metadataClean(driver, questId)) {
				expectedItemPlay.add(questId);
			}
		}
		assertEquals(ITEM_PLAY_ROUTED, expectedItemPlay.size(), "SimpleItemPlay 路由集规模冻结");
		assertEquals(expectedItemPlay, SimpleItemPlayHandler.instance().routedQuestIds(),
			"SimpleItemPlay 路由集必须逐元素等于独立复算");
	}

	/**
	 * ⑤ 门与页契约：{@code item_check} 行的门物品必须来自真端 quest.xml {@code check_itemK_L}，
	 * 且中继步页（SELECT2..4）必须是客户端任务页声明的可渲染页。
	 * The gate rows must read their items from the retail {@code quest.xml check_itemK_L} columns, and
	 * every relay step page must be declared by the client task-page contract.
	 */
	@Test
	void gateAndRelayPagesComeFromRetailDeclarations() throws Exception {
		RetailItemNameIndex items = RetailItemNameIndex.loadItemTemplates();
		QuestDialogContract contract = QuestDialogContract.loadDefault();
		SimpleUseItemHandler handler = SimpleUseItemHandler.instance();
		int[] relayPages = {QuestDialogPage.SELECT2.id(), QuestDialogPage.SELECT3.id(), QuestDialogPage.SELECT4.id()};

		for (Map.Entry<Integer, Map<String, String>> entry : useItemRaw.entrySet()) {
			int questId = entry.getKey();
			Map<String, String> raw = entry.getValue();
			boolean declared = !checkItems(questId).isEmpty();
			if (!declared) {
				assertTrue(handler.gateItems(questId).isEmpty(), "无 check_item 声明不得设门: " + questId);
				continue;
			}
			assertTrue(CHECK_ITEM_ROWS.contains(questId), "check_item 声明集必须冻结: " + questId);
			if ("1".equals(value(raw, "item_check"))) {
				assertEquals(checkItems(questId).stream().map(cell -> resolve(cell, items)).toList(),
					handler.gateItems(questId).stream().map(NativeItemSymbols.ItemStack::itemId).toList(),
					"门物品必须逐项等于 quest.xml check_itemK_L: " + questId);
			}
		}
		assertEquals(CHECK_ITEM_ROWS.stream().filter(RetiredQuestIds::contains).count(),
			CHECK_ITEM_ROWS.size(), "check_item 声明集必须全部为退役行");
		for (int questId : handler.routedQuestIds()) {
			List<Integer> relays = handler.relayNpcs(questId);
			for (int step = 1; step <= relays.size(); step++) {
				int relayStep = step;
				assertTrue(contract.hasButtonPage(questId, relayPages[relayStep - 1]),
					() -> "第 " + relayStep + " 中继步页必须由客户端任务页声明: " + questId);
			}
		}
	}

	// ---------------------------------------------------------------- 独立复算工具

	private static boolean metadataClean(RetailQuestDriver driver, int questId) {
		try {
			return driver.retailMetadataOf(questId).map(RetailQuestMetadataCompiler.Outcome::clean).orElse(false);
		} catch (RuntimeException e) {
			return false;
		}
	}

	private static boolean unique(String name, NativeNpcNameResolver npcs) {
		return name != null && !name.isBlank()
			&& npcs.resolve(name).resolution() == NativeNpcNameResolver.Resolution.UNIQUE;
	}

	private static boolean faceResolves(String name, NativeNpcNameResolver npcs,
			RetailClientHandinNpcSets handin, int questId) {
		return unique(name, npcs) || !handin.npcIds(questId).isEmpty();
	}

	/** 单元 = 「符号 [数量]」；两通道解析（先原名，未命中再去 {@code ITEM_} 前缀）。 / The two-channel rule. */
	private static boolean itemResolves(String cell, RetailItemNameIndex items) {
		return cell != null && !cell.isBlank() && resolve(cell, items) != null;
	}

	private static Integer resolve(String cell, RetailItemNameIndex items) {
		String symbol = cell.trim().split("\\s+")[0].toLowerCase(Locale.ROOT);
		Integer itemId = items.resolve(symbol);
		if (itemId == null && symbol.startsWith("item_")) {
			itemId = items.resolve(symbol.substring("item_".length()));
		}
		return itemId;
	}

	/** 该行真端 {@code check_itemK_L} 声明（按槽序）。 / The row's retail check_itemK_L cells. */
	private static List<String> checkItems(int questId) {
		Map<Integer, String> bySlot = new TreeMap<>();
		for (Map.Entry<String, List<String>> field
				: questXmlRaw.getOrDefault(questId, Map.of()).entrySet()) {
			Matcher matcher = Pattern.compile("check_item(\\d+)_(\\d+)").matcher(field.getKey());
			if (!matcher.matches() || field.getValue().isEmpty()
					|| field.getValue().getFirst().isBlank()) {
				continue;
			}
			bySlot.putIfAbsent(Integer.parseInt(matcher.group(1)), field.getValue().getFirst());
		}
		return List.copyOf(bySlot.values());
	}

	/** 已声明内容的列号（升序）。 / The declared column indices, ascending. */
	private static List<Integer> declaredSteps(Map<String, String> raw, String prefix) {
		List<Integer> steps = new ArrayList<>();
		for (int step = 1; step <= 4; step++) {
			if (value(raw, prefix + step) != null) {
				steps.add(step);
			}
		}
		return List.copyOf(steps);
	}

	private static int countDeclared(Map<Integer, Map<String, String>> rows, String prefix, int step) {
		int count = 0;
		for (Map<String, String> raw : rows.values()) {
			String key = step == 0 ? prefix : prefix + step;
			if (value(raw, key) != null) {
				count++;
			}
		}
		return count;
	}

	/**
	 * 按真端列序取 {@code prefixN}，**保留位置**（未声明的位为 null；与装载器的定长槽位同形状）。
	 * Numbered columns in retail order with fixed-length positions preserved (undeclared slots are
	 * null, matching the loader's positional shape).
	 */
	private static List<String> numberedSlots(Map<String, String> raw, String prefix, int slots) {
		List<String> out = new ArrayList<>(java.util.Collections.nCopies(slots, null));
		for (Map.Entry<String, String> entry : raw.entrySet()) {
			Matcher matcher = Pattern.compile(prefix + "(\\d+)").matcher(entry.getKey());
			if (matcher.matches()) {
				int index = Integer.parseInt(matcher.group(1));
				if (index >= 1 && index <= slots) {
					out.set(index - 1, entry.getValue().isBlank() ? null : entry.getValue());
				}
			}
		}
		// 位置形允许 null（未声明槽），故用 unmodifiableList 而非 List.copyOf。
		// The positional shape allows nulls, so unmodifiableList rather than List.copyOf.
		return java.util.Collections.unmodifiableList(out);
	}

	/** 紧凑声明序（装载器的中继链形状：只保留已声明的名字，按列号升序）。 / The loader's dense relay shape. */
	private static List<String> declaredNames(Map<String, String> raw, String prefix) {
		TreeMap<Integer, String> ordered = new TreeMap<>();
		for (Map.Entry<String, String> entry : raw.entrySet()) {
			Matcher matcher = Pattern.compile(prefix + "(\\d+)").matcher(entry.getKey());
			if (matcher.matches() && !entry.getValue().isBlank()) {
				ordered.put(Integer.parseInt(matcher.group(1)), entry.getValue());
			}
		}
		return List.copyOf(ordered.values());
	}

	private static Integer integer(Map<String, String> raw, String key) {
		String text = value(raw, key);
		return text == null ? null : Integer.parseInt(text);
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
		try (InputStream input = UseItemFamilyRowAlignmentGateTest.class.getClassLoader().getResourceAsStream(path)) {
			if (input == null) {
				throw new IllegalStateException("missing resource " + path);
			}
			return new String(input.readAllBytes(), StandardCharsets.UTF_8);
		}
	}
}
