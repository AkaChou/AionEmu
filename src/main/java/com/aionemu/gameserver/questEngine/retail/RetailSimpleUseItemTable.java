package com.aionemu.gameserver.questEngine.retail;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 真端 SimpleUseItem 模板表（Map/XML/Quest_SimpleUseItem.xml，入仓副本 UTF-8）只读视图。
 * <p>
 * 每行 = 用物品接取的道具符号（{@code use_item_name}，如 {@code ITEM_QUEST_1107A}）+ 报告 NPC 名。
 * 道具符号到本服物品 id 的映射规则：去掉 {@code ITEM_} 前缀转小写 = 物品 {@code name_desc}
 * （1107：{@code quest_1107a} → 182200501，全族 104/104 唯一解析，P3b 勘察）。
 * <p>
 * Read-only view of the retail SimpleUseItem template table (use-item symbol plus reward NPC).
 */
public final class RetailSimpleUseItemTable {

	private final Map<Integer, Entry> entries;

	private RetailSimpleUseItemTable(Map<Integer, Entry> entries) {
		this.entries = Map.copyOf(entries);
	}

	/** 按任务 id 取行。 / Looks up a row by quest id. */
	public Optional<Entry> find(int questId) {
		return Optional.ofNullable(entries.get(questId));
	}

	public int size() {
		return entries.size();
	}

	/** 全部任务 ID（归属门禁与族扫描用）。 / All quest ids, for ownership gates and family scans. */
	public Set<Integer> questIds() {
		return entries.keySet();
	}

	/**
	 * 解析真端模板表；文件带内部 DTD（实体是纯文本替换），因此允许内部子集、禁止外部访问。
	 * Parses the retail table; it carries an internal DTD subset with text-only entities.
	 */
	public static RetailSimpleUseItemTable load(InputStream input) throws IOException {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(false);
			factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", false);
			factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
			factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
			Document document = factory.newDocumentBuilder().parse(input);
			Map<Integer, Entry> entries = new HashMap<>();
			NodeList nodes = document.getElementsByTagName("id");
			for (int index = 0; index < nodes.getLength(); index++) {
				Node node = nodes.item(index);
				if (!(node instanceof Element element)) {
					continue;
				}
				String raw = element.getAttribute("id");
				if (raw == null || !raw.chars().allMatch(Character::isDigit)) {
					continue;
				}
				Entry entry = parseEntry(element, Integer.parseInt(raw));
				if (entry != null) {
					entries.put(entry.questId(), entry);
				}
			}
			return new RetailSimpleUseItemTable(entries);
		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			throw new IOException("failed to parse retail SimpleUseItem table", e);
		}
	}

	private static Entry parseEntry(Element element, int questId) {
		String useItem = text(element, "use_item_name");
		String reward = text(element, "reward_npc_name");
		if (useItem == null && reward == null) {
			return null;
		}
		return new Entry(questId, useItem, reward);
	}

	private static String text(Element parent, String tag) {
		NodeList nodes = parent.getElementsByTagName(tag);
		if (nodes.getLength() == 0) {
			return null;
		}
		String value = nodes.item(0).getTextContent();
		return value == null || value.isBlank() ? null : value.trim();
	}

	/**
	 * 一个任务行。 / One retail quest row.
	 *
	 * @param useItemName 用物品接取的道具符号（{@code ITEM_QUEST_*}）/ use-item symbol
	 * @param rewardNpc   报告 NPC 名
	 */
	public record Entry(int questId, String useItemName, String rewardNpc) {
	}
}
