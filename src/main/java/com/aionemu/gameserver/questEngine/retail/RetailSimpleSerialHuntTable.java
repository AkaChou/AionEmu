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
 * 真端 SimpleSerialHunt 模板表（Map/XML/Quest_SimpleSerialHunt.xml，入仓副本 UTF-8）只读视图。
 * <p>
 * 每行 = 接取/报告 NPC 名 + 至多五段并列击杀链（{@code monster/count_first..fifth}）。
 * "Serial" 指多怪清单而非顺序推进：生产 XML 用 {@code counter-grid} 每维 {@code required=count}
 * 表达（P3 勘察），与 SimpleHunt 的计数网格同构，因此本表直接产出
 * {@link RetailSimpleHuntTable.Entry}，下游复用 SimpleHunt 的计划绑定与合成器。
 * <p>
 * Read-only view of the retail SimpleSerialHunt template table; rows convert to hunt-shaped
 * entries so the SimpleHunt plan binding and synthesizer are reused unchanged.
 */
public final class RetailSimpleSerialHuntTable {

	/** 最多五段击杀链。 / At most five kill stages. */
	private static final int MAX_STAGES = 5;

	private final Map<Integer, RetailSimpleHuntTable.Entry> entries;

	private RetailSimpleSerialHuntTable(Map<Integer, RetailSimpleHuntTable.Entry> entries) {
		this.entries = Map.copyOf(entries);
	}

	/** 按任务 id 取行。 / Looks up a row by quest id. */
	public Optional<RetailSimpleHuntTable.Entry> find(int questId) {
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
	public static RetailSimpleSerialHuntTable load(InputStream input) throws IOException {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(false);
			factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", false);
			factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
			factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
			Document document = factory.newDocumentBuilder().parse(input);
			Map<Integer, RetailSimpleHuntTable.Entry> entries = new HashMap<>();
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
				RetailSimpleHuntTable.Entry entry = parseEntry(element, Integer.parseInt(raw));
				if (entry != null) {
					entries.put(entry.questId(), entry);
				}
			}
			return new RetailSimpleSerialHuntTable(entries);
		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			throw new IOException("failed to parse retail SimpleSerialHunt table", e);
		}
	}

	private static RetailSimpleHuntTable.Entry parseEntry(Element element, int questId) {
		List<RetailSimpleHuntTable.Counter> counters = new ArrayList<>(MAX_STAGES);
		for (int stage = 1; stage <= MAX_STAGES; stage++) {
			String monster = text(element, stageTag("monster", stage));
			if (monster == null) {
				continue;
			}
			// 单段可为逗号分隔的多怪清单（16991 的 8 个职业变体）。 /
			// A stage may list comma-separated monsters (16991's eight class variants).
			List<String> monsters = new ArrayList<>();
			for (String name : monster.split(",")) {
				if (!name.isBlank()) {
					monsters.add(name.trim());
				}
			}
			if (monsters.isEmpty()) {
				continue;
			}
			int required = 1;
			String count = text(element, stageTag("count", stage));
			if (count != null && count.chars().allMatch(Character::isDigit)) {
				required = Integer.parseInt(count);
			}
			counters.add(new RetailSimpleHuntTable.Counter(stage, required, List.copyOf(monsters)));
		}
		String acquired = text(element, "acquired_npc_name");
		String reward = text(element, "reward_npc_name");
		if (acquired == null && reward == null && counters.isEmpty()) {
			return null;
		}
		return new RetailSimpleHuntTable.Entry(questId, List.copyOf(counters), acquired, reward, null,
			RetailGrantKind.of(acquired), text(element, "talk_npc1"));
	}

	/** {@code monster_first}/{@code count_first} 等段位标签。 / Stage tags like {@code monster_first}. */
	private static String stageTag(String prefix, int stage) {
		return switch (stage) {
			case 1 -> prefix + "_first";
			case 2 -> prefix + "_second";
			case 3 -> prefix + "_third";
			case 4 -> prefix + "_fourth";
			default -> prefix + "_fifth";
		};
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
