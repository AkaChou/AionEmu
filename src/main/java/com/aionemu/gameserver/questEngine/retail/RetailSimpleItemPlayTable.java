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
 * 真端 SimpleItemPlay 模板表（Map/XML/Quest_SimpleItemPlay.xml，入仓副本 UTF-8，43 行）只读视图。
 * <p>
 * 族语义：接取时 NPC 发任务道具（{@code give_item*}）→ 玩家使用道具完成"物品演出"
 * （{@code use_item_name}）→ 回 {@code reward_npc_name} 交付。对话链任务另带
 * {@code talk_npc1/talk_npc2}（中途换道具 {@code give_item2/remove_item2}），
 * 前置任务 {@code con_quest}，过场 {@code cutsceneid1}。
 * 道具符号到本服物品 id 的映射与 SimpleUseItem 同规则：去 {@code ITEM_} 前缀转小写 = 物品
 * {@code name_desc}。Read-only view of the retail SimpleItemPlay template table (43 rows).
 */
public final class RetailSimpleItemPlayTable {

	private final Map<Integer, Entry> entries;

	private RetailSimpleItemPlayTable(Map<Integer, Entry> entries) {
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
	public static RetailSimpleItemPlayTable load(InputStream input) throws IOException {
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
				entries.put(Integer.parseInt(raw), parseEntry(element, Integer.parseInt(raw)));
			}
			return new RetailSimpleItemPlayTable(entries);
		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			throw new IOException("failed to parse retail SimpleItemPlay table", e);
		}
	}

	private static Entry parseEntry(Element element, int questId) {
		return new Entry(questId, text(element, "acquired_npc_name"), text(element, "talk_npc1"),
			text(element, "talk_npc2"), text(element, "give_item"), text(element, "give_item1"),
			text(element, "give_item2"), text(element, "remove_item1"), text(element, "remove_item2"),
			text(element, "use_item_name"), text(element, "reward_npc_name"), text(element, "con_quest"),
			text(element, "cutsceneid1"));
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
	 * 一个任务行（原始符号/名字，解析在合成器）。 / One raw retail row (resolution happens in the compiler).
	 *
	 * @param acquiredNpcName 接取 NPC 名（可为 {@code _faction_} 哨兵）
	 * @param talkNpc1        对话链 NPC 1 名（可空）
	 * @param talkNpc2        对话链 NPC 2 名（可空）
	 * @param giveItem        接取发放道具符号（可空）
	 * @param giveItem1       变体发放道具符号（可空）
	 * @param giveItem2       链中第二发放道具符号（可空）
	 * @param removeItem1     变体回收道具符号（可空）
	 * @param removeItem2     链中回收道具符号（可空）
	 * @param useItemName     物品演出道具符号（本族推进事件；可空）
	 * @param rewardNpcName   交付 NPC 名
	 * @param conQuest        前置任务（可空）
	 * @param cutsceneId      过场 id（可空）
	 */
	public record Entry(int questId, String acquiredNpcName, String talkNpc1, String talkNpc2, String giveItem,
			String giveItem1, String giveItem2, String removeItem1, String removeItem2, String useItemName,
			String rewardNpcName, String conQuest, String cutsceneId) {
	}
}
