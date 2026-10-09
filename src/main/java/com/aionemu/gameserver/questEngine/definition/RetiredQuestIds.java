package com.aionemu.gameserver.questEngine.definition;

import java.io.InputStream;
import java.util.Set;
import java.util.TreeSet;

import org.w3c.dom.Element;

import com.aionemu.gameserver.questEngine.retail.RetailLedgerXml;

/**
 * 已退役任务的 id 集合：生产 XML 已删除、定义由原版驱动合成。
 * <p>
 * 2026-09-23 口径（用户指令）：退役任务的 XML 不再进仓——历史内容在 git 里可回溯，仓库只保留
 * "谁已退役"的机器可读清单，即保留清单 {@code retail-xml-retention.xml} 里 {@code owner=RETAIL_TABLE} 的行。
 * <p>
 * Ids of retired quests (production XML deleted, definitions synthesized from the retail files).
 * The retention manifest is the single source of truth; git history holds the deleted XML.
 */
public final class RetiredQuestIds {

	private static final String RETENTION = "/aion/data/static_data/quest/retail/retail-xml-retention.xml";

	private static volatile Set<Integer> ids;

	private RetiredQuestIds() {
	}

	/** 全部已退役任务 id。 / Every retired quest id. */
	public static Set<Integer> all() {
		Set<Integer> local = ids;
		if (local == null) {
			local = load();
			ids = local;
		}
		return local;
	}

	/** 该任务是否已退役。 / Whether the quest is retired. */
	public static boolean contains(int questId) {
		return all().contains(questId);
	}

	private static Set<Integer> load() {
		Set<Integer> result = new TreeSet<>();
		InputStream input = RetiredQuestIds.class.getResourceAsStream(RETENTION);
		if (input == null) {
			throw new IllegalStateException("missing retention manifest " + RETENTION);
		}
		try {
			for (Element row : RetailLedgerXml.rows(RetailLedgerXml.parse(input), "quest")) {
				if ("RETAIL_TABLE".equals(RetailLedgerXml.text(row, "owner"))) {
					result.add(Integer.parseInt(RetailLedgerXml.text(row, "quest_id")));
				}
			}
		} catch (java.io.IOException e) {
			throw new IllegalStateException("retention manifest unreadable", e);
		}
		return Set.copyOf(result);
	}
}
