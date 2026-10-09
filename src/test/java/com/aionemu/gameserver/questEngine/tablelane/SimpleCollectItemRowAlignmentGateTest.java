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

import com.aionemu.gameserver.questEngine.definition.QuestDialogPage;

/**
 * SimpleCollectItem 族级**逐行对齐门**（P4 收口，计划 §8.9）：以原版表 {@code Quest_SimpleCollectItem.xml}
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

	/** 原版表事实（全量复算）。 / Retail-table facts (whole-table recomputation). */
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
	/** 原版 minlevel=999 的休眠行（元数据 min>max，不可路由）。 / Dormant retail rows. */
	private static final Set<Integer> DORMANT_LEVEL_ROWS = Set.of(36017, 46017, 47112);
	/** 表格声明了对象但原版不给槽位的行（41216 的来源是另一个 FOBJ）。 / Object without a retail slot. */
	private static final Set<Integer> OBJECT_WITHOUT_SLOT_ROWS = Set.of(41216);
	/** 无采集计数的行（TEST 5 + 事件行 4）：不可路由。 / Rows without a collect count. */
	private static final Set<Integer> NO_COLLECT_COUNT_ROWS =
		Set.of(9649, 9650, 9654, 9655, 9656, 50017, 50018, 51017, 51018);
	/** XML-only 行（注册集里让位给 XML 车道）。 / XML-owned rows that yield to the XML lane. */
	private static final Set<Integer> XML_ONLY_ROWS = Set.of(2237);
	/**
	 * 复合势力交付名的行（39611/49611：{@code LDF5b_Silverlin_LD}）。组表
	 * （{@code retail-quest-ai-name-groups.xml}）已声明该组名（成员 {@code LDF5b_*_Silverlin}）⇒
	 * 交付面经组通道解析为要塞守护组（800927-9），可路由；两行原版 minlevel=999，由接取轴自然拒绝。
	 * （2026-10-04 对齐：原判"无客户端集 ⇒ fail-closed 不路由"随组表扩批已过时。）
	 * Composite faction reward names (rows 39611/49611): the dialog-name group table declares
	 * {@code LDF5b_Silverlin_LD} (members {@code LDF5b_*_Silverlin}), so the hand-in face resolves to the
	 * fortress-guard group (800927-9) and the row routes; minlevel=999 is refused by the accept axis.
	 */
	private static final Set<Integer> COMPOSITE_REWARD_GROUP_ROWS = Set.of(39611, 49611);

	/** 其他 retail 族表（跨族 {@code con_quest} 目标的接取 NPC 来源）。 / Sibling retail family tables. */
	private static final List<String> SIBLING_RESOURCES = List.of(
		"aion/data/static_data/quest/retail/Quest_SimpleHunt.xml",
		"aion/data/static_data/quest/retail/Quest_SimpleTalk.xml",
		"aion/data/static_data/quest/retail/Quest_CombineTask.xml",
		"aion/data/static_data/quest/retail/Quest_SimpleUseItem.xml",
		"aion/data/static_data/quest/retail/Quest_SimpleItemPlay.xml",
		"aion/data/static_data/quest/retail/Quest_SimpleSerialHunt.xml");
	/** 链式接取窗目标的分布（原版全表冻结）。 / Distribution of the chain-window targets (frozen). */
	private static final int CHAIN_TARGET_IN_TABLE = 6;
	private static final int CHAIN_TARGET_SIBLING = 24;
	private static final int CHAIN_TARGET_NO_ROW = 7;
	private static final int CHAIN_TARGET_NO_ROW_XML_OWNED = 3;
	/** 原版声明的过场（交付节点 0x35 槽）：两行同为 movie 456 / 动作 SELECT1_1(1012)。 /
	 * The declared cutscenes (hand-in node slot 0x35): both rows are movie 456 / action SELECT1_1(1012). */
	private static final int CUTSCENE_MOVIE = 456;
	private static final int CUTSCENE_ACTION = QuestDialogPage.SELECT1_1.id();
	private static final Set<Integer> CUTSCENE_QUEST_IDS = Set.of(18501, 28501);

	private static final Pattern ROW = Pattern.compile("<id id=\"(\\d+)\">(.*?)</id>", Pattern.DOTALL);
	private static final Pattern FIELD = Pattern.compile("<(\\w+)>(.*?)</\\1>", Pattern.DOTALL);
	private static final Pattern QUEST = Pattern.compile("<quest>(.*?)</quest>", Pattern.DOTALL);

	private static Map<Integer, Map<String, String>> tableRaw;
	private static Map<Integer, Map<String, List<String>>> questXmlRaw;
	private static SimpleCollectItemHandler handler;

	@BeforeAll
	static void setUp() throws Exception {
		tableRaw = parseTable(readResource(COLLECT_RESOURCE));
		questXmlRaw = parseQuestXml(readResource(QUEST_XML_RESOURCE));
		handler = SimpleCollectItemHandler.instance();
	}

	/** ① 行集与逐行字段：native 映射必须与原版表原文逐行一致（含长尾列）。 */
	@Test
	void everyRetailRowIsMappedVerbatim() {
		assertEquals(ROWS, tableRaw.size(), "原版表行数漂移 / retail row count drifted");
		NativeQuestTableLoader loader = NativeQuestTableLoader.instance();
		assertEquals(tableRaw.keySet(), handler.ownedQuestIds(),
			"注册集必须逐行等于原版表 id 集 / registration set must equal the retail id set");
		assertEquals(tableRaw.size(), loader.collectSize(), "装载器行数必须等于原版表行数");

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
			"本族 XML-only 行（原版表 ∩ XML 保留）必须冻结");
		for (int questId : handler.ownedQuestIds()) {
			boolean expectRouted = !xmlOnly.contains(questId)
				&& !DORMANT_LEVEL_ROWS.contains(questId)
				&& !NO_COLLECT_COUNT_ROWS.contains(questId)
				&& !OBJECT_WITHOUT_SLOT_ROWS.contains(questId);
			assertEquals(expectRouted, handler.routes(questId),
				"路由/注册不一致 / routing differs from the derived verdict: " + questId);
		}
		assertEquals(ROWS - XML_ONLY_ROWS.size() - DORMANT_LEVEL_ROWS.size()
			- NO_COLLECT_COUNT_ROWS.size() - OBJECT_WITHOUT_SLOT_ROWS.size(),
			handler.routedQuestIds().size(), "路由集大小必须冻结");
		for (int questId : COMPOSITE_REWARD_GROUP_ROWS) {
			assertEquals(java.util.List.of(800927, 800928, 800929), handler.rewardNpcs(questId),
				() -> "复合交付名必须经组表解析为要塞守护组（不得凭空造交付面）: " + questId);
			assertTrue(handler.ownedQuestIds().contains(questId),
				() -> "复合行仍须装载（注册集逐行等于原版表）: " + questId);
		}
		assertTrue(java.util.Collections.disjoint(handler.routedQuestIds(), xmlOnly),
			"路由集与 XML-only 集不得交叠 / the routing set must not intersect the XML-owned set");
	}

	/**
	 * ③ 采集族不得派生相机行：原版相机调用（camera-params.tsv，2463 点）中本族 262 行 0 命中
	 * （对照 SimpleHunt 1812/1863），旧 XML 的交互/击杀转换亦零 var 写——采集为物品驱动，
	 * P4 的"相机 required = collect_item 计数"派生已撤销（2026-10-04）。
	 * No collect row may derive a camera row: the family is item-driven.
	 */
	@Test
	void collectRowsDeriveNoCameraRows() {
		CameraRegistry registry = CameraRegistry.instance();
		for (int questId : tableRaw.keySet()) {
			assertTrue(registry.find(questId).isEmpty(),
				"采集族不得派生相机行（原版 262/262 无相机调用）: " + questId);
		}
	}

	/**
	 * ④ 槽对齐（P4 原版复核）：原版 {@code drop_monster_K} 列出的来源产出 {@code drop_item_K}，
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
				// 原版一致性：掉落物必须是交付列里的一项，槽 = 该项的位置。
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
					assertEquals(1, ids.size(), () -> "原版来源名必须唯一解析: " + source);
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
		// 原版冻结（限本族可路由行）：K 列与 objectK 同名的列 275 个、仅大小写差异 8 个、
		// 一个掉落列列多个来源 3 个、掉落源是真怪（对象只是线索）3 个 —— 正因如此，槽不能按对象列序取。
		// 39611/49611 两行（各贡献一个同名对齐列）自组表解析生效起已路由，计入 275。
		// Frozen over the routed rows: the object column order is not the slot order (275 aligned, 8
		// case-only, 3 multi-source, 3 real-monster drop columns). The two composite rows 39611/49611
		// (one aligned column each) now route and count toward the 275.
		assertEquals(275, alignedColumns, "objectK 与 drop_monster_K 同名的列数必须冻结（路由集口径）");
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

	/**
	 * ⑥ 链式接取窗（原版 0x1e 槽）：{@code con_quest} 的下一环必须在本行的交付 NPC 上可接取
	 * （本车道接取路由按 NPC 建表，该等价物即窗口本身）；跨族目标按同一不变量独立复算。
	 * <p>
	 * The chain window (retail slot 0x1e): the next quest must acquire at this row's hand-in NPC;
	 * cross-family targets are recomputed under the same invariant.
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
			if (!Objects.equals(rewardName, targetAcquire)) {
				mismatches.add(questId + "->" + next + " reward=" + rewardName
					+ " targetAcquire=" + targetAcquire);
			}
		}
		assertTrue(mismatches.isEmpty(), () -> "链式接取窗未在本行交付 NPC 上闭环: " + mismatches);
		assertEquals(CHAIN_TARGET_IN_TABLE, inTable, "本表内目标数漂移");
		assertEquals(CHAIN_TARGET_SIBLING, inSibling, "跨族目标数漂移");
		assertEquals(CHAIN_TARGET_NO_ROW, noRow, "无表行目标数漂移");
		assertEquals(CHAIN_TARGET_NO_ROW_XML_OWNED, noRowXmlOwned, "无表行目标里的 XML-owner 数漂移");
		assertTrue(handler.unresolvedChainQuestIds().isEmpty(),
			() -> "本族链式接取窗未闭环: " + handler.unresolvedChainQuestIds());
	}

	/**
	 * ⑦ 过场装载（原版交付节点 0x35 槽）：{@code cutsceneid1}/{@code cs1_haction} 逐行进 native 消费面；
	 * 未声明的行不得有过场面（原版 thunk 不内联 movie，本车道以表列为唯一事实）。
	 * <p>
	 * The cutscene face (retail slot 0x35): declared rows map into the native consumption face and rows
	 * without the column expose none.
	 */
	@Test
	void cutsceneFaceFollowsTheRetailTable() {
		int declared = 0;
		Set<Integer> declaredQuestIds = new TreeSet<>();
		for (Map.Entry<Integer, Map<String, String>> entry : tableRaw.entrySet()) {
			int questId = entry.getKey();
			Integer movie = integer(entry.getValue(), "cutsceneid1");
			SimpleCollectItemHandler.Cutscene cutscene = handler.cutscene(questId);
			if (movie == null) {
				assertNull(cutscene, "未声明过场的行不得有过场面: " + questId);
				continue;
			}
			declared++;
			declaredQuestIds.add(questId);
			Integer action = integer(entry.getValue(), "cs1_haction");
			assertEquals(movie.intValue(), cutscene.movieId(), "cutsceneid1: " + questId);
			assertEquals(action == null ? -1 : action.intValue(), cutscene.triggerAction(),
				"cs1_haction: " + questId);
		}
		assertEquals(CUTSCENE_ROWS, declared, "过场行数漂移");
		assertEquals(CUTSCENE_QUEST_IDS, declaredQuestIds, "过场行集漂移");
		for (int questId : CUTSCENE_QUEST_IDS) {
			assertEquals(CUTSCENE_MOVIE, handler.cutscene(questId).movieId(), "过场 movie 漂移: " + questId);
			assertEquals(CUTSCENE_ACTION, handler.cutscene(questId).triggerAction(),
				"过场触发动作漂移: " + questId);
		}
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

	/** 按原版列序取 {@code prefixN}（缺号视为 0）。 / Numbered columns in retail order. */
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
		Map<String, String> entities = entities();
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
		Map<String, String> entities = entities();
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

	/**
	 * XML 预定义实体展开表（副本已无 DOCTYPE，2026-10-03 剥离批；替换顺序 = 解析器单层展开语义，
	 * 如 {@code &amp;quot;} 只展开一层）。
	 * The five XML predefined entities (the repo copy carries no DOCTYPE since the 2026-10-03 strip
	 * batch; replacement order reproduces single-level parser expansion, e.g. {@code &amp;quot;}).
	 */
	private static Map<String, String> entities() {
		Map<String, String> entities = new LinkedHashMap<>();
		entities.put("quot", "\"");
		entities.put("amp", "&");
		entities.put("apos", "'");
		entities.put("lt", "<");
		entities.put("gt", ">");
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

	/** 数值单元格（缺列/空值返回 null）。 / A numeric cell (null when absent or blank). */
	private static Integer integer(Map<String, String> raw, String key) {
		String text = value(raw, key);
		return text == null ? null : Integer.valueOf(text);
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
