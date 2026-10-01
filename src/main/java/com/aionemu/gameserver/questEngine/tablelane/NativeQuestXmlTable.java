package com.aionemu.gameserver.questEngine.tablelane;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * 真端 {@code quest.xml} 元数据行装载器（计划 §6.2：quest.xml 划归 NativeQuestTableLoader 族；
 * 本类是数据层——只装载与类型化访问，不做任何语义合成；起始条件/奖励组/职业奖励的语义提取
 * 属各家族切换批）。行模型：{@code <quests><quest>…</quest></quests>}，id 为行内
 * {@code <id>} 子元素。字段 = 行的**直接子元素**（实测全部单值；模型仍按多值保留首现序，以防
 * 数据演化）；同名嵌套后代不参与字段读取，容器型字段（如 {@code *_selectable_reward}）以
 * 后代拼接文本透出，结构化访问由后续批次决定——不发明语义。
 * <p>
 * The retail {@code quest.xml} metadata row loader (plan §6.2: quest.xml belongs to the
 * NativeQuestTableLoader family; this class is the data layer only — loading and typed access,
 * zero semantic synthesis; semantic extraction of start conditions / reward groups / class
 * rewards belongs to each family's switch batch). Row model: {@code <quests><quest>…</quest></quests>}
 * with the id as an {@code <id>} child element. Fields are the row's **direct children** (all
 * single-valued in the live data; the model still keeps first-occurrence-ordered lists in case
 * the data evolves); same-named descendants are never read as fields, and container fields
 * (e.g. {@code *_selectable_reward}) surface as concatenated descendant text — structured access
 * is a later batch's decision, no invented semantics.
 */
public final class NativeQuestXmlTable {

	/** quest.xml 行：id + 直接子字段（多值，首现序）。 / One quest.xml row: id + direct-child fields (multi-valued, first-occurrence order). */
	public record QuestRow(int questId, Map<String, List<String>> fields) {

		/** 首个值（缺字段/空值返回 ""）。 / The first value ("" when absent or empty). */
		public String text(String tag) {
			List<String> values = fields.get(tag);
			return values == null || values.isEmpty() ? "" : values.get(0);
		}

		/** 全部值（缺字段返回空表）。 / All values (empty when absent). */
		public List<String> list(String tag) {
			List<String> values = fields.get(tag);
			return values == null ? List.of() : values;
		}

		/**
		 * 收集带数字后缀的字段族（{@code collect_item1..N} → 按槽位升序，每槽取首值）。
		 * Collects a numbered tag family in ascending slot order (first value per slot).
		 */
		public List<String> numbered(String base) {
			java.util.Map<Integer, String> bySlot = new java.util.TreeMap<>();
			for (Map.Entry<String, List<String>> field : fields.entrySet()) {
				String key = field.getKey();
				if (!key.startsWith(base) || field.getValue().isEmpty()) {
					continue;
				}
				String suffix = key.substring(base.length());
				if (!suffix.isEmpty() && suffix.chars().allMatch(Character::isDigit)) {
					bySlot.put(Integer.parseInt(suffix), field.getValue().get(0));
				}
			}
			return List.copyOf(bySlot.values());
		}

		/** 首个值解析为整数（缺字段返回 null；非数字 fail-closed）。 / First value as Integer (null when absent; non-numeric fails closed). */
		public Integer integer(String tag) {
			String raw = text(tag);
			if (raw.isEmpty()) {
				return null;
			}
			try {
				return Integer.parseInt(raw.trim());
			} catch (NumberFormatException e) {
				throw new IllegalStateException("NATIVE_TABLE_PARSE_FAILED: quest " + questId + " <" + tag
						+ "> is not a number: " + raw);
			}
		}

		/** 首个值 == "1"。 / First value equals "1". */
		public boolean bool(String tag) {
			return "1".equals(text(tag));
		}
	}

	private static final String RESOURCE = "aion/data/static_data/quest/retail/quest.xml";
	private static final String EXPECTED_ROOT = "quests";
	private static final String ROW_TAG = "quest";
	private static final String ID_TAG = "id";

	private static volatile NativeQuestXmlTable instance;

	private final Map<Integer, QuestRow> rowsByQuestId;

	private NativeQuestXmlTable(Map<Integer, QuestRow> rowsByQuestId) {
		this.rowsByQuestId = rowsByQuestId;
	}

	/** 已装载的表（未装载则先装载）。 / The loaded table; loads it first when absent. */
	public static NativeQuestXmlTable instance() {
		NativeQuestXmlTable local = instance;
		if (local == null) {
			synchronized (NativeQuestXmlTable.class) {
				local = instance;
				if (local == null) {
					local = load(NativeQuestXmlTable.class.getClassLoader());
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
	static NativeQuestXmlTable load(ClassLoader loader) {
		try (InputStream input = loader.getResourceAsStream(RESOURCE)) {
			if (input == null) {
				throw new IllegalStateException("NATIVE_TABLE_PARSE_FAILED: missing " + RESOURCE);
			}
			return parse(input);
		} catch (IOException e) {
			throw new IllegalStateException("NATIVE_TABLE_PARSE_FAILED: cannot read " + RESOURCE, e);
		}
	}

	/** 解析表字节流（包内可见供负例测试）。 / Parses the table stream (package-visible for negative tests). */
	static NativeQuestXmlTable parse(InputStream input) throws IOException {
		Document document;
		try {
			document = newDocumentBuilder().parse(input);
		} catch (org.xml.sax.SAXException e) {
			throw new IllegalStateException("NATIVE_TABLE_PARSE_FAILED: malformed quest.xml", e);
		}
		Element root = document.getDocumentElement();
		if (root == null || !EXPECTED_ROOT.equals(root.getTagName())) {
			throw new IllegalStateException("NATIVE_TABLE_PARSE_FAILED: expected root <" + EXPECTED_ROOT
					+ ">, got <" + (root == null ? "(none)" : root.getTagName()) + ">");
		}
		Map<Integer, QuestRow> rows = new LinkedHashMap<>();
		NodeList children = root.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (!(child instanceof Element element) || !ROW_TAG.equals(element.getTagName())) {
				continue;
			}
			QuestRow row = new QuestRow(rowId(element), parseFields(element));
			if (rows.putIfAbsent(row.questId(), row) != null) {
				throw new IllegalStateException(
						"NATIVE_TABLE_PARSE_FAILED: duplicate quest id " + row.questId());
			}
		}
		if (rows.isEmpty()) {
			throw new IllegalStateException("NATIVE_TABLE_PARSE_FAILED: " + RESOURCE + " has no rows");
		}
		return new NativeQuestXmlTable(Collections.unmodifiableMap(rows));
	}

	private static int rowId(Element row) {
		String raw = directChildText(row, ID_TAG);
		if (raw.isBlank()) {
			throw new IllegalStateException(
					"NATIVE_TABLE_PARSE_FAILED: <" + ROW_TAG + "> without <" + ID_TAG + ">");
		}
		try {
			return Integer.parseInt(raw.trim());
		} catch (NumberFormatException e) {
			throw new IllegalStateException(
					"NATIVE_TABLE_PARSE_FAILED: <" + ID_TAG + "> is not a number: " + raw);
		}
	}

	/** 只遍历直接子元素：同标签可合法重复（selectable item），同名后代不参与字段读取。 / Direct children only: same tags may legally repeat (selectable items); same-named descendants are never read as fields. */
	private static Map<String, List<String>> parseFields(Element row) {
		Map<String, List<String>> fields = new LinkedHashMap<>();
		NodeList children = row.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (!(child instanceof Element element)) {
				continue;
			}
			String tag = element.getTagName();
			if (tag.equals(ID_TAG)) {
				continue;
			}
			String value = element.getTextContent().trim();
			fields.computeIfAbsent(tag, ignored -> new java.util.ArrayList<>()).add(value);
		}
		return Collections.unmodifiableMap(fields);
	}

	private static String directChildText(Element row, String tag) {
		NodeList children = row.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child instanceof Element element && tag.equals(element.getTagName())) {
				return element.getTextContent();
			}
		}
		return "";
	}

	private static DocumentBuilder newDocumentBuilder() {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		try {
			factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
			// 允许内部 DTD 子集（真端表的字符实体声明），但拒绝一切外部 DTD/实体。
			// Allow the internal DTD subset (the retail table's character entities), refuse all external DTDs.
			factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
			factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
			factory.setExpandEntityReferences(true);
			return factory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			throw new IllegalStateException("NATIVE_TABLE_PARSE_FAILED: cannot configure XML parser", e);
		}
	}

	/** 表行数。 / The number of rows. */
	public int size() {
		return rowsByQuestId.size();
	}

	/** 按任务查询。 / Looks a row up by quest id. */
	public Optional<QuestRow> find(int questId) {
		return Optional.ofNullable(rowsByQuestId.get(questId));
	}

	/** 按任务查询，缺行 fail-closed。 / Looks a row up; missing rows fail closed. */
	public QuestRow require(int questId) {
		QuestRow row = rowsByQuestId.get(questId);
		if (row == null) {
			throw new IllegalStateException("NATIVE_TABLE_ROW_MISSING: quest " + questId + " has no quest.xml row");
		}
		return row;
	}
}
