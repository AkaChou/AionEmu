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
import java.util.Set;

/**
 * 真端 SimpleCollectItem 模板表（Map/XML/Quest_SimpleCollectItem.xml，入仓副本 UTF-8）只读视图。
 * <p>
 * 每行 = 接取 NPC 名 + 采集对象名（{@code object1..N}）+ 报告 NPC 名 + 可选前置 {@code con_quest}。
 * 对象在本服就是 {@code npc_template}（{@code tribe=FIELD_OBJECT_*}），因此对象名与 NPC 名共用
 * {@link RetailNpcNameIndex}；这就是 ScriptDLL64 该族 helper 的"点对象推进进度"语义来源。
 * <p>
 * Read-only view of the retail SimpleCollectItem template table.
 */
public final class RetailSimpleCollectItemTable {

	private final Map<Integer, Entry> entries;

	private RetailSimpleCollectItemTable(Map<Integer, Entry> entries) {
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
	 * 解析真端模板表。文件带内部 DTD（实体是纯文本替换），因此允许内部子集、禁止外部访问。
	 * Parses the retail table; it carries an internal DTD subset with text-only entities.
	 */
	public static RetailSimpleCollectItemTable load(InputStream input) throws IOException {
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
			return new RetailSimpleCollectItemTable(entries);
		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			throw new IOException("failed to parse retail SimpleCollectItem table", e);
		}
	}

	private static Entry parseEntry(Element element, int questId) {
		List<String> objects = new ArrayList<>(2);
		for (int slot = 1; slot <= 8; slot++) {
			String object = text(element, "object" + slot);
			if (object != null) {
				objects.add(object);
			}
		}
		String acquired = text(element, "acquired_npc_name");
		String reward = text(element, "reward_npc_name");
		if (acquired == null && reward == null && objects.isEmpty()) {
			return null;
		}
		// talk_npc1 = 真端"先与中间 NPC 对话再开放采集/交付"的步骤（14120/14150 等）。 /
		// talk_npc1 is the retail mid-NPC step that must be visited before collect/hand-in.
		return new Entry(questId, acquired, List.copyOf(objects), reward, text(element, "con_quest"),
			RetailGrantKind.of(acquired), text(element, "talk_npc1"));
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
	 * @param questId    任务 id / quest id
	 * @param acquiredNpc 接取 NPC 名（可为真端类别哨兵，见 {@link #grantKind()}）
	 * @param objects     采集对象名（本服 {@code npc_template} 的 {@code name_desc}）
	 * @param rewardNpc   报告 NPC 名
	 * @param conQuest    真端表声明的后续任务（与 quest.xml 的 finished_quest_condN 互补）
	 * @param grantKind   接取名类别（NPC 名或三类系统发放哨兵）/ acquire-name kind (NPC name or a system-grant sentinel)
	 */
	public record Entry(int questId, String acquiredNpc, List<String> objects, String rewardNpc, String conQuest,
			RetailGrantKind grantKind, String talkNpc) {

		/** 不含简报 NPC 的旧形（历史调用方）。 / Legacy shape without the talk NPC. */
		public Entry(int questId, String acquiredNpc, List<String> objects, String rewardNpc, String conQuest,
				RetailGrantKind grantKind) {
			this(questId, acquiredNpc, objects, rewardNpc, conQuest, grantKind, null);
		}
	}
}
