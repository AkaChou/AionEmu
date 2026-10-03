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
 * 真端 SimpleHunt 模板表（Map/XML/Quest_SimpleHunt.xml）只读视图。
 * <p>
 * 每个任务一行，形如 {@code <id id="1102"><count1>3</count1><monster1>CherubimL_1_n, …</monster1>…}；
 * {@code countN/monsterN} 就是 ScriptDLL64 注册点里的 {@code param_4/param_3} 来源
 * （见 .agents/summary/scriptdll-quest-driver/2026-09-22-phase2-driver-semantics.zh-CN.md）。
 * Read-only view of the retail SimpleHunt template table.
 */
public final class RetailSimpleHuntTable {

	/** 真端表允许的最大计数器槽位（count1..countN）。 / Highest counter slot the retail table uses. */
	private static final int MAX_COUNTERS = 8;

	private final Map<Integer, Entry> entries;

	private RetailSimpleHuntTable(Map<Integer, Entry> entries) {
		this.entries = Map.copyOf(entries);
	}

	public Optional<Entry> find(int questId) {
		return Optional.ofNullable(entries.get(questId));
	}

	public int size() {
		return entries.size();
	}

	/** 全部任务 ID（门禁与归属清单比对用）。 / All quest ids, for ownership gates. */
	public Set<Integer> questIds() {
		return entries.keySet();
	}

	/**
	 * 解析真端模板表。副本已无 DOCTYPE（2026-10-03 剥离批；schema = 同目录
	 * {@code Quest_SimpleHunt.xsd}）；解析器保留内部子集能力、外部 DTD/实体一律拒绝（纵深防御）。
	 * Parses the retail template table. The repo copy carries no DOCTYPE (2026-10-03 strip batch; schema
	 * in the sibling {@code Quest_SimpleHunt.xsd}); internal-subset capability kept, external access denied.
	 */
	public static RetailSimpleHuntTable load(InputStream input) throws IOException {
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
			return new RetailSimpleHuntTable(entries);
		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			throw new IOException("failed to parse retail SimpleHunt table", e);
		}
	}

	private static Entry parseEntry(Element element, int questId) {
		List<Counter> counters = new ArrayList<>(2);
		for (int slot = 1; slot <= MAX_COUNTERS; slot++) {
			String count = text(element, "count" + slot);
			String monsters = text(element, "monster" + slot);
			if (count == null && monsters == null) {
				continue;
			}
			counters.add(new Counter(slot, count == null ? 0 : Integer.parseInt(count.trim()), splitNames(monsters)));
		}
		if (counters.isEmpty()) {
			return null;
		}
		String acquiredNpc = text(element, "acquired_npc_name");
		// talk_npc1 = 真端"先听简报再计数"的中间 NPC（串行族用它合成简报步骤）。 /
		// talk_npc1 is the retail briefing NPC that must be visited before kill counters open.
		return new Entry(questId, counters, acquiredNpc, text(element, "reward_npc_name"),
			text(element, "con_quest"), RetailGrantKind.of(acquiredNpc), text(element, "talk_npc1"));
	}

	private static String text(Element parent, String tag) {
		NodeList nodes = parent.getElementsByTagName(tag);
		if (nodes.getLength() == 0) {
			return null;
		}
		String value = nodes.item(0).getTextContent();
		return value == null || value.isBlank() ? null : value.trim();
	}

	private static List<String> splitNames(String raw) {
		List<String> names = new ArrayList<>();
		if (raw == null) {
			return List.of();
		}
		for (String token : raw.split(",")) {
			String name = token.trim();
			if (!name.isEmpty()) {
				names.add(name);
			}
		}
		return List.copyOf(names);
	}

	/**
	 * 一个任务行。 / One retail quest row.
	 *
	 * @param counters  按槽位升序的计数器（槽位 N → SECTION_(N-1)）
	 * @param acquiredNpc 接取 NPC 名（spawn/NPC 名，非 id）
	 * @param rewardNpc 报告 NPC 名
	 * @param conQuest 真端表声明的后续任务（与 quest.xml 的 finished_quest_condN 互补）
	 * @param talkNpc 真端 {@code talk_npc1} 简报 NPC 名（可空；串行族据此合成非计数步骤）
	 */
	public record Entry(int questId, List<Counter> counters, String acquiredNpc, String rewardNpc, String conQuest,
			RetailGrantKind grantKind, String talkNpc, boolean pvpProgress) {

		/** 不含简报 NPC 的旧形（历史调用方与普通击杀行）。 / Legacy shape without the briefing NPC. */
		public Entry(int questId, List<Counter> counters, String acquiredNpc, String rewardNpc, String conQuest,
				RetailGrantKind grantKind) {
			this(questId, counters, acquiredNpc, rewardNpc, conQuest, grantKind, null, false);
		}

		/** 带简报 NPC 的家族形（普通击杀行，非 PVP）。 / Family shape with the briefing NPC, kills only. */
		public Entry(int questId, List<Counter> counters, String acquiredNpc, String rewardNpc, String conQuest,
				RetailGrantKind grantKind, String talkNpc) {
			this(questId, counters, acquiredNpc, rewardNpc, conQuest, grantKind, talkNpc, false);
		}

		public Optional<Counter> counter(int slot) {
			return counters.stream().filter(counter -> counter.slot() == slot).findFirst();
		}
	}

	/** 单个击杀计数槽位。 / A single kill counter slot. */
	public record Counter(int slot, int required, List<String> monsters) {
	}
}
