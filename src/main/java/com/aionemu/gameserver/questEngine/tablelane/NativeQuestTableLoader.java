package com.aionemu.gameserver.questEngine.tablelane;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * 真端任务表装载器（计划 §6.2：家族逐个接入；本横切只装 {@code Quest_SimpleHunt.xml}）。
 * <p>
 * 数据源 = P0b 入仓副本的精简形（UTF-8、DOCTYPE/实体子集已移除，2026-10-03 剥离批；
 * 七张表各有同目录同名 .xsd）。行模型：{@code <id id="N">} + 接取/交付 NPC 名 +
 * {@code countN/monsterN}（槽 1..5）。真端数据事实（P1 对拍实证）：元素文本可跨行（37116 的
 * monster 名单含换行，必须归一内部空白）；存在零计数行（11013/11014/11208/11209，真端休眠行，
 * 合法装载但不得派生相机行）；存在有计数无 monster 的行（13912/23912 的槽 3——真端脚本也未给
 * 该槽事件源，行为 = 永不满足，本类如实装载不修复）；注释掉的行由 DOM 天然忽略。
 * <p>
 * 相机行推导（纯函数 {@link #cameraSpec}）：宽度规则 = 任一 count 超 6 位掩码 ⇒ 10 位，否则
 * 6 位（对拍 1812 任务零失败）；fullValue = 各槽 count 按位移或（对拍与脚本字面 fullValue 全等，
 * 含 13912/23912）。哪些行注册进 {@link CameraRegistry} 由切换批按脚本接线集决定（51 行真端
 * 休眠：表有行、脚本无包装函数 ⇒ 原生侧同样不接 handler）。
 * <p>
 * The retail quest-table loader (plan §6.2: families plug in one by one; this tranche loads
 * {@code Quest_SimpleHunt.xml} only). Source = the simplified P0b copy (UTF-8, DOCTYPE/entity subset
 * removed in the 2026-10-03 strip batch; every table has a same-stem sibling .xsd).
 * Row model: {@code <id id="N">} + acquire/reward NPC names + {@code countN/monsterN}
 * (slots 1..5). Retail data facts (P1 reconciliation evidence): element text can span lines
 * (37116's monster list contains newlines — inner whitespace must be normalized); zero-count rows
 * exist (11013/11014/11208/11209 — dormant retail rows that load legally but must never derive a
 * camera row); count-without-monster rows exist (slot 3 of 13912/23912 — retail's own script never
 * wires an event source for that slot either, so behavior is "never satisfied"; loaded as-is, never
 * fixed); commented-out rows are ignored by the DOM naturally.
 * <p>
 * Camera-row derivation (pure function {@link #cameraSpec}): width rule = any count above the 6-bit
 * mask implies 10-bit, otherwise 6-bit (reconciled with zero failures across 1812 quests);
 * fullValue = bit-OR of per-slot counts (equal to the script's literal fullValue on every quest,
 * including 13912/23912). Which rows register into {@link CameraRegistry} is the switch batch's
 * decision per the script-wired set (51 retail-dormant rows: table row present, no script wrapper
 * ⇒ the native lane wires no handler either).
 */
public final class NativeQuestTableLoader {

	/** 单槽狩猎目标：required 计数 + 怪物名（真端 13912 形允许空名单）。 / One hunt slot: required count + monster names (the retail 13912 shape allows an empty list). */
	public record KillSlot(int count, List<String> monsters) {
	}

	/**
	 * SimpleHunt 表行。{@code con_quest}（交付节点 0x1e 槽的链式接取窗，132 行）与
	 * {@code cutsceneid1}/{@code cs1_haction}（交付节点 0x35 槽的过场，3 行）按原文装载。
	 * <p>
	 * One SimpleHunt table row; the chain-window column (slot 0x1e, 132 rows) and the cutscene columns
	 * (slot 0x35, 3 rows) load verbatim.
	 */
	public record SimpleHuntRow(int questId, String acquiredNpcName, String rewardNpcName,
			Map<Integer, KillSlot> killSlots, Integer conQuest, Integer cutsceneId, Integer cutsceneAction) {
	}

	/** 串行阶段规约：阶段号 (1..5) + 所需击杀数 + 目标怪名列表。 / Serial stage: 1-based stage + required count + monster names. */
	public record SerialStage(int stage, int count, List<String> monsters) {
	}

	/** SimpleSerialHunt 表行。 / One SimpleSerialHunt table row. */
	public record SimpleSerialHuntRow(int questId, String devName, String acquiredNpcName,
			String rewardNpcName, List<String> talkNpcNames, List<SerialStage> stages) {
	}

	/**
	 * SimpleTalk 表行（真端 {@code quest_simpletalks}，3152 行）。形状实测：双 NPC 结构 100%、
	 * {@code talk_npc1..3} 中继链 15%、{@code item_check} 63%（值恒为 1，语义 = 交付前须持有工作物品）、
	 * {@code con_quest} 16%（链式接取窗的下一条）、give/remove 物品与 cutscene 为长尾列。
	 * <p>
	 * One SimpleTalk table row (retail {@code quest_simpletalks}, 3152 rows). Measured shape: the
	 * two-NPC structure is total, {@code talk_npc1..3} relay chains cover ~15%, {@code item_check}
	 * 63% (value is always 1 = the work item must be carried before the hand-in), {@code con_quest}
	 * 16% (the next quest of the chain-acquire window); give/remove items and cutscenes are the
	 * long tail. Values are kept as the table's own text so no semantics are invented here.
	 *
	 * @param acceptGiveItem  {@code give_item} 原文（形如 {@code NAME COUNT}；接取侧 20000 动作的发放）
	 * @param stepGiveItems   {@code give_item1..3} 原文，**按下标对齐中继步**（null = 该步无发放）
	 * @param stepRemoveItems {@code remove_item1..3} 原文，**按下标对齐中继步**（null = 该步无扣除）
	 */
	public record SimpleTalkRow(int questId, String devName, String acquiredNpcName, String rewardNpcName,
			List<String> talkNpcNames, Integer conQuest, boolean itemCheck, String acceptGiveItem,
			List<String> stepGiveItems, List<String> stepRemoveItems, Integer cutsceneId, Integer cutsceneAction) {
	}

	/**
	 * SimpleCollectItem 表行（真端 {@code quest_simplecollectitems}，262 行）。形状实测：
	 * 双 NPC 100%、{@code object1..4} 257 行（9649/9650/9654/9655/9656 五个 TEST 行只声明
	 * {@code reward_check} 无采集物）、{@code talk_npc1..3} 8 行、{@code party_drop} 80 行、
	 * {@code give_item}/{@code give_item1}/{@code remove_item2}/cutscene 长尾列按原文装载。
	 * <p>
	 * One SimpleCollectItem table row. Measured shape: the two-NPC pair is total, {@code object1..4}
	 * covers 257 rows (the five TEST rows 9649/9650/9654/9655/9656 declare only {@code reward_check}),
	 * {@code talk_npc1..3} covers 8 rows, {@code party_drop} 80 rows, and the give/remove/cutscene
	 * columns are loaded as the table's own text.
	 */
	public record SimpleCollectItemRow(int questId, String devName, String acquiredNpcName,
			String rewardNpcName, List<String> objects, List<String> talkNpcNames, Integer conQuest,
			String acceptGiveItem, String stepGiveItem, String removeItem2, Integer cutsceneId,
			Integer cutsceneAction, boolean partyDrop) {
	}

	/**
	 * SimpleUseItem 表行（真端 {@code quest_simpleuseitems}，160 行）。形状实测：本族**没有**
	 * {@code acquired_npc_name}——接取 = 使用 {@code use_item_name} 声明的道具；{@code use_item_name}/
	 * {@code reward_npc_name} 100%，{@code talk_npc1..3} 54/26/10 行（用物后中继链），{@code con_quest} 32 行，
	 * {@code give_itemN}/{@code remove_itemN} 8/4/3 与 10/6/3 行（第 N 步发/扣，实测这些行**都**声明了第 N 个中继 NPC），
	 * {@code item_check} 5 行（80482/80486/80612/80615/80616）。
	 * <p>
	 * One SimpleUseItem row. The family has no {@code acquired_npc_name}: the quest is accepted by
	 * using the declared item. The suffixed give/remove columns are step-scoped (every such row also
	 * declares the matching {@code talk_npcN}), so step lists keep their positions.
	 */
	public record SimpleUseItemRow(int questId, String devName, String useItemName, String rewardNpcName,
			List<String> talkNpcNames, Integer conQuest, boolean itemCheck, List<String> stepGiveItems,
			List<String> stepRemoveItems, Integer cutsceneId, Integer cutsceneAction) {
	}

	/**
	 * SimpleItemPlay 表行（真端 {@code quest_simpleitemplays}，43 行）。形状实测：{@code acquired_npc_name}/
	 * {@code reward_npc_name} 100%（NPC 接取），{@code use_item_name} 41 行（接取后使用道具推进），
	 * {@code give_item} 34 行（接取即发），{@code talk_npc1/2} 11/6 行，{@code con_quest} 9 行，
	 * {@code give_item1/2} 7/5 行、{@code remove_item1/2} 1/4 行（第 N 步发/扣，均带第 N 个中继 NPC），
	 * {@code cutsceneid1} 2 行。
	 * <p>
	 * One SimpleItemPlay row: the npc accepts and grants the item, using it advances, the relay chain and
	 * the hand-in follow. Step lists keep their positions (same convention as the talk family).
	 */
	public record SimpleItemPlayRow(int questId, String devName, String acquiredNpcName, String rewardNpcName,
			String useItemName, List<String> talkNpcNames, Integer conQuest, boolean itemCheck,
			String acceptGiveItem, List<String> stepGiveItems, List<String> stepRemoveItems,
			Integer cutsceneId, Integer cutsceneAction) {
	}

	/**
	 * CombineTask 表行（真端 {@code quest_combinetasks}，574 行）。形状实测：{@code task_npc} 100%
	 * （574/574 均为两名 = 天/魔各一）、{@code combineskill} / {@code combine_skillpoint} /
	 * {@code recipe_name} / {@code product} / {@code give_component1} 各 100%、{@code give_component2} 152 行；
	 * 真端 helper 允许 8 个分量槽位（{@code lVar4 = 8}），装载按位置保留（缺位 = {@code null}）。
	 * <p>
	 * One CombineTask row. Measured shape: all 574 rows carry two accept npcs, a combine skill, a skill
	 * point, a recipe name, a single product slot and at least one component; 152 rows add a second
	 * component. The retail helper allows eight component slots, so components keep their positions.
	 */
	public record CombineTaskRow(int questId, String devName, List<String> taskNpcNames, String combineSkill,
			int skillPoint, String recipeName, String product, List<String> components) {
	}

	private static final String RESOURCE = "aion/data/static_data/quest/retail/Quest_SimpleHunt.xml";
	private static final String SERIAL_RESOURCE = "aion/data/static_data/quest/retail/Quest_SimpleSerialHunt.xml";
	private static final String EXPECTED_SERIAL_ROOT = "quest_simpleserialhunts";
	private static final String COLLECT_RESOURCE =
			"aion/data/static_data/quest/retail/Quest_SimpleCollectItem.xml";
	private static final String EXPECTED_COLLECT_ROOT = "quest_simplecollectitems";
	private static final String TALK_RESOURCE = "aion/data/static_data/quest/retail/Quest_SimpleTalk.xml";
	private static final String EXPECTED_TALK_ROOT = "quest_simpletalks";
	private static final String USE_ITEM_RESOURCE = "aion/data/static_data/quest/retail/Quest_SimpleUseItem.xml";
	private static final String EXPECTED_USE_ITEM_ROOT = "quest_simpleuseitems";
	private static final String ITEM_PLAY_RESOURCE = "aion/data/static_data/quest/retail/Quest_SimpleItemPlay.xml";
	private static final String EXPECTED_ITEM_PLAY_ROOT = "quest_simpleitemplays";
	private static final String COMBINE_RESOURCE = "aion/data/static_data/quest/retail/Quest_CombineTask.xml";
	private static final String EXPECTED_COMBINE_ROOT = "quest_combinetasks";
	/** 真端 CombineTask helper 的分量槽位数（{@code lVar4 = 8}）。 / Retail component slot count. */
	private static final int COMBINE_COMPONENT_SLOTS = 8;
	private static final String EXPECTED_ROOT = "quest_simplehunts";
	private static final String ROW_TAG = "id";
	private static final Pattern COUNT_TAG = Pattern.compile("count([1-5])");
	private static final Pattern MONSTER_TAG = Pattern.compile("monster([1-5])");
	private static final Pattern COMMA = Pattern.compile("\\s*,\\s*");

	private static volatile NativeQuestTableLoader instance;

	private final Map<Integer, SimpleHuntRow> rowsByQuestId;
	private final Map<Integer, SimpleSerialHuntRow> serialRowsByQuestId;
	private final Map<Integer, SimpleTalkRow> talkRowsByQuestId;
	private final Map<Integer, SimpleCollectItemRow> collectRowsByQuestId;
	private final Map<Integer, SimpleUseItemRow> useItemRowsByQuestId;
	private final Map<Integer, SimpleItemPlayRow> itemPlayRowsByQuestId;
	private final Map<Integer, CombineTaskRow> combineRowsByQuestId;

	private NativeQuestTableLoader(Map<Integer, SimpleHuntRow> rowsByQuestId,
			Map<Integer, SimpleSerialHuntRow> serialRowsByQuestId,
			Map<Integer, SimpleTalkRow> talkRowsByQuestId,
			Map<Integer, SimpleCollectItemRow> collectRowsByQuestId,
			Map<Integer, SimpleUseItemRow> useItemRowsByQuestId,
			Map<Integer, SimpleItemPlayRow> itemPlayRowsByQuestId,
			Map<Integer, CombineTaskRow> combineRowsByQuestId) {
		this.rowsByQuestId = rowsByQuestId;
		this.serialRowsByQuestId = serialRowsByQuestId;
		this.talkRowsByQuestId = talkRowsByQuestId;
		this.collectRowsByQuestId = collectRowsByQuestId;
		this.useItemRowsByQuestId = useItemRowsByQuestId;
		this.itemPlayRowsByQuestId = itemPlayRowsByQuestId;
		this.combineRowsByQuestId = combineRowsByQuestId;
	}

	/** 已装载的表（未装载则先装载）。 / The loaded table; loads it first when absent. */
	public static NativeQuestTableLoader instance() {
		NativeQuestTableLoader local = instance;
		if (local == null) {
			synchronized (NativeQuestTableLoader.class) {
				local = instance;
				if (local == null) {
					local = load(NativeQuestTableLoader.class.getClassLoader());
					instance = local;
				}
			}
		}
		return local;
	}

	/** 启动期强制装载（失败即异常，由调用方决定是否终止启动）。 / Eagerly loads at startup; throws on failure. */
	public static void ensureLoaded() {
		instance();
	}

	/** 从 classpath 装载并解析表。 / Loads and parses the table from the classpath. */
	static NativeQuestTableLoader load(ClassLoader loader) {
		try (InputStream input = loader.getResourceAsStream(RESOURCE);
				InputStream serialInput = loader.getResourceAsStream(SERIAL_RESOURCE);
				InputStream talkInput = loader.getResourceAsStream(TALK_RESOURCE);
				InputStream collectInput = loader.getResourceAsStream(COLLECT_RESOURCE);
				InputStream useItemInput = loader.getResourceAsStream(USE_ITEM_RESOURCE);
				InputStream itemPlayInput = loader.getResourceAsStream(ITEM_PLAY_RESOURCE);
				InputStream combineInput = loader.getResourceAsStream(COMBINE_RESOURCE)) {
			if (input == null) {
				throw new IllegalStateException("NATIVE_TABLE_PARSE_FAILED: missing " + RESOURCE);
			}
			if (serialInput == null) {
				throw new IllegalStateException("NATIVE_TABLE_PARSE_FAILED: missing " + SERIAL_RESOURCE);
			}
			if (talkInput == null) {
				throw new IllegalStateException("NATIVE_TABLE_PARSE_FAILED: missing " + TALK_RESOURCE);
			}
			if (collectInput == null) {
				throw new IllegalStateException("NATIVE_TABLE_PARSE_FAILED: missing " + COLLECT_RESOURCE);
			}
			if (useItemInput == null) {
				throw new IllegalStateException("NATIVE_TABLE_PARSE_FAILED: missing " + USE_ITEM_RESOURCE);
			}
			if (itemPlayInput == null) {
				throw new IllegalStateException("NATIVE_TABLE_PARSE_FAILED: missing " + ITEM_PLAY_RESOURCE);
			}
			if (combineInput == null) {
				throw new IllegalStateException("NATIVE_TABLE_PARSE_FAILED: missing " + COMBINE_RESOURCE);
			}
			return parse(input, serialInput, talkInput, collectInput, useItemInput, itemPlayInput, combineInput);
		} catch (IOException e) {
			throw new IllegalStateException("NATIVE_TABLE_PARSE_FAILED: cannot read tables", e);
		}
	}

	/** 解析表字节流（包内可见供负例测试）。 / Parses the table stream (package-visible for negative tests). */
	static NativeQuestTableLoader parse(InputStream input) throws IOException {
		try (InputStream serialInput = NativeQuestTableLoader.class.getClassLoader()
				.getResourceAsStream(SERIAL_RESOURCE);
				InputStream talkInput = NativeQuestTableLoader.class.getClassLoader()
						.getResourceAsStream(TALK_RESOURCE);
				InputStream collectInput = NativeQuestTableLoader.class.getClassLoader()
						.getResourceAsStream(COLLECT_RESOURCE);
				InputStream useItemInput = NativeQuestTableLoader.class.getClassLoader()
						.getResourceAsStream(USE_ITEM_RESOURCE);
				InputStream itemPlayInput = NativeQuestTableLoader.class.getClassLoader()
						.getResourceAsStream(ITEM_PLAY_RESOURCE);
				InputStream combineInput = NativeQuestTableLoader.class.getClassLoader()
						.getResourceAsStream(COMBINE_RESOURCE)) {
			return parse(input, serialInput, talkInput, collectInput, useItemInput, itemPlayInput, combineInput);
		}
	}

	/** 解析狩猎 + 串行两表（包内可见供负例测试；Talk 表从 classpath 补足）。 / Parses the hunt and serial tables. */
	static NativeQuestTableLoader parse(InputStream input, InputStream serialInput) throws IOException {
		try (InputStream talkInput = NativeQuestTableLoader.class.getClassLoader()
				.getResourceAsStream(TALK_RESOURCE);
				InputStream collectInput = NativeQuestTableLoader.class.getClassLoader()
						.getResourceAsStream(COLLECT_RESOURCE);
				InputStream useItemInput = NativeQuestTableLoader.class.getClassLoader()
						.getResourceAsStream(USE_ITEM_RESOURCE);
				InputStream itemPlayInput = NativeQuestTableLoader.class.getClassLoader()
						.getResourceAsStream(ITEM_PLAY_RESOURCE);
				InputStream combineInput = NativeQuestTableLoader.class.getClassLoader()
						.getResourceAsStream(COMBINE_RESOURCE)) {
			return parse(input, serialInput, talkInput, collectInput, useItemInput, itemPlayInput, combineInput);
		}
	}

	/** 解析三张表的字节流（包内可见供负例测试）。 / Parses the three table streams. */
	static NativeQuestTableLoader parse(InputStream input, InputStream serialInput, InputStream talkInput)
			throws IOException {
		try (InputStream combineInput = NativeQuestTableLoader.class.getClassLoader()
				.getResourceAsStream(COMBINE_RESOURCE)) {
			return parse(input, serialInput, talkInput, NativeQuestTableLoader.class.getClassLoader()
					.getResourceAsStream(COLLECT_RESOURCE), NativeQuestTableLoader.class.getClassLoader()
							.getResourceAsStream(USE_ITEM_RESOURCE),
					NativeQuestTableLoader.class.getClassLoader().getResourceAsStream(ITEM_PLAY_RESOURCE),
					combineInput);
		}
	}

	/** 解析四张表的字节流（包内可见供负例测试）；P5 两表从 classpath 补足。 /
	 * Parses the four table streams (package-visible for negative tests); the two P5 tables come from the
	 * classpath. */
	static NativeQuestTableLoader parse(InputStream input, InputStream serialInput, InputStream talkInput,
			InputStream collectInput) throws IOException {
		try (InputStream useItemInput = NativeQuestTableLoader.class.getClassLoader()
				.getResourceAsStream(USE_ITEM_RESOURCE);
				InputStream itemPlayInput = NativeQuestTableLoader.class.getClassLoader()
						.getResourceAsStream(ITEM_PLAY_RESOURCE);
				InputStream combineInput = NativeQuestTableLoader.class.getClassLoader()
						.getResourceAsStream(COMBINE_RESOURCE)) {
			return parse(input, serialInput, talkInput, collectInput, useItemInput, itemPlayInput, combineInput);
		}
	}

	/** 解析六张表的字节流（包内可见供负例测试）；CombineTask 表从 classpath 补足。 /
	 * Parses the six table streams (package-visible for negative tests); the CombineTask table comes from
	 * the classpath. */
	static NativeQuestTableLoader parse(InputStream input, InputStream serialInput, InputStream talkInput,
			InputStream collectInput, InputStream useItemInput, InputStream itemPlayInput)
			throws IOException {
		try (InputStream combineInput = NativeQuestTableLoader.class.getClassLoader()
				.getResourceAsStream(COMBINE_RESOURCE)) {
			return parse(input, serialInput, talkInput, collectInput, useItemInput, itemPlayInput, combineInput);
		}
	}

	/** 解析七张表的字节流（包内可见供负例测试）。 / Parses the seven table streams. */
	static NativeQuestTableLoader parse(InputStream input, InputStream serialInput, InputStream talkInput,
			InputStream collectInput, InputStream useItemInput, InputStream itemPlayInput,
			InputStream combineInput) throws IOException {
		Document document;
		DocumentBuilder builder = newDocumentBuilder();
		try {
			document = builder.parse(input);
		} catch (org.xml.sax.SAXException e) {
			throw new IllegalStateException("NATIVE_TABLE_PARSE_FAILED: malformed Quest_SimpleHunt.xml", e);
		}
		Element root = document.getDocumentElement();
		if (root == null || !EXPECTED_ROOT.equals(root.getTagName())) {
			throw new IllegalStateException("NATIVE_TABLE_PARSE_FAILED: expected root <" + EXPECTED_ROOT
					+ ">, got <" + (root == null ? "(none)" : root.getTagName()) + ">");
		}
		Map<Integer, SimpleHuntRow> rows = new LinkedHashMap<>();
		NodeList children = root.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (!(child instanceof Element element) || !ROW_TAG.equals(element.getTagName())) {
				continue;
			}
			int questId = rowId(element);
			SimpleHuntRow row = new SimpleHuntRow(questId,
					optionalText(element, "acquired_npc_name"),
					optionalText(element, "reward_npc_name"),
					parseKillSlots(questId, element),
					optionalInt(element, "con_quest", questId),
					optionalInt(element, "cutsceneid1", questId),
					optionalInt(element, "cs1_haction", questId));
			if (rows.putIfAbsent(questId, row) != null) {
				throw new IllegalStateException("NATIVE_TABLE_PARSE_FAILED: duplicate quest id " + questId);
			}
		}
		if (rows.isEmpty()) {
			throw new IllegalStateException("NATIVE_TABLE_PARSE_FAILED: " + RESOURCE + " has no rows");
		}
		Map<Integer, SimpleSerialHuntRow> serialRows = serialInput != null ? loadSerial(serialInput, builder) : Map.of();
		Map<Integer, SimpleTalkRow> talkRows = talkInput != null ? loadTalk(talkInput, builder) : Map.of();
		Map<Integer, SimpleCollectItemRow> collectRows =
				collectInput != null ? loadCollect(collectInput, builder) : Map.of();
		Map<Integer, SimpleUseItemRow> useItemRows =
				useItemInput != null ? loadUseItem(useItemInput, builder) : Map.of();
		Map<Integer, SimpleItemPlayRow> itemPlayRows =
				itemPlayInput != null ? loadItemPlay(itemPlayInput, builder) : Map.of();
		Map<Integer, CombineTaskRow> combineRows =
				combineInput != null ? loadCombine(combineInput, builder) : Map.of();
		return new NativeQuestTableLoader(Collections.unmodifiableMap(rows),
				Collections.unmodifiableMap(serialRows), Collections.unmodifiableMap(talkRows),
				Collections.unmodifiableMap(collectRows), Collections.unmodifiableMap(useItemRows),
				Collections.unmodifiableMap(itemPlayRows), Collections.unmodifiableMap(combineRows));
	}

	private static Map<Integer, SimpleTalkRow> loadTalk(InputStream stream, DocumentBuilder builder) {
		Document document;
		try {
			document = builder.parse(stream);
		} catch (IOException | org.xml.sax.SAXException e) {
			throw new IllegalStateException("NATIVE_TABLE_PARSE_FAILED: malformed " + TALK_RESOURCE, e);
		}
		Element root = document.getDocumentElement();
		if (root == null || !EXPECTED_TALK_ROOT.equals(root.getTagName())) {
			throw new IllegalStateException("NATIVE_TABLE_PARSE_FAILED: expected root <" + EXPECTED_TALK_ROOT
					+ ">, got <" + (root == null ? "(none)" : root.getTagName()) + ">");
		}
		Map<Integer, SimpleTalkRow> rows = new LinkedHashMap<>();
		NodeList children = root.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (!(child instanceof Element element) || !ROW_TAG.equals(element.getTagName())) {
				continue;
			}
			int questId = rowId(element);
			SimpleTalkRow row = parseTalkRow(questId, element);
			if (rows.putIfAbsent(questId, row) != null) {
				throw new IllegalStateException("NATIVE_TABLE_PARSE_FAILED: duplicate talk quest id " + questId);
			}
		}
		if (rows.isEmpty()) {
			throw new IllegalStateException("NATIVE_TABLE_PARSE_FAILED: " + TALK_RESOURCE + " has no rows");
		}
		return rows;
	}

	/**
	 * SimpleTalk 行解析：双 NPC 为**必填**（真端 3152/3152），缺失即 fail-closed；
	 * give/remove 物品与 cutscene 按原文装载（不在装载层发明 count/id 语义）。
	 * <p>
	 * SimpleTalk row parsing: the two-NPC pair is mandatory in retail (3152/3152) and missing
	 * values fail closed; give/remove items and cutscenes are loaded as the table's own text so
	 * the loader invents no count/id semantics.
	 */
	private static SimpleTalkRow parseTalkRow(int questId, Element element) {
		String acquired = optionalText(element, "acquired_npc_name");
		String reward = optionalText(element, "reward_npc_name");
		if (acquired == null || acquired.isBlank()) {
			throw new IllegalStateException(
					"NATIVE_TABLE_PARSE_FAILED: talk quest " + questId + " has no acquired_npc_name");
		}
		if (reward == null || reward.isBlank()) {
			throw new IllegalStateException(
					"NATIVE_TABLE_PARSE_FAILED: talk quest " + questId + " has no reward_npc_name");
		}
		List<String> talkNpcs = new ArrayList<>(3);
		for (int i = 1; i <= 3; i++) {
			String talk = optionalText(element, "talk_npc" + i);
			if (talk != null && !talk.isBlank()) {
				talkNpcs.add(talk.strip());
			}
		}
		// 物品列按真端语义分槽：give_item = 接取侧发放；give_itemK/remove_itemK = 第 K 中继步的发放/扣除
		// （与 1131 的 cab520/cabb10 立即数逐字节对拍，见 p3-prereqs/simple-talk-codegen.md §5/§6）。
		String acceptGiveItem = null;
		String give = optionalText(element, "give_item");
		if (give != null && !give.isBlank()) {
			acceptGiveItem = normalizeValue(give);
		}
		List<String> stepGiveItems = new ArrayList<>(3);
		List<String> stepRemoveItems = new ArrayList<>(3);
		for (int i = 1; i <= 3; i++) {
			String indexed = optionalText(element, "give_item" + i);
			stepGiveItems.add(indexed != null && !indexed.isBlank() ? normalizeValue(indexed) : null);
			String removed = optionalText(element, "remove_item" + i);
			stepRemoveItems.add(removed != null && !removed.isBlank() ? normalizeValue(removed) : null);
		}
		Integer conQuest = optionalInt(element, "con_quest", questId);
		Integer cutsceneId = optionalInt(element, "cutsceneid1", questId);
		Integer cutsceneAction = optionalInt(element, "cs1_haction", questId);
		boolean itemCheck = "1".equals(optionalText(element, "item_check"));
		return new SimpleTalkRow(questId, optionalText(element, "dev_name"), acquired.strip(), reward.strip(),
				Collections.unmodifiableList(talkNpcs), conQuest, itemCheck, acceptGiveItem,
				Collections.unmodifiableList(stepGiveItems), Collections.unmodifiableList(stepRemoveItems),
				cutsceneId, cutsceneAction);
	}

	/** 归一元素文本内部空白（源表可跨行）。 / Normalizes inner whitespace (source text may span lines). */
	private static String normalizeValue(String raw) {
		return String.join(" ", raw.trim().split("\\s+"));
	}

	/** 可选整数元素：缺省返回 null，非数字 fail-closed。 / Optional int element: null when absent, fail closed when malformed. */
	private static Integer optionalInt(Element row, String tag, int questId) {
		String raw = optionalText(row, tag);
		if (raw == null || raw.isBlank()) {
			return null;
		}
		try {
			return Integer.valueOf(raw.trim());
		} catch (NumberFormatException e) {
			throw new IllegalStateException("NATIVE_TABLE_PARSE_FAILED: quest " + questId + " <" + tag
					+ "> is not a number: " + raw);
		}
	}

	private static Map<Integer, SimpleSerialHuntRow> loadSerial(InputStream stream, DocumentBuilder builder) {
		Document document;
		try {
			document = builder.parse(stream);
		} catch (IOException | org.xml.sax.SAXException e) {
			throw new IllegalStateException("NATIVE_TABLE_PARSE_FAILED: malformed " + SERIAL_RESOURCE, e);
		}
		Element root = document.getDocumentElement();
		if (root == null || !EXPECTED_SERIAL_ROOT.equals(root.getTagName())) {
			throw new IllegalStateException("NATIVE_TABLE_PARSE_FAILED: expected root <" + EXPECTED_SERIAL_ROOT
					+ ">, got <" + (root == null ? "(none)" : root.getTagName()) + ">");
		}
		Map<Integer, SimpleSerialHuntRow> rows = new LinkedHashMap<>();
		NodeList children = root.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (!(child instanceof Element element) || !ROW_TAG.equals(element.getTagName())) {
				continue;
			}
			int questId = rowId(element);
			SimpleSerialHuntRow row = parseSerialRow(questId, element);
			if (rows.putIfAbsent(questId, row) != null) {
				throw new IllegalStateException("NATIVE_TABLE_PARSE_FAILED: duplicate serial quest id " + questId);
			}
		}
		return rows;
	}

	private static SimpleSerialHuntRow parseSerialRow(int questId, Element element) {
		String devName = optionalText(element, "dev_name");
		String acquired = optionalText(element, "acquired_npc_name");
		String reward = optionalText(element, "reward_npc_name");
		List<String> talkNpcs = new ArrayList<>(4);
		for (int i = 1; i <= 4; i++) {
			String talk = optionalText(element, "talk_npc" + i);
			if (talk != null && !talk.isBlank()) {
				talkNpcs.add(talk.strip());
			}
		}
		String[] countTags = {"count_first", "count_second", "count_third", "count_fourth", "count_fifth"};
		String[] monsterTags = {"monster_first", "monster_second", "monster_third", "monster_fourth", "monster_fifth"};
		List<SerialStage> stages = new ArrayList<>(5);
		for (int i = 0; i < 5; i++) {
			String countStr = optionalText(element, countTags[i]);
			if (countStr != null && !countStr.isBlank()) {
				int count = Integer.parseInt(countStr.strip());
				if (count > 0) {
					String monsterStr = optionalText(element, monsterTags[i]);
					List<String> monsterList = new ArrayList<>();
					if (monsterStr != null && !monsterStr.isBlank()) {
						for (String m : COMMA.split(monsterStr.trim())) {
							String trimmed = m.strip();
							if (!trimmed.isEmpty()) {
								monsterList.add(trimmed);
							}
						}
					}
					stages.add(new SerialStage(i + 1, count, Collections.unmodifiableList(monsterList)));
				}
			}
		}
		return new SimpleSerialHuntRow(questId, devName, acquired, reward,
				Collections.unmodifiableList(talkNpcs), Collections.unmodifiableList(stages));
	}

	/**
	 * 装载 SimpleCollectItem 表：双 NPC 必填（真端 262/262），采集物 0..4 个按原文装载
	 * （五个 TEST 行无采集物，由处理器 at-load 视为不可路由，不在装载层伪造对象）。
	 * <p>
	 * Loads the SimpleCollectItem table: the two-NPC pair is mandatory in retail (262/262); the
	 * 0..4 collect objects are loaded as written (the five TEST rows carry none and are treated as
	 * unroutable at load time by the handler; no object is invented here).
	 */
	private static Map<Integer, SimpleCollectItemRow> loadCollect(InputStream stream, DocumentBuilder builder) {
		Document document;
		try {
			document = builder.parse(stream);
		} catch (IOException | org.xml.sax.SAXException e) {
			throw new IllegalStateException("NATIVE_TABLE_PARSE_FAILED: malformed " + COLLECT_RESOURCE, e);
		}
		Element root = document.getDocumentElement();
		if (root == null || !EXPECTED_COLLECT_ROOT.equals(root.getTagName())) {
			throw new IllegalStateException("NATIVE_TABLE_PARSE_FAILED: expected root <"
					+ EXPECTED_COLLECT_ROOT + ">, got <" + (root == null ? "(none)" : root.getTagName()) + ">");
		}
		Map<Integer, SimpleCollectItemRow> rows = new LinkedHashMap<>();
		NodeList children = root.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (!(child instanceof Element element) || !ROW_TAG.equals(element.getTagName())) {
				continue;
			}
			int questId = rowId(element);
			SimpleCollectItemRow row = parseCollectRow(questId, element);
			if (rows.putIfAbsent(questId, row) != null) {
				throw new IllegalStateException(
						"NATIVE_TABLE_PARSE_FAILED: duplicate collect quest id " + questId);
			}
		}
		if (rows.isEmpty()) {
			throw new IllegalStateException("NATIVE_TABLE_PARSE_FAILED: " + COLLECT_RESOURCE + " has no rows");
		}
		return rows;
	}

	private static SimpleCollectItemRow parseCollectRow(int questId, Element element) {
		String acquired = optionalText(element, "acquired_npc_name");
		String reward = optionalText(element, "reward_npc_name");
		if (acquired == null || acquired.isBlank()) {
			throw new IllegalStateException(
					"NATIVE_TABLE_PARSE_FAILED: collect quest " + questId + " has no acquired_npc_name");
		}
		if (reward == null || reward.isBlank()) {
			throw new IllegalStateException(
					"NATIVE_TABLE_PARSE_FAILED: collect quest " + questId + " has no reward_npc_name");
		}
		List<String> objects = new ArrayList<>(4);
		for (int i = 1; i <= 4; i++) {
			String object = optionalText(element, "object" + i);
			if (object != null && !object.isBlank()) {
				objects.add(object.strip());
			}
		}
		List<String> talkNpcs = new ArrayList<>(3);
		for (int i = 1; i <= 3; i++) {
			String talk = optionalText(element, "talk_npc" + i);
			if (talk != null && !talk.isBlank()) {
				talkNpcs.add(talk.strip());
			}
		}
		boolean partyDrop = "1".equals(optionalText(element, "party_drop").strip());
		return new SimpleCollectItemRow(questId, optionalText(element, "dev_name"), acquired, reward,
				Collections.unmodifiableList(objects), Collections.unmodifiableList(talkNpcs),
				optionalInt(element, "con_quest", questId),
				blankToNull(optionalText(element, "give_item")),
				blankToNull(optionalText(element, "give_item1")),
				blankToNull(optionalText(element, "remove_item2")),
				optionalInt(element, "cutsceneid1", questId),
				optionalInt(element, "cs1_haction", questId),
				partyDrop);
	}

	private static Map<Integer, SimpleUseItemRow> loadUseItem(InputStream stream, DocumentBuilder builder) {
		Document document;
		try {
			document = builder.parse(stream);
		} catch (IOException | org.xml.sax.SAXException e) {
			throw new IllegalStateException("NATIVE_TABLE_PARSE_FAILED: malformed " + USE_ITEM_RESOURCE, e);
		}
		Element root = document.getDocumentElement();
		if (root == null || !EXPECTED_USE_ITEM_ROOT.equals(root.getTagName())) {
			throw new IllegalStateException("NATIVE_TABLE_PARSE_FAILED: expected root <"
					+ EXPECTED_USE_ITEM_ROOT + ">, got <" + (root == null ? "(none)" : root.getTagName()) + ">");
		}
		Map<Integer, SimpleUseItemRow> rows = new LinkedHashMap<>();
		NodeList children = root.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (!(child instanceof Element element) || !ROW_TAG.equals(element.getTagName())) {
				continue;
			}
			int questId = rowId(element);
			SimpleUseItemRow row = parseUseItemRow(questId, element);
			if (rows.putIfAbsent(questId, row) != null) {
				throw new IllegalStateException(
						"NATIVE_TABLE_PARSE_FAILED: duplicate use-item quest id " + questId);
			}
		}
		if (rows.isEmpty()) {
			throw new IllegalStateException("NATIVE_TABLE_PARSE_FAILED: " + USE_ITEM_RESOURCE + " has no rows");
		}
		return rows;
	}

	/**
	 * SimpleUseItem 行解析：{@code use_item_name} 与 {@code reward_npc_name} 为必填（真端 160/160），
	 * 中继 NPC 与第 K 步发/扣物品按原文装载（位置保留，缺位为 {@code null}）。
	 * <p>
	 * SimpleUseItem row parsing: the use-item symbol and the reward npc are mandatory in retail
	 * (160/160); relay npcs and the step-scoped give/remove items keep their positions.
	 */
	private static SimpleUseItemRow parseUseItemRow(int questId, Element element) {
		String useItem = optionalText(element, "use_item_name");
		String reward = optionalText(element, "reward_npc_name");
		if (useItem == null || useItem.isBlank()) {
			throw new IllegalStateException(
					"NATIVE_TABLE_PARSE_FAILED: use-item quest " + questId + " has no use_item_name");
		}
		if (reward == null || reward.isBlank()) {
			throw new IllegalStateException(
					"NATIVE_TABLE_PARSE_FAILED: use-item quest " + questId + " has no reward_npc_name");
		}
		List<String> talkNpcs = new ArrayList<>(3);
		List<String> stepGiveItems = new ArrayList<>(3);
		List<String> stepRemoveItems = new ArrayList<>(3);
		for (int i = 1; i <= 3; i++) {
			String talk = optionalText(element, "talk_npc" + i);
			if (talk != null && !talk.isBlank()) {
				talkNpcs.add(talk.strip());
			}
			String give = optionalText(element, "give_item" + i);
			stepGiveItems.add(give != null && !give.isBlank() ? normalizeValue(give) : null);
			String removed = optionalText(element, "remove_item" + i);
			stepRemoveItems.add(removed != null && !removed.isBlank() ? normalizeValue(removed) : null);
		}
		return new SimpleUseItemRow(questId, optionalText(element, "dev_name"), normalizeValue(useItem),
				reward.strip(), Collections.unmodifiableList(talkNpcs),
				optionalInt(element, "con_quest", questId), "1".equals(optionalText(element, "item_check")),
				Collections.unmodifiableList(stepGiveItems), Collections.unmodifiableList(stepRemoveItems),
				optionalInt(element, "cutsceneid1", questId),
				optionalInt(element, "cs1_haction", questId));
	}

	private static Map<Integer, SimpleItemPlayRow> loadItemPlay(InputStream stream, DocumentBuilder builder) {
		Document document;
		try {
			document = builder.parse(stream);
		} catch (IOException | org.xml.sax.SAXException e) {
			throw new IllegalStateException("NATIVE_TABLE_PARSE_FAILED: malformed " + ITEM_PLAY_RESOURCE, e);
		}
		Element root = document.getDocumentElement();
		if (root == null || !EXPECTED_ITEM_PLAY_ROOT.equals(root.getTagName())) {
			throw new IllegalStateException("NATIVE_TABLE_PARSE_FAILED: expected root <"
					+ EXPECTED_ITEM_PLAY_ROOT + ">, got <" + (root == null ? "(none)" : root.getTagName()) + ">");
		}
		Map<Integer, SimpleItemPlayRow> rows = new LinkedHashMap<>();
		NodeList children = root.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (!(child instanceof Element element) || !ROW_TAG.equals(element.getTagName())) {
				continue;
			}
			int questId = rowId(element);
			SimpleItemPlayRow row = parseItemPlayRow(questId, element);
			if (rows.putIfAbsent(questId, row) != null) {
				throw new IllegalStateException(
						"NATIVE_TABLE_PARSE_FAILED: duplicate item-play quest id " + questId);
			}
		}
		if (rows.isEmpty()) {
			throw new IllegalStateException("NATIVE_TABLE_PARSE_FAILED: " + ITEM_PLAY_RESOURCE + " has no rows");
		}
		return rows;
	}

	/**
	 * SimpleItemPlay 行解析：接取/交付 NPC 为必填（真端 43/43），{@code give_item} = 接取发放，
	 * {@code give_itemK}/{@code remove_itemK} = 第 K 中继步的发/扣（位置保留）。
	 * <p>
	 * SimpleItemPlay row parsing: the accept and hand-in npcs are mandatory in retail (43/43);
	 * {@code give_item} is the accept grant and the indexed give/remove columns are step-scoped.
	 */
	private static SimpleItemPlayRow parseItemPlayRow(int questId, Element element) {
		String acquired = optionalText(element, "acquired_npc_name");
		String reward = optionalText(element, "reward_npc_name");
		if (acquired == null || acquired.isBlank()) {
			throw new IllegalStateException(
					"NATIVE_TABLE_PARSE_FAILED: item-play quest " + questId + " has no acquired_npc_name");
		}
		if (reward == null || reward.isBlank()) {
			throw new IllegalStateException(
					"NATIVE_TABLE_PARSE_FAILED: item-play quest " + questId + " has no reward_npc_name");
		}
		String useItem = optionalText(element, "use_item_name");
		String give = optionalText(element, "give_item");
		String acceptGiveItem = give != null && !give.isBlank() ? normalizeValue(give) : null;
		List<String> talkNpcs = new ArrayList<>(2);
		List<String> stepGiveItems = new ArrayList<>(2);
		List<String> stepRemoveItems = new ArrayList<>(2);
		for (int i = 1; i <= 2; i++) {
			String talk = optionalText(element, "talk_npc" + i);
			if (talk != null && !talk.isBlank()) {
				talkNpcs.add(talk.strip());
			}
			String indexed = optionalText(element, "give_item" + i);
			stepGiveItems.add(indexed != null && !indexed.isBlank() ? normalizeValue(indexed) : null);
			String removed = optionalText(element, "remove_item" + i);
			stepRemoveItems.add(removed != null && !removed.isBlank() ? normalizeValue(removed) : null);
		}
		return new SimpleItemPlayRow(questId, optionalText(element, "dev_name"), acquired.strip(),
				reward.strip(), useItem != null && !useItem.isBlank() ? normalizeValue(useItem) : null,
				Collections.unmodifiableList(talkNpcs), optionalInt(element, "con_quest", questId),
				"1".equals(optionalText(element, "item_check")), acceptGiveItem,
				Collections.unmodifiableList(stepGiveItems), Collections.unmodifiableList(stepRemoveItems),
				optionalInt(element, "cutsceneid1", questId),
				optionalInt(element, "cs1_haction", questId));
	}

	private static Map<Integer, CombineTaskRow> loadCombine(InputStream stream, DocumentBuilder builder) {
		Document document;
		try {
			document = builder.parse(stream);
		} catch (IOException | org.xml.sax.SAXException e) {
			throw new IllegalStateException("NATIVE_TABLE_PARSE_FAILED: malformed " + COMBINE_RESOURCE, e);
		}
		Element root = document.getDocumentElement();
		if (root == null || !EXPECTED_COMBINE_ROOT.equals(root.getTagName())) {
			throw new IllegalStateException("NATIVE_TABLE_PARSE_FAILED: expected root <"
					+ EXPECTED_COMBINE_ROOT + ">, got <" + (root == null ? "(none)" : root.getTagName()) + ">");
		}
		Map<Integer, CombineTaskRow> rows = new LinkedHashMap<>();
		NodeList children = root.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (!(child instanceof Element element) || !ROW_TAG.equals(element.getTagName())) {
				continue;
			}
			int questId = rowId(element);
			CombineTaskRow row = parseCombineRow(questId, element);
			if (rows.putIfAbsent(questId, row) != null) {
				throw new IllegalStateException(
						"NATIVE_TABLE_PARSE_FAILED: duplicate combine-task quest id " + questId);
			}
		}
		if (rows.isEmpty()) {
			throw new IllegalStateException("NATIVE_TABLE_PARSE_FAILED: " + COMBINE_RESOURCE + " has no rows");
		}
		return rows;
	}

	/**
	 * CombineTask 行解析：{@code task_npc}（逗号分隔，真端 574/574 两名）、{@code combineskill}、
	 * {@code recipe_name}、{@code product} 为必填；{@code combine_skillpoint} 缺省 0（与元数据交叉校验同口径）；
	 * 分量按 {@code give_component1..8} **位置保留**（缺位 = {@code null}）。
	 * <p>
	 * CombineTask row parsing: the accept npcs (comma separated), skill, recipe name and product slot are
	 * mandatory; the skill point defaults to zero and components keep their eight retail positions.
	 */
	private static CombineTaskRow parseCombineRow(int questId, Element element) {
		String taskNpc = optionalText(element, "task_npc");
		if (taskNpc == null || taskNpc.isBlank()) {
			throw new IllegalStateException(
					"NATIVE_TABLE_PARSE_FAILED: combine-task quest " + questId + " has no task_npc");
		}
		List<String> npcs = new ArrayList<>(2);
		for (String name : COMMA.split(taskNpc.strip())) {
			String trimmed = name.strip();
			if (!trimmed.isEmpty()) {
				npcs.add(trimmed);
			}
		}
		if (npcs.isEmpty()) {
			throw new IllegalStateException(
					"NATIVE_TABLE_PARSE_FAILED: combine-task quest " + questId + " has an empty task_npc list");
		}
		String skill = optionalText(element, "combineskill");
		if (skill == null || skill.isBlank()) {
			throw new IllegalStateException(
					"NATIVE_TABLE_PARSE_FAILED: combine-task quest " + questId + " has no combineskill");
		}
		String recipe = optionalText(element, "recipe_name");
		if (recipe == null || recipe.isBlank()) {
			throw new IllegalStateException(
					"NATIVE_TABLE_PARSE_FAILED: combine-task quest " + questId + " has no recipe_name");
		}
		String product = optionalText(element, "product");
		if (product == null || product.isBlank()) {
			throw new IllegalStateException(
					"NATIVE_TABLE_PARSE_FAILED: combine-task quest " + questId + " has no product");
		}
		Integer skillPoint = optionalInt(element, "combine_skillpoint", questId);
		if (skillPoint != null && skillPoint < 0) {
			throw new IllegalStateException("NATIVE_TABLE_PARSE_FAILED: combine-task quest " + questId
					+ " has a negative combine_skillpoint: " + skillPoint);
		}
		List<String> components = new ArrayList<>(COMBINE_COMPONENT_SLOTS);
		for (int i = 1; i <= COMBINE_COMPONENT_SLOTS; i++) {
			String component = optionalText(element, "give_component" + i);
			components.add(component != null && !component.isBlank() ? normalizeValue(component) : null);
		}
		return new CombineTaskRow(questId, optionalText(element, "dev_name"),
				Collections.unmodifiableList(npcs), skill.strip(), skillPoint == null ? 0 : skillPoint,
				recipe.strip(), normalizeValue(product), Collections.unmodifiableList(components));
	}

	private static String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.strip();
	}

	private static Map<Integer, KillSlot> parseKillSlots(int questId, Element row) {
		Map<Integer, Integer> counts = new TreeMap<>();
		Map<Integer, String> monsterTexts = new TreeMap<>();
		NodeList children = row.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (!(child instanceof Element element)) {
				continue;
			}
			Matcher count = COUNT_TAG.matcher(element.getTagName());
			if (count.matches()) {
				int slot = Integer.parseInt(count.group(1));
				if (counts.put(slot, requireOwnInt(element)) != null) {
					throw new IllegalStateException(
							"NATIVE_TABLE_PARSE_FAILED: quest " + questId + " has duplicate count" + slot);
				}
				continue;
			}
			Matcher monster = MONSTER_TAG.matcher(element.getTagName());
			if (monster.matches()) {
				monsterTexts.put(Integer.parseInt(monster.group(1)), element.getTextContent());
			}
		}
		Map<Integer, KillSlot> killSlots = new TreeMap<>();
		for (Map.Entry<Integer, Integer> entry : counts.entrySet()) {
			int slot = entry.getKey();
			// 有计数无 monster 属真端 13912 形（空名单 = 事件源缺失，行为永不满足），如实装载。
			// Count-without-monster is the retail 13912 shape (empty list = missing event source,
			// never satisfied); loaded as-is.
			String rawMonsters = monsterTexts.getOrDefault(slot, "");
			// 源表元素文本可跨行（37116）：先归一内部空白再按逗号切名单。
			// Source element text can span lines (37116): normalize inner whitespace, then split.
			String normalized = String.join(" ", rawMonsters.trim().split("\\s+"));
			List<String> monsters = new ArrayList<>();
			for (String name : COMMA.split(normalized)) {
				if (!name.isEmpty()) {
					monsters.add(name);
				}
			}
			killSlots.put(slot, new KillSlot(entry.getValue(), List.copyOf(monsters)));
		}
		for (int slot : monsterTexts.keySet()) {
			if (!killSlots.containsKey(slot)) {
				// 有 monster 无 count = 相机 required 缺失，真端不存在此形，fail-closed。
				// Monster-without-count lacks the camera required; retail has no such shape — fail closed.
				throw new IllegalStateException(
						"NATIVE_TABLE_PARSE_FAILED: quest " + questId + " has monster" + slot + " without count"
								+ slot);
			}
		}
		return Collections.unmodifiableSortedMap(new TreeMap<>(killSlots));
	}

	/** 读元素自身文本为整数（计数元素叶节点）。 / Reads the element's own text as an int (leaf count element). */
	private static int requireOwnInt(Element valueHolder) {
		String raw = valueHolder.getTextContent();
		if (raw == null || raw.isBlank()) {
			throw new IllegalStateException("NATIVE_TABLE_PARSE_FAILED: <" + valueHolder.getTagName()
					+ "> has no value");
		}
		try {
			return Integer.parseInt(raw.trim());
		} catch (NumberFormatException e) {
			throw new IllegalStateException("NATIVE_TABLE_PARSE_FAILED: <" + valueHolder.getTagName()
					+ "> is not a number: " + raw.trim());
		}
	}

	private static DocumentBuilder newDocumentBuilder() {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		try {
			factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
			// 保留内部 DTD 子集兼容（解析器能力面，负例测试仍依赖）；拒绝一切外部 DTD/实体为纵深防御
			// （精简副本已无 DOCTYPE，2026-10-03 剥离批）。
			// Keep internal-DTD-subset support (parser capability; negative tests still rely on it) and
			// refuse all external DTDs/entities as defense in depth (the simplified copies carry no DOCTYPE).
			factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
			factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
			factory.setExpandEntityReferences(true);
			return factory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			throw new IllegalStateException("NATIVE_TABLE_PARSE_FAILED: cannot configure XML parser", e);
		}
	}

	private static int requireInt(Element row, String tag) {
		String raw = textOf(row, tag);
		if (raw == null || raw.isBlank()) {
			throw new IllegalStateException(
					"NATIVE_TABLE_PARSE_FAILED: <" + ROW_TAG + "> is missing <" + tag + ">");
		}
		try {
			return Integer.parseInt(raw.trim());
		} catch (NumberFormatException e) {
			throw new IllegalStateException("NATIVE_TABLE_PARSE_FAILED: <" + tag + "> is not a number: " + raw);
		}
	}

	private static String optionalText(Element row, String tag) {
		String value = textOf(row, tag);
		return value == null ? "" : value.trim();
	}

	private static String textOf(Element row, String tag) {
		NodeList nodes = row.getElementsByTagName(tag);
		return nodes.getLength() == 0 ? null : nodes.item(0).getTextContent();
	}

	/** 行 id 在真端表里是行元素自身的 {@code id} 属性（{@code <id id="N">}）。 / The row id is the row element's own {@code id} attribute ({@code <id id="N">}). */
	private static int rowId(Element row) {
		String raw = row.getAttribute("id");
		if (raw.isBlank()) {
			throw new IllegalStateException("NATIVE_TABLE_PARSE_FAILED: <" + ROW_TAG + "> without id attribute");
		}
		try {
			return Integer.parseInt(raw.trim());
		} catch (NumberFormatException e) {
			throw new IllegalStateException("NATIVE_TABLE_PARSE_FAILED: id attribute is not a number: " + raw);
		}
	}

	/** SimpleTalk 全量行（只读集合）。 / All SimpleTalk rows. */
	public Collection<SimpleTalkRow> talkRows() {
		return talkRowsByQuestId.values();
	}

	/** SimpleTalk 行数。 / The number of SimpleTalk rows. */
	public int talkSize() {
		return talkRowsByQuestId.size();
	}

	/** 按任务查询 SimpleTalk 行，无行返回 Optional.empty()。 / Looks a SimpleTalk row up. */
	public Optional<SimpleTalkRow> findTalk(int questId) {
		return Optional.ofNullable(talkRowsByQuestId.get(questId));
	}

	/** 按任务查询 SimpleTalk 行，缺行 fail-closed。 / Looks a SimpleTalk row up; missing rows fail closed. */
	public SimpleTalkRow requireTalk(int questId) {
		SimpleTalkRow row = talkRowsByQuestId.get(questId);
		if (row == null) {
			throw new IllegalStateException("NATIVE_TABLE_ROW_MISSING: quest " + questId
					+ " has no SimpleTalk row");
		}
		return row;
	}

	/** SimpleCollectItem 全量行（只读集合）。 / All SimpleCollectItem rows. */
	public Collection<SimpleCollectItemRow> collectRows() {
		return collectRowsByQuestId.values();
	}

	/** SimpleCollectItem 行数。 / The number of SimpleCollectItem rows. */
	public int collectSize() {
		return collectRowsByQuestId.size();
	}

	/** 按任务查询采集行，无行返回 Optional.empty()。 / Looks a collect row up. */
	public Optional<SimpleCollectItemRow> findCollect(int questId) {
		return Optional.ofNullable(collectRowsByQuestId.get(questId));
	}

	/** 按任务查询采集行，缺行 fail-closed。 / Looks a collect row up; missing rows fail closed. */
	public SimpleCollectItemRow requireCollect(int questId) {
		SimpleCollectItemRow row = collectRowsByQuestId.get(questId);
		if (row == null) {
			throw new IllegalStateException("NATIVE_TABLE_ROW_MISSING: quest " + questId
					+ " has no SimpleCollectItem row");
		}
		return row;
	}

	/** SimpleUseItem 全量行（只读集合）。 / All SimpleUseItem rows. */
	public Collection<SimpleUseItemRow> useItemRows() {
		return useItemRowsByQuestId.values();
	}

	/** SimpleUseItem 行数。 / The number of SimpleUseItem rows. */
	public int useItemSize() {
		return useItemRowsByQuestId.size();
	}

	/** 按任务查询 SimpleUseItem 行，无行返回 Optional.empty()。 / Looks a SimpleUseItem row up. */
	public Optional<SimpleUseItemRow> findUseItem(int questId) {
		return Optional.ofNullable(useItemRowsByQuestId.get(questId));
	}

	/** 按任务查询 SimpleUseItem 行，缺行 fail-closed。 / Looks a SimpleUseItem row up; missing rows fail closed. */
	public SimpleUseItemRow requireUseItem(int questId) {
		SimpleUseItemRow row = useItemRowsByQuestId.get(questId);
		if (row == null) {
			throw new IllegalStateException("NATIVE_TABLE_ROW_MISSING: quest " + questId
					+ " has no SimpleUseItem row");
		}
		return row;
	}

	/** SimpleItemPlay 全量行（只读集合）。 / All SimpleItemPlay rows. */
	public Collection<SimpleItemPlayRow> itemPlayRows() {
		return itemPlayRowsByQuestId.values();
	}

	/** SimpleItemPlay 行数。 / The number of SimpleItemPlay rows. */
	public int itemPlaySize() {
		return itemPlayRowsByQuestId.size();
	}

	/** 按任务查询 SimpleItemPlay 行，无行返回 Optional.empty()。 / Looks a SimpleItemPlay row up. */
	public Optional<SimpleItemPlayRow> findItemPlay(int questId) {
		return Optional.ofNullable(itemPlayRowsByQuestId.get(questId));
	}

	/** 按任务查询 SimpleItemPlay 行，缺行 fail-closed。 / Looks a SimpleItemPlay row up; missing rows fail closed. */
	public SimpleItemPlayRow requireItemPlay(int questId) {
		SimpleItemPlayRow row = itemPlayRowsByQuestId.get(questId);
		if (row == null) {
			throw new IllegalStateException("NATIVE_TABLE_ROW_MISSING: quest " + questId
					+ " has no SimpleItemPlay row");
		}
		return row;
	}

	/** CombineTask 全量行（只读集合）。 / All CombineTask rows. */
	public Collection<CombineTaskRow> combineRows() {
		return combineRowsByQuestId.values();
	}

	/** CombineTask 行数。 / The number of CombineTask rows. */
	public int combineSize() {
		return combineRowsByQuestId.size();
	}

	/** 按任务查询 CombineTask 行，无行返回 Optional.empty()。 / Looks a CombineTask row up. */
	public Optional<CombineTaskRow> findCombine(int questId) {
		return Optional.ofNullable(combineRowsByQuestId.get(questId));
	}

	/** 按任务查询 CombineTask 行，缺行 fail-closed。 / Looks a CombineTask row up; missing rows fail closed. */
	public CombineTaskRow requireCombine(int questId) {
		CombineTaskRow row = combineRowsByQuestId.get(questId);
		if (row == null) {
			throw new IllegalStateException("NATIVE_TABLE_ROW_MISSING: quest " + questId
					+ " has no CombineTask row");
		}
		return row;
	}

	/** 表行数。 / The number of table rows. */
	public int size() {
		return rowsByQuestId.size();
	}

	/** 全部表行（questId 升序由底层 LinkedHashMap 装载序决定）。 / All rows (in load order). */
	public List<SimpleHuntRow> rows() {
		return List.copyOf(rowsByQuestId.values());
	}

	/** 按任务查询。 / Looks a row up by quest id. */
	public Optional<SimpleHuntRow> find(int questId) {
		return Optional.ofNullable(rowsByQuestId.get(questId));
	}

	/** 按任务查询，缺行 fail-closed。 / Looks a row up; missing rows fail closed. */
	public SimpleHuntRow require(int questId) {
		SimpleHuntRow row = rowsByQuestId.get(questId);
		if (row == null) {
			throw new IllegalStateException("NATIVE_TABLE_ROW_MISSING: quest " + questId
					+ " has no SimpleHunt row");
		}
		return row;
	}

	/** 串行任务全量行（只读集合）。 / All serial hunt rows. */
	public Collection<SimpleSerialHuntRow> serialHuntRows() {
		return serialRowsByQuestId.values();
	}

	/** 按任务查询串行行，无行返回 Optional.empty()。 / Looks a serial row up. */
	public Optional<SimpleSerialHuntRow> findSerial(int questId) {
		return Optional.ofNullable(serialRowsByQuestId.get(questId));
	}

	/** 按任务查询串行行，缺行 fail-closed。 / Looks a serial row up; missing rows fail closed. */
	public SimpleSerialHuntRow requireSerial(int questId) {
		SimpleSerialHuntRow row = serialRowsByQuestId.get(questId);
		if (row == null) {
			throw new IllegalStateException("NATIVE_TABLE_ROW_MISSING: serial quest " + questId
					+ " has no SimpleSerialHunt row");
		}
		return row;
	}

	/**
	 * 表行 → 相机行规约（纯函数；零计数行拒绝——真端休眠行不得派生相机）。
	 * Table row → camera row spec (pure; zero-count rows are rejected — dormant retail rows must
	 * never derive a camera row).
	 */
	public CameraRegistry.RowSpec cameraSpec(SimpleHuntRow row) {
		if (row.killSlots().isEmpty()) {
			throw new IllegalStateException(
					"NATIVE_CAMERA_ROW_MISSING: quest " + row.questId() + " has no kill counts");
		}
		RawQuestVarsCodec.Width width = widthOf(row);
		Map<Integer, Integer> slotRequires = new TreeMap<>();
		int fullValue = 0;
		for (Map.Entry<Integer, KillSlot> entry : row.killSlots().entrySet()) {
			int slot = entry.getKey();
			int count = entry.getValue().count();
			slotRequires.put(slot, count);
			fullValue |= count << width.shift(slot);
		}
		return new CameraRegistry.RowSpec(row.questId(), width, fullValue, slotRequires);
	}

	/**
	 * 串行表行 → 相机行规约（纯函数；全 16 行均在 6 位掩码内）。
	 * Serial table row → camera row spec (pure; all 16 rows fit in the 6-bit mask).
	 */
	public CameraRegistry.RowSpec cameraSpec(SimpleSerialHuntRow row) {
		if (row.stages().isEmpty()) {
			throw new IllegalStateException(
					"NATIVE_CAMERA_ROW_MISSING: serial quest " + row.questId() + " has no stages");
		}
		RawQuestVarsCodec.Width width = RawQuestVarsCodec.Width.SIX;
		Map<Integer, Integer> slotRequires = new TreeMap<>();
		int fullValue = 0;
		for (SerialStage stage : row.stages()) {
			int slot = stage.stage();
			int count = stage.count();
			slotRequires.put(slot, count);
			fullValue |= count << width.shift(slot);
		}
		return new CameraRegistry.RowSpec(row.questId(), width, fullValue, slotRequires);
	}

	/**
	 * 采集行 → 相机行规约：单槽（槽 1）+ {@code collect_item1} 的 required（真端 collect 表 262 行
	 * 里 253 行有采集计数，最大 40；9 行为事件/测试形态不派生相机行）。宽度规则同狩猎族。
	 * <p>
	 * Collect row → camera row spec: one slot (slot 1) with the {@code collect_item1} requirement
	 * (253 of the 262 retail collect rows carry a count, max 40; the 9 event/test shapes derive no
	 * camera row). Width rule mirrors the hunt family.
	 */
	public CameraRegistry.RowSpec cameraSpec(int questId, Map<Integer, Integer> slotRequires) {
		if (slotRequires == null || slotRequires.isEmpty()) {
			throw new IllegalStateException(
					"NATIVE_CAMERA_ROW_MISSING: collect quest " + questId + " has no collect counts");
		}
		RawQuestVarsCodec.Width width = RawQuestVarsCodec.Width.SIX;
		for (int required : slotRequires.values()) {
			if (required > width.slotMask()) {
				width = RawQuestVarsCodec.Width.TEN;
				break;
			}
		}
		Map<Integer, Integer> ordered = new TreeMap<>(slotRequires);
		int fullValue = 0;
		for (Map.Entry<Integer, Integer> entry : ordered.entrySet()) {
			fullValue |= entry.getValue() << width.shift(entry.getKey());
		}
		return new CameraRegistry.RowSpec(questId, width, fullValue, ordered);
	}

	/**
	 * 宽度规则：任一 count 超 6 位掩码 ⇒ 10 位，否则 6 位（对拍 1812 任务零失败）。
	 * Width rule: any count above the 6-bit mask implies 10-bit, else 6-bit (zero failures across
	 * the 1812 reconciled quests).
	 */
	static RawQuestVarsCodec.Width widthOf(SimpleHuntRow row) {
		int sixBitMask = RawQuestVarsCodec.Width.SIX.slotMask();
		for (KillSlot slot : row.killSlots().values()) {
			if (slot.count() > sixBitMask) {
				return RawQuestVarsCodec.Width.TEN;
			}
		}
		return RawQuestVarsCodec.Width.SIX;
	}
}
