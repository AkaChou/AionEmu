package com.aionemu.gameserver.questEngine.retail;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Element;

/**
 * 原版对话名组表内存规范视图（原 {@code retail-quest-ai-name-groups.tsv} 退役后转为动态测试资源流与规范视图；
 * 2026-10-03 台账 XML 化批起表体为 {@code retail-quest-ai-name-groups.xml}）。
 * <p>
 * 守备队同组共用 ScriptDLL 对话名，组名解析为全组成员 name_desc 并打标。
 * <p>
 * Memory canonical view of the retail dialog-name group table (XML since the 2026-10-03
 * ledger-XML batch).
 */
public final class RetailQuestAiNameGroups {

	private RetailQuestAiNameGroups() {
	}

	private static InputStream openStream() {
		// 2026-10-02 归位：单一事实源 = quest/retail/，原 src/main/resources/quest/ 双链退役。
		// 2026-10-03 台账 XML 化：同名 .tsv → .xml。
		// Relocated 2026-10-02: single canonical home under quest/retail/; the old dual chain
		// under src/main/resources/quest/ is retired. Converted to XML in the 2026-10-03 batch.
		InputStream in = RetailQuestAiNameGroups.class
			.getResourceAsStream("/aion/data/static_data/quest/retail/retail-quest-ai-name-groups.xml");
		if (in == null) {
			return new ByteArrayInputStream(new byte[0]);
		}
		return in;
	}

	/** 获取内存规范流列表。 / Returns the canonical input streams. */
	public static List<InputStream> streams() {
		return List.of(openStream());
	}

	/**
	 * 获取全量声明的对话名组映射（{@code quest_ai_name_group} 行的 quest_ai_name / member_name_descs
	 * 两列；旧 TSV 的「列数不等于 2 即跳过」语义保留）。
	 * Returns all declared dialog-name groups (the quest_ai_name / member_name_descs columns of the
	 * {@code quest_ai_name_group} rows; the old "skip rows that are not exactly two columns"
	 * semantics are preserved).
	 */
	public static Map<String, List<String>> defaultGroups() {
		Map<String, List<String>> groups = new LinkedHashMap<>();
		try {
			for (Element row : RetailLedgerXml.rows(RetailLedgerXml.parse(openStream()),
					"quest_ai_name_group")) {
				String name = RetailLedgerXml.text(row, "quest_ai_name");
				String members = RetailLedgerXml.text(row, "member_name_descs");
				if (name != null && members != null) {
					groups.put(name, List.of(members.split(",")));
				}
			}
		} catch (Exception e) {
			throw new IllegalStateException("Failed to parse retail quest ai name groups", e);
		}
		return Map.copyOf(groups);
	}
}
