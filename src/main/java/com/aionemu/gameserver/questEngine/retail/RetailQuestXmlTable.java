package com.aionemu.gameserver.questEngine.retail;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * 真端 {@code quest.xml}（Map/XML/quest.xml，10035 个任务的模板元数据）只读视图。
 * <p>
 * 这是"真端元数据层"的原始数据源：任务的等级/种族/职业/前置/奖励/掉落等
 * {@link com.aionemu.gameserver.questEngine.definition.QuestMetadata} 全部字段都从这里映射
 * （见 {@link RetailQuestMetadataCompiler}）。入仓副本为精简形（UTF-8、DOCTYPE/实体子集已移除，
 * 2026-10-03 剥离批；schema = 同目录 {@code quest.xsd}），装载策略与 {@link RetailSimpleHuntTable}
 * 相同：解析器保留内部子集能力，外部 DTD/实体一律拒绝（纵深防御）。
 * Read-only view of the retail quest.xml template metadata for every quest.
 */
public final class RetailQuestXmlTable {

	private final Map<Integer, Entry> entries;

	private RetailQuestXmlTable(Map<Integer, Entry> entries) {
		this.entries = Map.copyOf(entries);
	}

	public Optional<Entry> find(int questId) {
		return Optional.ofNullable(entries.get(questId));
	}

	public int size() {
		return entries.size();
	}

	public boolean contains(int questId) {
		return entries.containsKey(questId);
	}

	/**
	 * 解析真端 quest.xml。 / Parses the retail quest.xml.
	 */
	public static RetailQuestXmlTable load(InputStream input) throws IOException {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(false);
			factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", false);
			factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
			factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
			Document document = factory.newDocumentBuilder().parse(input);
			Map<Integer, Entry> entries = new HashMap<>();
			NodeList nodes = document.getElementsByTagName("quest");
			for (int index = 0; index < nodes.getLength(); index++) {
				Node node = nodes.item(index);
				if (!(node instanceof Element element)) {
					continue;
				}
				NodeList children = element.getChildNodes();
				int questId = 0;
				Map<String, String> fields = new TreeMap<>();
				Map<String, List<String>> blocks = new TreeMap<>();
				for (int child = 0; child < children.getLength(); child++) {
					Node item = children.item(child);
					if (!(item instanceof Element tag)) {
						continue;
					}
					String name = tag.getTagName();
					if ("id".equals(name)) {
						String value = tag.getTextContent() == null ? "" : tag.getTextContent().trim();
						questId = value.isEmpty() ? 0 : Integer.parseInt(value);
						continue;
					}
					// 容器标签（如 fighter_selectable_reward）按其 <data> 子块逐条记录文本。
					// Container tags (fighter_selectable_reward) record one text per <data> child.
					List<String> dataTexts = childTexts(tag, "data");
					if (!dataTexts.isEmpty()) {
						blocks.put(name, dataTexts);
						continue;
					}
					String value = tag.getTextContent() == null ? "" : tag.getTextContent().trim();
					if (!fields.containsKey(name)) {
						fields.put(name, value);
					}
				}
				if (questId > 0) {
					entries.put(questId, new Entry(questId, fields, blocks));
				}
			}
			return new RetailQuestXmlTable(entries);
		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			throw new IOException("failed to parse retail quest.xml", e);
		}
	}

	/**
	 * 一个任务的原始字段行：标签名 → 文本值（空白已裁剪，重复标签取首个），外加容器块的子块文本。
	 * One raw quest row: tag to trimmed text values plus per-child texts of container blocks.
	 */
	public record Entry(int questId, Map<String, String> fields, Map<String, List<String>> blocks) {

		public Entry {
			fields = Map.copyOf(fields);
			blocks = blocks.entrySet().stream()
				.collect(TreeMap::new, (map, entry) -> map.put(entry.getKey(), List.copyOf(entry.getValue())),
					TreeMap::putAll);
		}

		/** 读取文本字段；缺失或空白返回 null。 / Reads a text field, or null when missing or blank. */
		public String text(String tag) {
			String value = fields.get(tag);
			return value == null || value.isBlank() ? null : value;
		}

		/** 读取整数字段；缺失返回 null。 / Reads an integer field, or null when missing. */
		public Integer integer(String tag) {
			String value = text(tag);
			return value == null ? null : Integer.valueOf(value.trim());
		}

		/** 读取布尔字段（1/TRUE）。 / Reads a 1/TRUE boolean field. */
		public boolean bool(String tag) {
			String value = text(tag);
			return value != null && ("1".equals(value) || "TRUE".equalsIgnoreCase(value));
		}

		/**
		 * 收集带数字后缀的字段族（{@code finished_quest_cond1..N} → 按槽位升序）。
		 * Collects a numbered tag family in ascending slot order.
		 */
		public List<String> numbered(String base) {
			Map<Integer, String> bySlot = new TreeMap<>();
			for (Map.Entry<String, String> field : fields.entrySet()) {
				String key = field.getKey();
				if (!key.startsWith(base)) {
					continue;
				}
				String suffix = key.substring(base.length());
				if (!suffix.isEmpty() && suffix.chars().allMatch(Character::isDigit)) {
					bySlot.put(Integer.parseInt(suffix), field.getValue());
				}
			}
			return List.copyOf(bySlot.values());
		}

		/**
		 * 容器块（如 {@code fighter_selectable_reward}）的逐子块文本（按文档顺序）。
		 * Per-child texts of a container block, in document order.
		 */
		public List<String> block(String tag) {
			return blocks.getOrDefault(tag, List.of());
		}
	}

	/** 收集 {@code parent} 下指定子标签的文本。 / Collects texts of the named child elements. */
	private static List<String> childTexts(Element parent, String childName) {
		NodeList nodes = parent.getChildNodes();
		List<String> texts = new ArrayList<>();
		for (int index = 0; index < nodes.getLength(); index++) {
			Node node = nodes.item(index);
			if (node instanceof Element child && childName.equals(child.getTagName())) {
				String value = child.getTextContent() == null ? "" : child.getTextContent().trim();
				texts.add(value);
			}
		}
		return texts;
	}
}
