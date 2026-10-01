package com.aionemu.gameserver.questEngine.tablelane;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.questEngine.definition.QuestDialogContract;
import com.aionemu.gameserver.questEngine.definition.QuestDialogPage;
import com.aionemu.gameserver.questEngine.definition.RetiredQuestIds;
import com.aionemu.gameserver.questEngine.retail.RetailItemNameIndex;
import com.aionemu.gameserver.questEngine.retail.RetailQuestDriver;
import com.aionemu.gameserver.questEngine.retail.RetailQuestMetadataCompiler;
import com.aionemu.gameserver.questEngine.retail.RetailRecipeIndex;

/**
 * P6 CombineTask 的**逐行对齐门**（计划 §8.9）。
 * <p>
 * 以真端 {@code Quest_CombineTask.xml} 与真端 {@code quest.xml} 的**独立重解析**（正则，不走
 * {@link NativeQuestTableLoader} 的 DOM 路径）为唯一事实：① 逐行字段与装载器逐元素对拍；② 家族列填充率
 * 冻结；③ 路由判据（退役 ∧ 非 XML-only ∧ 双 NPC 名唯一 ∧ 技能符号可解 ∧ 产物/分量可解 ∧
 * {@code (skill, product)} 配方唯一 ∧ 元数据干净）在本类内独立复算，必须与处理器路由集逐元素相等；
 * ④ 接取入口页必须来自客户端任务页契约（本族 574 行未登记 ⇒ 回落真端接取窗页 4）。
 * <p>
 * Per-row alignment gate for the P6 CombineTask family: the retail tables are re-parsed independently
 * (regex) and compared against the native loader row by row; the routing verdict is recomputed here and
 * must equal the handler's routing set element by element; accept entry pages must come from the client
 * task-page contract.
 */
class CombineTaskRowAlignmentGateTest {

	private static final String COMBINE_TABLE = "aion/data/static_data/quest/retail/Quest_CombineTask.xml";
	private static final String RECIPE_TEMPLATES = "/aion/data/static_data/recipe/recipe_templates.xml";

	/** 真端表事实（全量复算）。 / Retail-table facts (whole-table recomputation). */
	private static final int ROWS = 574;
	private static final int TASK_NPCS = 574;
	private static final int COMPONENT_SLOTS = 574;
	private static final int SECOND_COMPONENT = 152;
	private static final int ROUTED = 574;
	/** 真端 helper 允许的分量槽位数（{@code lVar4 = 8}）。 / The retail component slot count. */
	private static final int RETAIL_COMPONENT_SLOTS = 8;

	private static final Pattern ROW = Pattern.compile("<id id=\"(\\d+)\">(.*?)</id>", Pattern.DOTALL);
	private static final Pattern FIELD = Pattern.compile("<(\\w+)>(.*?)</\\1>", Pattern.DOTALL);
	private static final Pattern ENTITY = Pattern.compile("<!ENTITY\\s+(\\w+)\\s+\"([^\"]*)\"\\s*>");

	private static Map<Integer, Map<String, String>> combineRaw;

	@BeforeAll
	static void setUp() throws Exception {
		combineRaw = parseTable(readResource(COMBINE_TABLE));
	}

	/** ① 行集与逐行字段：native 装载必须与真端表原文逐行一致（含位置保留的分量槽）。 */
	@Test
	void everyCombineRowIsMappedVerbatim() {
		assertEquals(ROWS, combineRaw.size(), "真端表行数漂移");
		NativeQuestTableLoader loader = NativeQuestTableLoader.instance();
		assertEquals(ROWS, loader.combineSize(), "装载器行数必须等于真端表行数");
		for (Map.Entry<Integer, Map<String, String>> entry : combineRaw.entrySet()) {
			int questId = entry.getKey();
			Map<String, String> raw = entry.getValue();
			NativeQuestTableLoader.CombineTaskRow row = loader.requireCombine(questId);
			assertEquals(value(raw, "dev_name"), row.devName(), "dev_name: " + questId);
			assertEquals(declaredNames(raw, "task_npc"), row.taskNpcNames(), "task_npc: " + questId);
			assertEquals(value(raw, "combineskill"), row.combineSkill(), "combineskill: " + questId);
			assertEquals(integer(raw, "combine_skillpoint", 0), row.skillPoint(),
				"combine_skillpoint（缺省 0）: " + questId);
			assertEquals(value(raw, "recipe_name"), row.recipeName(), "recipe_name: " + questId);
			assertEquals(value(raw, "product"), row.product(), "product: " + questId);
			assertEquals(numberedSlots(raw, "give_component", RETAIL_COMPONENT_SLOTS), row.components(),
				"give_component1..8（位置保留）: " + questId);
		}
	}

	/** ② 家族填充率冻结（任何一行增删列即红灯）。 / Frozen column coverage. */
	@Test
	void combineFamilyCountsAreFrozen() {
		assertEquals(TASK_NPCS, countDeclared(combineRaw, "task_npc", 0), "task_npc 覆盖");
		assertEquals(COMPONENT_SLOTS, countDeclared(combineRaw, "give_component", 1), "give_component1 覆盖");
		assertEquals(SECOND_COMPONENT, countDeclared(combineRaw, "give_component", 2), "give_component2 覆盖");
		for (int slot = 3; slot <= RETAIL_COMPONENT_SLOTS; slot++) {
			assertEquals(0, countDeclared(combineRaw, "give_component", slot),
				"give_component" + slot + " 必须恒空（真端数据只有 1/2 两槽）");
		}
		assertEquals(ROWS, combineRaw.values().stream().filter(raw -> value(raw, "product") != null).count(),
			"product 覆盖 574 行");
		assertEquals(ROWS, combineRaw.values().stream().filter(raw -> value(raw, "recipe_name") != null).count(),
			"recipe_name 覆盖 574 行");
	}

	/** ③ 路由判据独立复算 ⇒ 必须与处理器路由集逐元素相等。 / Independently recomputed routing verdict. */
	@Test
	void routingVerdictMatchesTheHandlerElementByElement() throws Exception {
		RetailItemNameIndex items = RetailItemNameIndex.loadItemTemplates();
		NativeNpcNameResolver npcs = NativeNpcNameResolver.instance();
		Set<Integer> xmlOnly = NativeQuestOwnerResolver.instance().xmlOnlyIds();
		RetailQuestDriver driver = RetailQuestDriver.ensureLoaded();
		RetailRecipeIndex recipes;
		try (InputStream input = CombineTaskRowAlignmentGateTest.class.getResourceAsStream(RECIPE_TEMPLATES)) {
			assertTrue(input != null, "missing " + RECIPE_TEMPLATES);
			recipes = RetailRecipeIndex.build(List.of(input));
		}

		Set<Integer> expected = new TreeSet<>();
		for (Map.Entry<Integer, Map<String, String>> entry : combineRaw.entrySet()) {
			int questId = entry.getKey();
			Map<String, String> raw = entry.getValue();
			boolean npcsOk = declaredNames(raw, "task_npc").stream()
				.allMatch(name -> unique(name, npcs));
			Integer skillId = RetailQuestMetadataCompiler.combineSkillId(value(raw, "combineskill"));
			Integer productId = resolve(value(raw, "product"), items);
			boolean productOk = productId != null;
			boolean componentsOk = !declaredSlots(raw, "give_component").isEmpty()
				&& declaredSlots(raw, "give_component").stream()
					.allMatch(slot -> resolve(value(raw, "give_component" + slot), items) != null);
			boolean recipeOk = skillId != null && productOk
				&& recipes.resolveUnique(skillId, productId).isPresent();
			if (npcsOk && recipeOk && componentsOk && RetiredQuestIds.contains(questId)
					&& !xmlOnly.contains(questId) && metadataClean(driver, questId)) {
				expected.add(questId);
			}
		}
		assertEquals(ROUTED, expected.size(), "CombineTask 路由集规模冻结");
		assertEquals(expected, SimpleCombineTaskHandler.instance().routedQuestIds(),
			"CombineTask 路由集必须逐元素等于独立复算（ADDED 0 / REMOVED 0）");
		assertTrue(SimpleCombineTaskHandler.instance().unroutableQuestIds().isEmpty(),
			"本族零 fail-closed 残余");
	}

	/**
	 * ④ 接取入口页契约：每行的入口页必须等于客户端任务页声明的第一页；本族 574 行无页登记 ⇒ 回落
	 * 真端接取窗页 4（不得发明新页）。
	 * Accept entry pages must come from the client task-page contract; with no registration in this
	 * family the contract falls back to the retail ask window page 4.
	 */
	@Test
	void acceptEntryPagesComeFromTheClientTaskPageContract() {
		QuestDialogContract contract = QuestDialogContract.loadDefault();
		SimpleCombineTaskHandler handler = SimpleCombineTaskHandler.instance();
		int askWindow = QuestDialogPage.SHOW_ASK_QUEST_ACCEPT_WINDOW.id();
		int registered = 0;
		for (int questId : handler.routedQuestIds()) {
			if (contract.hasButtonPage(questId, askWindow)) {
				registered++;
			}
			assertEquals(contract.acceptEntryPage(questId), askWindow,
				"本族行的入口页 = 真端接取窗页 4（客户端页登记缺失即回落）: " + questId);
		}
		assertEquals(0, registered, "本族 574 行客户端页索引完全无登记（P6 步骤 1 冻结事实）");
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

	/** 单元 = 「符号 [数量]」；两通道解析（先原名，未命中再去 {@code ITEM_} 前缀）。 / The two-channel rule. */
	private static Integer resolve(String cell, RetailItemNameIndex items) {
		if (cell == null || cell.isBlank()) {
			return null;
		}
		String symbol = cell.trim().split("\\s+")[0].toLowerCase(Locale.ROOT);
		Integer itemId = items.resolve(symbol);
		if (itemId == null && symbol.startsWith("item_")) {
			itemId = items.resolve(symbol.substring("item_".length()));
		}
		return itemId;
	}

	/** 真端 {@code task_npc}（逗号分隔，按列序去空）。 / The comma-separated retail npc list. */
	private static List<String> declaredNames(Map<String, String> raw, String key) {
		String text = value(raw, key);
		if (text == null) {
			return List.of();
		}
		List<String> names = new ArrayList<>();
		for (String part : text.split(",")) {
			String name = part.trim();
			// 表内换行/制表符归一后再比较（真端原文用空白缩进）。 / Normalize the retail whitespace.
			name = name.replaceAll("\\s+", " ");
			if (!name.isBlank()) {
				names.add(name);
			}
		}
		return List.copyOf(names);
	}

	/** 已声明内容的槽号（升序）。 / The declared slot indices, ascending. */
	private static List<Integer> declaredSlots(Map<String, String> raw, String prefix) {
		List<Integer> slots = new ArrayList<>();
		for (int slot = 1; slot <= RETAIL_COMPONENT_SLOTS; slot++) {
			if (value(raw, prefix + slot) != null) {
				slots.add(slot);
			}
		}
		return List.copyOf(slots);
	}

	private static int countDeclared(Map<Integer, Map<String, String>> rows, String prefix, int index) {
		int count = 0;
		for (Map<String, String> raw : rows.values()) {
			if (value(raw, index == 0 ? prefix : prefix + index) != null) {
				count++;
			}
		}
		return count;
	}

	/**
	 * 按真端列序取 {@code prefixN}，**保留位置**（未声明的位为 null；与装载器的定长槽位同形状）。
	 * Numbered columns with fixed-length positions preserved (undeclared slots are null).
	 */
	private static List<String> numberedSlots(Map<String, String> raw, String prefix, int slots) {
		List<String> out = new ArrayList<>(Collections.nCopies(slots, null));
		for (Map.Entry<String, String> entry : raw.entrySet()) {
			Matcher matcher = Pattern.compile(prefix + "(\\d+)").matcher(entry.getKey());
			if (matcher.matches()) {
				int index = Integer.parseInt(matcher.group(1));
				if (index >= 1 && index <= slots) {
					out.set(index - 1, entry.getValue().isBlank() ? null : entry.getValue());
				}
			}
		}
		return Collections.unmodifiableList(out);
	}

	private static Integer integer(Map<String, String> raw, String key, int defaultValue) {
		String text = value(raw, key);
		return text == null ? defaultValue : Integer.parseInt(text);
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
		try (InputStream input = CombineTaskRowAlignmentGateTest.class.getClassLoader().getResourceAsStream(path)) {
			if (input == null) {
				throw new IllegalStateException("missing resource " + path);
			}
			return new String(input.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

}
