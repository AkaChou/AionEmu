package com.aionemu.gameserver.questEngine.tablelane;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * 原版 DataDriven 模板表的**原生行模型**（计划 §10.2「P7 DataDriven」步 2）。
 * <p>
 * 与旧 IR 面（`RetailDataDrivenTable` + `RetailDataDriven*Compiler`）的区别：本类按原版
 * 「每步 = 一条 handler 记录」建模——步 kind（原版 `QuestProgressExtraInfo_*` 对象）+
 * 类别载荷列 + 通用附加动作列（原版 `LoadExtraAction`，证据与守卫见
 * `p7/P7-STEP2-PREREQ-COLUMN-SEMANTICS.zh-CN.md`），供 `DataDrivenProgress` 与 DD 原生 handler 直接消费。
 * <p>
 * 装载即校验（fail-closed）：未知类别、缺 `value0_progress_`、类别与列号的非法组合（原版 guard 不放行的组合）
 * 一律抛稳定码异常，绝不静默吞列。
 * <p>
 * Native row model of the retail DataDriven template table: one record per step with the step kind
 * (retail {@code QuestProgressExtraInfo_*} object), the category payload columns and the generic
 * extra-action columns admitted by the retail {@code LoadExtraAction} guard. Load-time validation is
 * fail-closed: unknown categories, missing payloads and illegal category/column combinations throw
 * stable codes instead of silently dropping data.
 */
public final class DataDrivenQuestTable {

	/** 步 kind（原版 `QuestProgressExtraInfo_*`；1=CollectItem / 2=Hunt / 3=ItemPlay / 4=Talk / 5=PvP / 6=EnterArea / 7=EnterWorld / 9=TalkFOBJ）。 */
	public enum Kind {
		HUNT("hunt"),
		COLLECT_ITEM("collectitem"),
		PVP("pvp"),
		TALK("talk"),
		ENTER_AREA("enterarea"),
		ITEM_PLAY("itemplay"),
		ENTER_WORLD("enterworld"),
		TALK_FOBJ("talkfobj");

		private final String tableName;

		Kind(String tableName) {
			this.tableName = tableName;
		}

		/** 表里的类别名（小写规范形）。 / The lower-cased table category name. */
		public String tableName() {
			return tableName;
		}

		/** 按表里的类别名解析（未知类别返回空）。 / Resolves a table category name (empty when unknown). */
		public static Optional<Kind> of(String tableName) {
			if (tableName == null) {
				return Optional.empty();
			}
			String normalised = tableName.trim().toLowerCase(Locale.ROOT);
			for (Kind kind : values()) {
				if (kind.tableName.equals(normalised)) {
					return Optional.of(kind);
				}
			}
			return Optional.empty();
		}
	}

	/**
	 * 通用附加动作（原版 `FUN_180c49610` = `DataDrivenQuestLoader::LoadExtraAction`，按列号分派）。
	 * Generic extra action, dispatched by the value column index.
	 */
	public enum ExtraAction {
		/** 列 1：`Give/Remove Items` 的发放半边。 / Column 1: the give side of Give/Remove Items. */
		GIVE_ITEMS(1),
		/** 列 2：`Give/Remove Items` 的回收半边。 / Column 2: the remove side of Give/Remove Items. */
		REMOVE_ITEMS(2),
		/** 列 3：`Teleport To`（世界 x y z heading）。 / Column 3: Teleport To. */
		TELEPORT(3),
		/** 列 4：`Play Cutscene`（Cutscene|Cutscene2|Movie|Movie2 [+HACTION_*]）。 / Column 4: Play Cutscene. */
		CUTSCENE(4),
		/** 列 5：`Spawn Npcs`（Absolute|Relative）。 / Column 5: Spawn Npcs. */
		SPAWN(5),
		/** 列 6：`Delay Time`（毫秒）。 / Column 6: Delay Time. */
		DELAY(6),
		/** 列 7/8：`Message`（字符串索引）。 / Columns 7/8: Message. */
		MESSAGE(7),
		/** 列 9：`Enter Instance`。 / Column 9: Enter Instance. */
		ENTER_INSTANCE(9),
		/** 列 10：`Add Timer`。 / Column 10: Add Timer. */
		TIMER(10);

		private final int column;

		ExtraAction(int column) {
			this.column = column;
		}

		/** 该动作的主列号（MESSAGE 覆盖 7 与 8）。 / The primary column index (MESSAGE spans 7 and 8). */
		public int column() {
			return column;
		}

		/** 按列号解析（无动作返回空）。 / Resolves the action for a column index. */
		public static Optional<ExtraAction> ofColumn(int column) {
			if (column == 8) {
				return Optional.of(MESSAGE);
			}
			for (ExtraAction action : values()) {
				if (action.column == column) {
					return Optional.of(action);
				}
			}
			return Optional.empty();
		}
	}

	/**
	 * 一个进度步。 / One progress step.
	 *
	 * @param index   步号（原版注册期写入的 `expectedStep`，落 vars bit0-5）/ the step number
	 * @param kind    步类别 / the step kind
	 * @param columns 该步声明的全部非空 `valueN_progress_` 列（列号 → 原文）/ non-empty value columns
	 */
	public record Step(int index, Kind kind, Map<Integer, String> columns) {

		/** 类别载荷（原版 `value0_progress_`）。 / The category payload (retail {@code value0_progress_}). */
		public String payload() {
			return columns.getOrDefault(0, "");
		}

		/**
		 * 该步的通用附加动作（按列号顺序）。 / The step's extra actions, in column order.
		 */
		public List<ExtraAction> extraActions() {
			List<ExtraAction> actions = new ArrayList<>();
			Set<Integer> payloads = PAYLOAD_COLUMNS.get(kind);
			for (Integer column : new TreeMap<>(columns).keySet()) {
				if (payloads.contains(column)) {
					// 类别载荷列（原版 FUN_180c4b980 先解析）不是附加动作：PvP 的列 3 = 等级差、
					// CollectItem 的列 1..4 = 追加 FOBJ、列 5 = 整数。
					// Category payload columns are parsed before the extra-action pass and never act as actions.
					continue;
				}
				ExtraAction.ofColumn(column).ifPresent(actions::add);
			}
			return List.copyOf(actions);
		}

		/** 某列的原文（未声明返回 null）。 / The raw text of one column (null when undeclared). */
		public String column(int index) {
			return columns.get(index);
		}
	}

	/**
	 * 一行 DD 行。 / One DataDriven row.
	 *
	 * @param questId      任务 id / quest id
	 * @param acquireKind  原版接取类别原文（小写规范形）/ the acquire category
	 * @param acquireParam 接取参数（`value0_acquire_`）/ the acquire parameter
	 * @param rewardNpc    原版领奖 NPC 名（`reward_npc_name`）/ the reward npc name
	 * @param conQuest     接取条件列原文（`con_quest`，语义未坐实只装载）/ raw acquire-condition column
	 * @param conQuestList 接取条件列原文（`con_quest_list`，语义未坐实只装载）/ raw acquire-condition list column
	 * @param acceptColumns 接取行附加动作列（`value1..10_acquire_`；原版 `FUN_180c49120` 装载进
	 *                     `QuestProgressExtraInfo` 对象，接取收尾独取此表）/ the accept-side extra-action
	 *                     columns, loaded into the retail QuestProgressExtraInfo object
	 * @param steps        进度步序列（表序，index = 位置）/ the ordered progress steps
	 */
	public record Row(int questId, String acquireKind, String acquireParam, String rewardNpc, String conQuest,
			String conQuestList, Map<Integer, String> acceptColumns, List<Step> steps) {

		/** 是否带未坐实的接取条件列。 / Whether the row carries un-adjudicated acquire conditions. */
		public boolean hasAcquireConditions() {
			return (conQuest != null && !conQuest.isBlank()) || (conQuestList != null && !conQuestList.isBlank());
		}
	}

	/** 类别载荷列（原版 `FUN_180c4b980` 按 kind 解析的列号）。 / Category payload columns per kind. */
	private static final Map<Kind, Set<Integer>> PAYLOAD_COLUMNS = Map.of(
		Kind.HUNT, Set.of(0),
		Kind.COLLECT_ITEM, Set.of(0, 1, 2, 3, 4, 5),
		Kind.PVP, Set.of(0, 1, 2, 3),
		Kind.TALK, Set.of(0),
		Kind.ENTER_AREA, Set.of(0),
		Kind.ITEM_PLAY, Set.of(0),
		Kind.ENTER_WORLD, Set.of(0),
		Kind.TALK_FOBJ, Set.of(0));

	/** 原版 `LoadExtraAction` guard 放行的附加动作列。 / Extra-action columns the retail guard admits. */
	private static final Map<Kind, Set<Integer>> EXTRA_ACTION_COLUMNS = Map.of(
		Kind.HUNT, Set.of(4, 5),
		Kind.ITEM_PLAY, Set.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10),
		Kind.TALK, Set.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10),
		Kind.ENTER_AREA, Set.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10),
		Kind.ENTER_WORLD, Set.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10),
		Kind.TALK_FOBJ, Set.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));

	private final Map<Integer, Row> rows;

	private DataDrivenQuestTable(Map<Integer, Row> rows) {
		this.rows = Map.copyOf(rows);
	}

	/** 按任务 id 取行。 / Looks up a row by quest id. */
	public Optional<Row> find(int questId) {
		return Optional.ofNullable(rows.get(questId));
	}

	/** 全部任务 id。 / All quest ids. */
	public Set<Integer> questIds() {
		return rows.keySet();
	}

	public int size() {
		return rows.size();
	}

	/**
	 * 解析原版 DD 表（精简副本已无 DOCTYPE，2026-10-03 剥离批；schema = 同目录
	 * {@code data_driven_quest.xsd}；解析器保留内部子集能力、外部访问一律拒绝）。
	 * Parses the retail DD table (the repo copy carries no DOCTYPE after the 2026-10-03 strip batch;
	 * schema in the sibling {@code data_driven_quest.xsd}; internal-subset capability kept, external
	 * access denied).
	 */
	public static DataDrivenQuestTable load(InputStream input) throws IOException {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(false);
			factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", false);
			factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
			factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
			Document document = factory.newDocumentBuilder().parse(input);
			Map<Integer, Row> parsed = new LinkedHashMap<>();
			NodeList nodes = document.getElementsByTagName("quest_data_driven");
			for (int index = 0; index < nodes.getLength(); index++) {
				Node node = nodes.item(index);
				if (node instanceof Element element) {
					Row row = parseRow(element);
					parsed.put(row.questId(), row);
				}
			}
			return new DataDrivenQuestTable(parsed);
		} catch (IOException e) {
			throw e;
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new IOException("failed to parse retail DataDriven table", e);
		}
	}

	private static Row parseRow(Element element) {
		int questId = Integer.parseInt(text(element, "id"));
		String acquire = text(element, "category_acquire_");
		String acquireParam = text(element, "value0_acquire_");
		String reward = text(element, "reward_npc_name");
		// 原版 LoadBasicInfo 解析的接取条件列（0x640 条目 type 表未坐实 ⇒ 步 e1 只装载不解释）。
		// The acquire-condition columns parsed by retail LoadBasicInfo (the 0x640 entry type table is
		// not adjudicated yet ⇒ step e1 only loads them, never interprets them).
		String conQuest = text(element, "con_quest");
		String conQuestList = text(element, "con_quest_list");
		// 接取行附加动作列（value1..10_acquire_）：原版与进度列同轴解析（`FUN_180c49120` 循环
		// `value%d_acquire_` → `FUN_180c4b980`），装载进 `QuestProgressExtraInfo` 对象，
		// **由接取收尾独取**——不是进度步 0 的动作（13403 实机：误跑步 0 动作 ⇒ 探测器双发）。
		// Accept-side extra-action columns (value1..10_acquire_): parsed on the same axis as the
		// progress columns and stored in the retail QuestProgressExtraInfo object; the ACQUIRE tail
		// executes them — never progress step 0's actions (live 13403 double-grant cause).
		Map<Integer, String> acceptColumns = new TreeMap<>();
		NodeList acquireFields = element.getChildNodes();
		for (int child = 0; child < acquireFields.getLength(); child++) {
			Node item = acquireFields.item(child);
			if (!(item instanceof Element field)) {
				continue;
			}
			String name = field.getTagName();
			if (!name.startsWith("value") || !name.endsWith("_acquire_")) {
				continue;
			}
			String value = field.getTextContent();
			if (value == null || value.isBlank()) {
				continue;
			}
			int column;
			try {
				column = Integer.parseInt(name.substring("value".length(), name.indexOf("_acquire_")));
			} catch (NumberFormatException e) {
				continue;
			}
			if (column >= 1) {
				acceptColumns.put(column, value.trim());
			}
		}
		List<Step> steps = new ArrayList<>();
		NodeList infos = element.getElementsByTagName("data");
		for (int index = 0; index < infos.getLength(); index++) {
			Node node = infos.item(index);
			if (!(node instanceof Element data)) {
				continue;
			}
			String rawCategory = text(data, "category_progress_");
			if (rawCategory == null) {
				continue;
			}
			Kind kind = Kind.of(rawCategory).orElseThrow(() -> new IllegalStateException(
				"DATA_DRIVEN_STEP_KIND_UNKNOWN: quest " + questId + " category " + rawCategory));
			Map<Integer, String> columns = new TreeMap<>();
			NodeList children = data.getChildNodes();
			for (int child = 0; child < children.getLength(); child++) {
				Node item = children.item(child);
				if (!(item instanceof Element field)) {
					continue;
				}
				String name = field.getTagName();
				if (!name.startsWith("value") || !name.endsWith("_progress_")) {
					continue;
				}
				String value = field.getTextContent();
				if (value == null || value.isBlank()) {
					continue;
				}
				int column;
				try {
					column = Integer.parseInt(name.substring("value".length(), name.indexOf("_progress_")));
				} catch (NumberFormatException e) {
					continue;
				}
				columns.put(column, value.trim());
			}
			validateColumns(questId, kind, columns);
			steps.add(new Step(steps.size(), kind, Map.copyOf(columns)));
		}
		return new Row(questId, acquire == null ? "" : acquire.trim().toLowerCase(Locale.ROOT), acquireParam,
			reward, conQuest == null ? "" : conQuest.trim(), conQuestList == null ? "" : conQuestList.trim(),
			Map.copyOf(acceptColumns), List.copyOf(steps));
	}

	/** 列面 fail-closed 校验：每步必须声明载荷列，且每个列号必须落在原版放行的载荷/附加动作列内。 */
	private static void validateColumns(int questId, Kind kind, Map<Integer, String> columns) {
		if (!columns.containsKey(0)) {
			throw new IllegalStateException("DATA_DRIVEN_STEP_PAYLOAD_MISSING: quest " + questId
				+ " category " + kind.tableName());
		}
		Set<Integer> payloads = PAYLOAD_COLUMNS.get(kind);
		Set<Integer> extras = EXTRA_ACTION_COLUMNS.getOrDefault(kind, Set.of());
		for (Integer column : columns.keySet()) {
			if (!payloads.contains(column) && !extras.contains(column)) {
				throw new IllegalStateException("DATA_DRIVEN_STEP_COLUMN_ILLEGAL: quest " + questId
					+ " category " + kind.tableName() + " column " + column);
			}
		}
	}

	private static String text(Element parent, String tag) {
		NodeList nodes = parent.getElementsByTagName(tag);
		if (nodes.getLength() == 0) {
			return null;
		}
		String value = nodes.item(0).getTextContent();
		return value == null || value.isBlank() ? null : value.trim();
	}
}
