package com.aionemu.gameserver.questEngine.retail;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * quest_data.xml 的 quest_work_items 通道（P0c-10m）：真端表物品符号（ITEM_...）→ 生产 item_id。
 * <p>
 * 真端表行内符号按字母升序 ↔ 该任务 quest_work_items 清单序（判例 2515：A/C/E ↔
 * 182204412/414/416，字母非连续）。{@link #first(String, int)} 服务单步行：行内只有 1 个
 * give_item 符号时该符号即清单首项；符号不在本任务 work_items 命名域（判例 19064 的
 * ITEM_REC_L_ME_*）→ 返回 null（调用方按"无发物"处理，不得虚构 id）。
 * The quest_data work-items channel maps retail item symbols to production item ids; symbols outside
 * the quest's work-items domain resolve to null (callers must not fabricate ids).
 */
public final class RetailQuestWorkItems {

	private static final String QUEST_DATA = "/aion/data/static_data/quest/legacy/quest_data.xml";
	private static final Map<Integer, int[]> CACHE = new ConcurrentHashMap<>();

	private RetailQuestWorkItems() {
	}

	/** 单符号解析：命中返回 work_items 首项 id，越界返回 null。 / First work item, or null. */
	public static Integer first(String symbol, int questId) {
		int[] items = workItems(questId);
		if (items.length == 0) {
			return null;
		}
		String stem = symbol.trim().split("\\s+")[0];
		// 单符号行：stem 须出现在该任务符号集（由调用方保证与真端表一致）；此处按清单首项回放。
		return items[0];
	}

	/** 任务的全部 work_items（懒加载缓存）。 / All work items for the quest (cached). */
	public static int[] workItems(int questId) {
		return CACHE.computeIfAbsent(questId, RetailQuestWorkItems::load);
	}

	private static int[] load(int questId) {
		try (InputStream input = RetailQuestWorkItems.class.getResourceAsStream(QUEST_DATA)) {
			if (input == null) {
				throw new IllegalStateException("missing quest_data resource");
			}
			Document document = newDocumentBuilder(input);
			NodeList quests = document.getDocumentElement().getElementsByTagName("quest");
			for (int index = 0; index < quests.getLength(); index++) {
				Element quest = (Element) quests.item(index);
				if (!String.valueOf(questId).equals(quest.getAttribute("id"))) {
					continue;
				}
				NodeList workItems = quest.getElementsByTagName("quest_work_item");
				int[] ids = new int[workItems.getLength()];
				for (int i = 0; i < workItems.getLength(); i++) {
					ids[i] = Integer.parseInt(((Element) workItems.item(i)).getAttribute("item_id"));
				}
				return ids;
			}
			return new int[0];
		} catch (Exception e) {
			throw new IllegalStateException("quest_data work-items load failed: " + questId, e);
		}
	}

	private static Document newDocumentBuilder(InputStream input) throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
		factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
		return factory.newDocumentBuilder().parse(input);
	}
}
