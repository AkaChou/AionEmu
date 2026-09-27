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
 * 真端 CombineTask 模板表（Map/XML/Quest_CombineTask.xml，入仓副本 UTF-8）只读视图。
 * <p>
 * 每行 = 接取 NPC 名（多为两个：天/魔各一）+ 合成技能 + 技能点 + 配方名 + 产物（名 + 数量）
 * + 1..8 个工料分量（{@code give_componentN}，名 + 数量）。
 * 这些字段就是 ScriptDLL64 {@code FUN_180caac10} 的参数来源：接取时发分量 + 学配方，
 * 合成时逐个检查/发放产物并收尾（{@code 0x1c0}/{@code 0x1f8}/{@code 0x1d0}/{@code 0x1f0}）。
 * <p>
 * Read-only view of the retail CombineTask template table; one row per craft quest.
 */
public final class RetailCombineTaskTable {

	/** 真端 helper 写死的分量槽位数（{@code lVar4 = 8}）。 / Component slots hard-coded by the retail helper. */
	private static final int MAX_COMPONENTS = 8;

	private final Map<Integer, Entry> entries;

	private RetailCombineTaskTable(Map<Integer, Entry> entries) {
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
	public static RetailCombineTaskTable load(InputStream input) throws IOException {
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
			return new RetailCombineTaskTable(entries);
		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			throw new IOException("failed to parse retail CombineTask table", e);
		}
	}

	private static Entry parseEntry(Element element, int questId) {
		List<String> npcs = new ArrayList<>();
		for (String name : text(element, "task_npc").split(",")) {
			if (!name.isBlank()) {
				npcs.add(name.trim());
			}
		}
		List<Slot> products = slots(textOrEmpty(element, "product"));
		List<Slot> components = new ArrayList<>(MAX_COMPONENTS);
		for (int slot = 1; slot <= MAX_COMPONENTS; slot++) {
			String raw = text(element, "give_component" + slot);
			if (raw == null) {
				continue;
			}
			components.addAll(slots(raw));
		}
		if (npcs.isEmpty() && products.isEmpty() && components.isEmpty()) {
			return null;
		}
		int skillPoint = 0;
		String rawPoint = text(element, "combine_skillpoint");
		if (rawPoint != null && rawPoint.chars().allMatch(Character::isDigit) && !rawPoint.isEmpty()) {
			skillPoint = Integer.parseInt(rawPoint);
		}
		return new Entry(questId, List.copyOf(npcs), text(element, "combineskill"), skillPoint,
			text(element, "recipe_name"), List.copyOf(products), List.copyOf(components));
	}

	/** 解析 "名字 数量" 序列（真端表用空格分隔）。 / Parses the "name count" token sequence used by the table. */
	private static List<Slot> slots(String raw) {
		List<Slot> slots = new ArrayList<>(2);
		String[] tokens = raw.trim().split("\\s+");
		for (int index = 0; index + 1 < tokens.length; index += 2) {
			String name = tokens[index];
			String count = tokens[index + 1];
			if (name.isBlank() || !count.chars().allMatch(Character::isDigit) || count.isEmpty()) {
				continue;
			}
			slots.add(new Slot(name, Integer.parseInt(count)));
		}
		return slots;
	}

	private static String text(Element parent, String tag) {
		NodeList nodes = parent.getElementsByTagName(tag);
		if (nodes.getLength() == 0) {
			return null;
		}
		String value = nodes.item(0).getTextContent();
		return value == null || value.isBlank() ? null : value.trim();
	}

	private static String textOrEmpty(Element parent, String tag) {
		String value = text(parent, tag);
		return value == null ? "" : value;
	}

	/**
	 * 一个名字 + 数量槽位（产物或工料分量）。 / One named slot with a count (product or component).
	 */
	public record Slot(String name, int count) {

		public Slot {
			if (name == null || name.isBlank()) {
				throw new IllegalArgumentException("slot name must not be blank");
			}
			if (count <= 0) {
				throw new IllegalArgumentException("slot count must be positive");
			}
		}
	}

	/**
	 * 一个任务行。 / One retail quest row.
	 *
	 * @param taskNpcs     接取 NPC 名（真端表），通常 2 个（阵营各一）
	 * @param combineSkill 合成技能符号名（如 {@code weaponsmith}）
	 * @param skillPoint   合成技能点
	 * @param recipeName   配方符号名（真端表不直接给 recipe id，需要本服配方索引反查）
	 * @param products     产物槽位（本族实测恒为 1；多产物拒绝码另计）
	 * @param components   接取时发放、报告时回收的工料分量（1..8 槽）
	 */
	public record Entry(int questId, List<String> taskNpcs, String combineSkill, int skillPoint,
			String recipeName, List<Slot> products, List<Slot> components) {
	}
}
